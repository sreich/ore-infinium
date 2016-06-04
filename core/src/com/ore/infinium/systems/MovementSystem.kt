/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreSettings
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.getNullable

@Wire(failOnNull = false)
class MovementSystem(private val m_world: OreWorld) : IteratingSystem(
        Aspect.all(SpriteComponent::class.java, VelocityComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem
    private lateinit var m_clientNetworkSystem: ClientNetworkSystem

    private lateinit var m_tagManager: TagManager

    //lowest value we want to represent before killing velocity
    private val VELOCITY_MINIMUM_CUTOFF = 0.008f

    companion object {
        val GRAVITY_ACCEL = 0.5f
        val GRAVITY_ACCEL_CLAMP = 0.5f
    }

    override fun setWorld(world: World) {
        super.setWorld(world)
    }

    override fun initialize() {

    }

    override fun process(entityId: Int) {
        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions
        simulate(entityId, getWorld().delta)

        //fixme maybe make a dropped component?
        if (m_world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            val itemComponent = itemMapper.getNullable(entityId)

            if (itemComponent != null && itemComponent.state == ItemComponent.State.DroppedInWorld) {
                simulateDroppedItem(entityId, getWorld().delta)
            }
        }

        if (m_world.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            val mainPlayer = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
            val playerSprite = spriteMapper.get(mainPlayer)
            m_world.m_camera.position.set(playerSprite.sprite.x, playerSprite.sprite.y, 0f)
            m_world.m_camera.update()

            m_clientNetworkSystem.sendPlayerMoved()
        }
    }

    private fun simulate(entity: Int, delta: Float) {
        if (m_world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            //server doesn't process past here. client tells us where they are.
            //fixme, though we do need to eventually at least half-ass verify it, which
            //means doing it on server as well
            return
        }

        //it isn't a player or npc or anything that can be controlled
        if (!controlMapper.has(entity)) {
            return
        }

        //noclip and is a player
        if (OreSettings.noclip && playerMapper.has(entity)) {
            simulateNoClip(entity, delta)
            return
        }

        //fixme handle noclip
        val spriteComponent = spriteMapper.get(entity)
        val origPosition = Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y)

        val velocityComponent = velocityMapper.get(entity)

        val oldVelocity = Vector2(velocityComponent.velocity)
        var newVelocity = Vector2(oldVelocity)

        val desiredDirection = controlMapper.get(entity).desiredDirection

        //acceleration due to gravity
        var acceleration = Vector2(desiredDirection.x * PlayerComponent.movementSpeed, GRAVITY_ACCEL)

        acceleration = maybePerformJump(acceleration, playerEntity = entity)

        newVelocity = newVelocity.add(acceleration.x * delta, acceleration.y * delta)

        //bleed velocity a bit
        newVelocity.x *= 0.8f

        //gets small enough velocity, cease movement/sleep object.
        //on both x and y, independently
        //TODO: add threshold to nullify velocity..so we don't infinitely move and thus burn through ticks/packets
        //and don't send anything if not dirty
        if (Math.abs(newVelocity.x) < VELOCITY_MINIMUM_CUTOFF) {
            newVelocity.x = 0f
        }
        if (Math.abs(newVelocity.y) < VELOCITY_MINIMUM_CUTOFF) {
            newVelocity.y = 0f
        }

        //        newVelocity.x = MathUtils.clamp(newVelocity.x, -PlayerComponent.maxMovementSpeed, PlayerComponent
        // .maxMovementSpeed);
        //        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World
        // .GRAVITY_ACCEL_CLAMP);


        //todo  clamp both axes between some max/min values..
        velocityComponent.velocity.set(newVelocity.x, newVelocity.y)

        ///////// the desired position uses velocity verlet integration
        // http://lolengine.net/blog/2011/12/14/understanding-motion-in-games

        // newVelocity is now invalid, note (vector reference modification).
        val desiredPosition = origPosition.add(oldVelocity.add(newVelocity.scl(0.5f * delta)))
        val finalPosition = performCollision(desiredPosition, entity)

        spriteComponent.sprite.setPosition(finalPosition.x, finalPosition.y)
        //FIXME: do half-ass friction, to feel better than this. and then when movement is close to 0, 0 it.
    }

    private fun maybePerformJump(acceleration: Vector2, playerEntity: Int): Vector2 {
        val jumpComponent = jumpMapper.get(playerEntity)
        if (jumpComponent.canJump && jumpComponent.shouldJump) {

            if (jumpComponent.jumpTimer.milliseconds() >= jumpComponent.jumpInterval) {
                //good to jump, actually do it now.
                jumpComponent.jumpTimer.reset()

                acceleration.y = -PlayerComponent.jumpVelocity
            }
        }

        jumpComponent.canJump = false
        jumpComponent.shouldJump = false

        return acceleration
    }

    private fun simulateNoClip(entity: Int, delta: Float) {
        val spriteComponent = spriteMapper.get(entity)
        val desiredDirection = controlMapper.get(entity).desiredDirection

        val x = spriteComponent.sprite.x
        val y = spriteComponent.sprite.y
        spriteComponent.sprite.setPosition(x + desiredDirection.x * PlayerComponent.movementSpeed * 6.0f * delta,
                                           y + desiredDirection.y * PlayerComponent.movementSpeed * 6.0f * delta)
    }

    private fun simulateDroppedItem(item: Int, delta: Float) {
        val itemComponent = itemMapper.get(item)
        assert(itemComponent.state == ItemComponent.State.DroppedInWorld)

        val itemSpriteComponent = spriteMapper.get(item)
        val itemVelocityComponent = velocityMapper.get(item)

        val itemPosition = Vector2(itemSpriteComponent.sprite.x, itemSpriteComponent.sprite.y)

        val x = itemPosition.x.toInt()
        val y = itemPosition.y.toInt()

        val itemOldVelocity = Vector2(itemVelocityComponent.velocity)
        val itemNewVelocity = Vector2(itemVelocityComponent.velocity)

        val acceleration = Vector2(0.0f, GRAVITY_ACCEL)

        if (itemComponent.playerIdWhoDropped != null && itemComponent.justDropped ) {
            val playerEntityWhoDropped = m_world.playerEntityForPlayerConnectionID(itemComponent.playerIdWhoDropped!!)
            val playerVelocityComponent = velocityMapper.get(playerEntityWhoDropped)
            val playerVelocity = Vector2(playerVelocityComponent.velocity)

            //acceleration.x += Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);
            //acceleration.x Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);

            //only add player velocity the firstEntity tick, as soon as they drop it.
            //so that we can throw things harder using players current speed
            itemComponent.justDropped = false
            acceleration.y += -GRAVITY_ACCEL * 3.0f
            acceleration.x += 0.5f
        }

        itemNewVelocity.add(acceleration)

        itemNewVelocity.x *= 0.55f

        itemNewVelocity.x = MathUtils.clamp(itemNewVelocity.x, -PlayerComponent.maxMovementSpeed,
                                            PlayerComponent.maxMovementSpeed)
        //        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World
        // .GRAVITY_ACCEL_CLAMP);
        itemNewVelocity.y = MathUtils.clamp(itemNewVelocity.y, -GRAVITY_ACCEL_CLAMP * 10,
                                            GRAVITY_ACCEL_CLAMP)

        //gets small enough velocity, cease movement/sleep object.
        //on both x and y, independently
        if (Math.abs(itemNewVelocity.x) < VELOCITY_MINIMUM_CUTOFF) {
            itemNewVelocity.x = 0f
        }
        if (Math.abs(itemNewVelocity.y) < VELOCITY_MINIMUM_CUTOFF) {
            itemNewVelocity.y = 0f
        }

        if (!itemNewVelocity.isZero) {
            itemVelocityComponent.velocity.set(itemNewVelocity)
            val desiredPosition = itemPosition.add(itemOldVelocity.add(itemNewVelocity.scl(0.5f * delta)))
            val finalPosition = performCollision(desiredPosition, item)

            itemSpriteComponent.sprite.setPosition(finalPosition.x, finalPosition.y)
            maybeSendEntityMoved(item)
        }

    }

    /**
     * @param desiredPosition
     * *         position we'd like to be at, given an integrated velocity + position
     * *
     * @param entity
     * *
     * *
     * @return the actual new position, after collision (if any).
     */
    private fun performCollision(desiredPosition: Vector2, entity: Int): Vector2 {
        var canJump = false

        val spriteComponent = spriteMapper.get(entity)
        val velocityComponent = velocityMapper.get(entity)
        val velocity = velocityComponent.velocity
        val sizeMeters = Vector2(spriteComponent.sprite.width, spriteComponent.sprite.height)

        //FIXME: this whole thing needs a way better solution, it's horrible.
        val epsilon = 1 * 0.01f
        var leftX = (desiredPosition.x - sizeMeters.x * 0.5f).toInt()
        var rightX = (desiredPosition.x + sizeMeters.x * 0.5f).toInt()
        var topY = (desiredPosition.y - sizeMeters.y * 0.5f).toInt()
        val bottomY = (desiredPosition.y + sizeMeters.y * 0.5f).toInt()

        val oldTopY = topY

        if (velocity.y > 0.0f) {
            //we are not falling/moving up
            topY += 1
        }

        var collision = false
        if (velocity.x > 0.0f) {
            //try moving right, only loop over tiles on the right side(inclusive, remember)
            for (y in topY..bottomY - 1) {
                if (m_world.isBlockSolid(rightX, y)) {
                    velocity.x = 0.0f
                    collision = true

                    val tileRight = (rightX - 0).toFloat()

                    //fixme: super small threshold to prevent sticking to right side,
                    //i dont know why this isn't the same case as all the others. i feel like something
                    //is wrong somewhere else..doesn't make sense.
                    desiredPosition.x = tileRight - sizeMeters.x * 0.5f - epsilon
                    break
                } // else noop, move freely
            }
        } else if (velocity.x < 0.0f) {
            //try moving left, only loop over tiles on the left side
            for (y in topY..bottomY - 1) {
                if (m_world.isBlockSolid(leftX, y)) {

                    velocity.x = 0.0f
                    collision = true

                    val tileLeft = (leftX + 1).toFloat()
                    desiredPosition.x = tileLeft + sizeMeters.x * 0.5f + epsilon
                    break
                } // else noop, move freely
            }
        }

        topY = oldTopY
        // recalculate startx, etc. now that we reperformed x position
        // y was not touched, so no need
        leftX = (desiredPosition.x - sizeMeters.x * 0.5f).toInt()
        rightX = (desiredPosition.x + sizeMeters.x * 0.5f).toInt()
        collision = false

        //qCDebug(ORE_IMPORTANT) << "y collision test: bottomy: " << bottomY << " leftX: " << leftX << " topY: " <<
        // topY << " rightX: " << rightX;

        if (velocity.y > 0.0f) {
            //try moving down, only loop over tiles on the bottom side(inclusive, remember)
            for (x in leftX..rightX) {
                if (m_world.isBlockSolid(x, bottomY + 0)) {
                    canJump = true

                    //collision occured, stop here
                    velocity.y = 0.0f
                    collision = true

                    //indexes are top-left remember, due to how it's rendered and such.
                    val tileTop = bottomY.toFloat()
                    desiredPosition.y = tileTop - sizeMeters.y * 0.5f
                    break
                } // else noop, move freely
            }
        } else if (velocity.y < 0.0f) {
            //try moving up, only loop over tiles on the bottom side(inclusive, remember)
            for (x in leftX..rightX) {
                if (m_world.isBlockSolid(x, topY - 1)) {
                    //collision occured, stop here
                    velocity.y = 0.0f
                    collision = true

                    //indexes are top-left remember, due to how it's rendered and such.
                    val tileBottom = topY.toFloat()
                    desiredPosition.y = tileBottom + sizeMeters.y * 0.5f + epsilon
                    break
                } // else noop, move freely
            }
        }

        val jumpComponent = jumpMapper.getNullable(entity)
        if (jumpComponent != null) {
            jumpComponent.canJump = canJump
        }

        return desiredPosition
    }

    private fun maybeSendEntityMoved(entity: Int) {
        val aspectSubscriptionManager = m_world.m_artemisWorld.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(PlayerComponent::class.java))
        val entities = entitySubscription.entities

        for (i in 0..entities.size() - 1) {
            val player = entities.get(i)
            val playerComponent = playerMapper.get(player)

            //            if (playerComponent.loadedViewport.contains(new Vector2(spriteComponent.sprite.getX(),
            // spriteComponent.sprite.getY()))) {

            m_serverNetworkSystem.sendEntityMoved(player, entity)

            //           }
        }
    }

}

