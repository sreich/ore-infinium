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
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreSettings
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.*

@Wire(failOnNull = false)
class MovementSystem(private val oreWorld: OreWorld) : IteratingSystem(Aspect.all()) {

    private val mSprite by require<SpriteComponent>()
    private val mVelocity by require<VelocityComponent>()
    private val mPlayer by mapper<PlayerComponent>()
    private val mControl by mapper<ControllableComponent>()
    private val mItem by mapper<ItemComponent>()
    private val mJump by mapper<JumpComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tagManager by system<TagManager>()

    //lowest value we want to represent before killing velocity
    private val VELOCITY_MINIMUM_CUTOFF = 0.008f

    companion object {
        val GRAVITY_ACCEL = 1.2f

        //max velocity that can be obtained via gravity
        val GRAVITY_VELOCITY_CLAMP = 0.6f
    }

    override fun process(entityId: Int) {
        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions

        //fixme maybe make a dropped component?
        if (oreWorld.isServer()) {
            if (oreWorld.isItemDroppedInWorldOpt(entityId)) {
                simulateDroppedItem(entityId, getWorld().delta)
            }
        }

        if (!oreWorld.isServer()) {
            //server doesn't process this(physics). client tells us where they are.
            //fixme, though we do need to eventually at least half-ass verify it, which
            //means doing it on server as well. and only players and stuff should get simulated
            //by the client. the rest has to be the server
            simulate(entityId, getWorld().delta)

            recenterCameraOnPlayer()

            clientNetworkSystem.sendPlayerMoved()
        }
    }

    private fun recenterCameraOnPlayer() {
        val mainPlayer = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerSprite = mSprite.get(mainPlayer)
        oreWorld.camera.position.set(playerSprite.sprite.x, playerSprite.sprite.y, 0f)
        oreWorld.camera.update()
    }

    /**
     * simulate physics against the world for this entity
     *
     * the desiredPosition var uses velocity verlet integration
     * http://lolengine.net/blog/2011/12/14/understanding-motion-in-games
     */
    private fun simulate(entity: Int, delta: Float) {
        //it isn't a player or npc or anything that can be controlled
        if (!mControl.has(entity)) {
            return
        }

        val cSprite = mSprite.get(entity)
        val cVelocity = mVelocity.get(entity)

        attemptSpeedRunCheat()

        val oldVelocity = Vector2(cVelocity.velocity)
        var newVelocity = Vector2(oldVelocity)

        //could be a player or something else..but we're only interested if this entity is a player
        //for enabling no clipping on him

        val desiredDirection = mControl.get(entity).desiredDirection

        val acceleration = Vector2()
        //x movement has an initial ramp up time until it hits max speed (similar feel as terraria)
        acceleration.x = ((desiredDirection.x * PlayerComponent.maxMovementSpeed) - oldVelocity.x) *
                PlayerComponent.movementRampUpFactor
        //acceleration due to gravity
        acceleration.y = GRAVITY_ACCEL

        acceleration.y = maybePerformJump(acceleration.y, playerEntity = entity)

        newVelocity = newVelocity.add(acceleration.x * delta, acceleration.y * delta)

        val noClip = shouldNoClip(entity)
        //cancel out gravity, if in noclip mode
        if (noClip) {
            acceleration.y = 0f
            newVelocity.y = 0f
            //allow simple movement up/down...doesn't use fancy ramp up though
            //may be able to once we get ladders implemented
            newVelocity.y = desiredDirection.y * PlayerComponent.maxMovementSpeed
        }

        if (desiredDirection.x == 0f) {
            //bleed velocity a bit, due to friction
            newVelocity.x *= 0.6f
        }

        //gets small enough velocity, cease movement/sleep object.
        //on both x and y, independently
        //so we don't infinitely move and thus burn through ticks/packets
        //and don't send anything if not dirty
        if (newVelocity.x.abs() < VELOCITY_MINIMUM_CUTOFF) {
            newVelocity.x = 0f
        }
        if (newVelocity.y.abs() < VELOCITY_MINIMUM_CUTOFF) {
            newVelocity.y = 0f
        }

        if (!noClip) {
            newVelocity.y = newVelocity.y.coerceAtMost(GRAVITY_VELOCITY_CLAMP)
        }

        //todo  clamp both axes between some max/min values..
        cVelocity.velocity.set(newVelocity.x, newVelocity.y)

        val origPosition = Vector2(cSprite.sprite.x, cSprite.sprite.y)

        // newVelocity is now invalid, note (vector reference modification).
        val desiredPosition = origPosition.add(oldVelocity.add(newVelocity.scl(0.5f * delta)))

        val finalPosition = if (noClip) {
            //just set where we expect we should be/want to be (ignore collision)
            desiredPosition
        } else {
            val entityCollisionPosition = performEntitiesCollision(desiredPosition, entity)
            performBlockCollision(entityCollisionPosition, entity)
        }

        cSprite.sprite.setPosition(finalPosition.x, finalPosition.y)
        //FIXME: do half-ass friction, to feel better than this. and then when movement is close to 0, 0 it.
    }

    /**
     * @return if this entity id should no clip (if it's a player,
     * and noclip enabled)
     */
    private fun shouldNoClip(entityId: Int) =
            OreSettings.noClip && mPlayer.has(entityId)

    /**
     * sets player max movement according to if speed run cheat is enabled or not
     */
    private fun attemptSpeedRunCheat() {
        //cheat
        if (OreSettings.speedRun) {
            // reset velocity each time, or it'll be too fast for bleed off
            //cVelocity.velocity.setZero()
            PlayerComponent.maxMovementSpeed = PlayerComponent.NORMAL_MOVEMENT_SPEED + 1f
            PlayerComponent.movementRampUpFactor = PlayerComponent.NORMAL_MOVEMENT_RAMP_UP_FACTOR * 1.5f
        } else {
            PlayerComponent.maxMovementSpeed = PlayerComponent.NORMAL_MOVEMENT_SPEED
            PlayerComponent.movementRampUpFactor = PlayerComponent.NORMAL_MOVEMENT_RAMP_UP_FACTOR
        }
    }

    private fun maybePerformJump(acceleration: Float, playerEntity: Int): Float {
        var newAcceleration = acceleration
        val cJump = mJump.get(playerEntity)
        if (cJump.canJump && cJump.shouldJump) {

            if (cJump.jumpTimer.resetIfSurpassed(cJump.jumpInterval)) {
                //good to jump, actually do it now.
                newAcceleration = -PlayerComponent.jumpVelocity
            }
        }

        cJump.canJump = false
        cJump.shouldJump = false

        return newAcceleration
    }

    private fun simulateNoClip(entity: Int, delta: Float) {
        val cSprite = mSprite.get(entity)
        val desiredDirection = mControl.get(entity).desiredDirection

        val x = cSprite.sprite.x
        val y = cSprite.sprite.y
        //       cSprite.sprite.setPosition(
//                x + desiredDirection.x * PlayerComponent.startingMovementSpeed * 6.0f * delta,
        //             y + desiredDirection.y * PlayerComponent.startingMovementSpeed * 6.0f * delta)
    }

    private fun simulateDroppedItem(item: Int, delta: Float) {
        val cItem = mItem.get(item)
        assert(cItem.state == ItemComponent.State.DroppedInWorld)

        val cSprite = mSprite.get(item)
        val cVelocity = mVelocity.get(item)

        val cPosition = Vector2(cSprite.sprite.x, cSprite.sprite.y)

        val x = cPosition.x.toInt()
        val y = cPosition.y.toInt()

        val itemOldVelocity = Vector2(cVelocity.velocity)
        val itemNewVelocity = Vector2(cVelocity.velocity)

        val acceleration = Vector2(0.0f, GRAVITY_ACCEL)

        if (cItem.justDropped) {
            //TODO
            val playerEntityWhoDropped = oreWorld.playerEntityForPlayerConnectionID(cItem.playerIdWhoDropped!!)
            val playerVelocityComponent = mVelocity.get(playerEntityWhoDropped)
            val playerVelocity = Vector2(playerVelocityComponent.velocity)

            //acceleration.x += Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);
            //acceleration.x Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);

            //only add player velocity the firstEntity tick, as soon as they drop it.
            //so that we can throw things harder using players current speed
            cItem.justDropped = false
            acceleration.y += -GRAVITY_ACCEL * 3.0f
            acceleration.x += 0.5f
        }

        itemNewVelocity.add(acceleration)

        itemNewVelocity.x *= 0.55f

        itemNewVelocity.x = (itemNewVelocity.x).coerceIn(-PlayerComponent.maxMovementSpeed,
                                                         PlayerComponent.maxMovementSpeed)
        //        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World
        // .GRAVITY_ACCEL_CLAMP);
        itemNewVelocity.y = (itemNewVelocity.y).coerceIn(-GRAVITY_VELOCITY_CLAMP * 10,
                                                         GRAVITY_VELOCITY_CLAMP)

        //gets small enough velocity, cease movement/sleep object.
        //on both x and y, independently
        if (itemNewVelocity.x.abs() < VELOCITY_MINIMUM_CUTOFF) {
            itemNewVelocity.x = 0f
        }
        if (itemNewVelocity.y.abs() < VELOCITY_MINIMUM_CUTOFF) {
            itemNewVelocity.y = 0f
        }

        if (!itemNewVelocity.isZero) {
            cVelocity.velocity.set(itemNewVelocity)
            val desiredPosition = cPosition.add(itemOldVelocity.add(itemNewVelocity.scl(0.5f * delta)))
            val finalPosition = performBlockCollision(desiredPosition, item)

            cSprite.sprite.setPosition(finalPosition.x, finalPosition.y)
            maybeSendEntityMoved(item)
        }
    }

    /**
     *@param desiredPosition pos we'd like to be at, given integrated velocity + position
     * takes the input and if there was a collision against an entity, backs out to the
     * closest to the entity it can be.
     *
     * block collision is handled seperately.
     */
    private fun performEntitiesCollision(desiredPosition: Vector2, entity: Int): Vector2 {
        //check every other entity to collide with
        world.entities(allOf(SpriteComponent::class)).forEach {
            val cSprite = mSprite.get(entity)
            val cSpriteOther = mSprite.get(it)

            if (oreWorld.isItemDroppedInWorldOpt(it) || oreWorld.shouldIgnoreClientEntityTag(it) ||
                    cSpriteOther.noClip) {
                //ignore collision with these
                return@forEach
            }

            if (cSprite.sprite.rect.overlapsPadded(cSpriteOther.sprite.rect, entityPadding)) {
                OreWorld.log("movement system", "overlaps!! entityId: $it")
                return performEntityCollision(desiredPosition = desiredPosition, entityToMove = entity,
                                              collidingEntity = it)
            }
        }

        return desiredPosition
    }

    val entityPadding = 0.1f

    /**
     * @param collidingEntity entity that the other one is colliding against
     */
    private fun performEntityCollision(desiredPosition: Vector2, entityToMove: Int, collidingEntity: Int): Vector2 {
        val cSprite = mSprite.get(entityToMove)
        val cSpriteColliding = mSprite.get(collidingEntity)

//        val entityToMoveRect = cSprite.sprite.rect
        val collidingEntityRect = cSpriteColliding.sprite.rect

        val velocity = mVelocity.get(entityToMove).velocity
        var entityToMoveRect = desiredPosition.rectFromSize(cSprite.sprite.size.x, cSprite.sprite.size.y)

        val rightSide = rightSideTouches(collidingEntityRect, entityToMoveRect)

        //true if past bottom of moving entity
        val pastBottom = entityToMoveRect.bottom >= collidingEntityRect.top - entityPadding
        val pastLeft = entityToMoveRect.left <= collidingEntityRect.right //+ entityPadding
        val pastRight = entityToMoveRect.right >= collidingEntityRect.left //- entityPadding

        //true if bottom collision is fully satisfied and velocity should be halted
        val stopBottomCollision = pastLeft && entityToMoveRect.right >= collidingEntityRect.right

        if (velocity.x > 0f) {
            //trying to move right
            if (pastRight && entityToMoveRect.left <= collidingEntityRect.left &&
                    entityToMoveRect.bottom > collidingEntityRect.top //+ entityPadding
            ) {
                desiredPosition.x = (collidingEntityRect.left - entityToMoveRect.halfWidth) - entityPadding
                velocity.x = 0f

                //update our helper rect. fixme, this should probably be made more efficient..not making new
                //rects all the time
                entityToMoveRect = desiredPosition.rectFromSize(cSprite.sprite.size.x, cSprite.sprite.size.y)
            }
        } else if (velocity.x < 0f) {
            //trying to move left
            //ensure entity to collide with is up against our left side
            //and our other side is facing away from the colliding side
            // (or switching directions will teleport to the wrong side)
            if (pastLeft
                    && entityToMoveRect.right >= collidingEntityRect.right
                    && entityToMoveRect.bottom > collidingEntityRect.top
            ) {
                //if (entityToMoveRect.left <= collidingEntityRect.right + entityPadding
                //       && entityToMoveRect.right >= collidingEntityRect.right) {
                desiredPosition.x = (collidingEntityRect.right + entityToMoveRect.halfWidth) + entityPadding
                velocity.x = 0f

                entityToMoveRect = desiredPosition.rectFromSize(cSprite.sprite.size.x, cSprite.sprite.size.y)
            }
        }

        if (velocity.y > 0f) {
            //trying to move down
            /*
            if (pastBottom && entityToMoveRect.top <= collidingEntityRect.bottom
                    && (pastLeft && pastRight) ||
                    (entityToMoveRect.left <= collidingEntityRect.left &&
                            entityToMoveRect.right >= collidingEntityRect.right)
//                    && (!pastRight || !pastLeft)
            ) {
            */
            if (entityToMoveRect.bottom > collidingEntityRect.top - entityPadding
                    && (entityToMoveRect.right > collidingEntityRect.left + entityPadding
                    && entityToMoveRect.left < collidingEntityRect.right - entityPadding)
            ) {
                desiredPosition.y = (collidingEntityRect.top - entityToMoveRect.halfHeight) - entityPadding
                velocity.y = 0f
            }
        } else if (velocity.y < 0f) {
            //trying to move up
            /*
            if (entityToMoveRect.top <= collidingEntityRect.bottom + entityPadding
                    && entityToMoveRect.bottom >= collidingEntityRect.top) {
                desiredPosition.x = (collidingEntityRect.right + entityToMoveRect.halfWidth) + entityPadding
                velocity.y = 0f
                */
        }

        return desiredPosition
    }

    fun rightSideTouches(collidingEntityRect: Rectangle, entityToMoveRect: Rectangle): Boolean {
        return entityToMoveRect.right > collidingEntityRect.left - entityPadding
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
    private fun performBlockCollision(desiredPosition: Vector2, entity: Int): Vector2 {
        val cSprite = mSprite.get(entity)
        val velocity = mVelocity.get(entity).velocity
        val sizeMeters = cSprite.sprite.size

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

        var walkedUpSingleBlock = false
        if (velocity.x > 0.0f) {
            //try moving right, only loop over tiles on the right side
            for (y in topY until bottomY) {
                if (oreWorld.isBlockSolid(rightX, y)) {
                    val tileRight = (rightX - 0).toFloat()

                    if (y == bottomY - 1) {
                        //we're at the last iteration, closest to the bottom,
                        //this is the only block solid and it's on our feet,
                        //we can walk over it
                        //todo i bet this breaks awfully when some block is above you
                        desiredPosition.x = tileRight - 1f
                        desiredPosition.y -= 1f
                        velocity.y = 0f
                        walkedUpSingleBlock = true
                        break
                    }

                    velocity.x = 0.0f

                    //fixme: super small threshold to prevent sticking to right side,
                    desiredPosition.x = tileRight - sizeMeters.x * 0.5f - epsilon
                    break
                } // else no collision
            }
        } else if (velocity.x < 0.0f) {
            //try moving left, only loop over tiles on the left side
            for (y in topY until bottomY) {
                if (oreWorld.isBlockSolid(leftX, y)) {

                    val tileLeft = (leftX + 1).toFloat()

                    if (y == bottomY - 1) {
                        //we're at the last iteration, closest to the bottom,
                        //this is the only block solid and it's on our feet,
                        //we can walk over it
                        //todo i bet this breaks awfully when some block is above you
                        desiredPosition.x = tileLeft + 1f
                        desiredPosition.y -= 1f
                        velocity.y = 0f
                        walkedUpSingleBlock = true
                        break
                    }

                    velocity.x = 0.0f

                    desiredPosition.x = tileLeft + sizeMeters.x * 0.5f + epsilon
                    break
                } // else no collision
            }
        }

        topY = oldTopY
        // recalculate startx, etc. now that we reperformed x position
        // y was not touched, so no need
        leftX = (desiredPosition.x - sizeMeters.x * 0.5f).toInt()
        rightX = (desiredPosition.x + sizeMeters.x * 0.5f).toInt()
        var canJump = false

        if (velocity.y > 0.0f) {
            //try moving down, only loop over tiles on the bottom side(inclusive, remember)
            for (x in leftX..rightX) {
                if (oreWorld.isBlockSolid(x, bottomY + 0)) {
                    canJump = true
                    if (walkedUpSingleBlock) {
                        break
                    }

                    //collision occured, stop here
                    velocity.y = 0.0f

                    //indexes are top-left remember, due to how it's rendered and such.
                    val tileTop = bottomY.toFloat()
                    desiredPosition.y = tileTop - sizeMeters.y * 0.5f
                    break
                } // else noop, move freely
            }
        } else if (velocity.y < 0.0f) {
            //try moving up, only loop over tiles on the bottom side(inclusive, remember)
            for (x in leftX..rightX) {
                if (oreWorld.isBlockSolid(x, topY - 1)) {
                    //collision occured, stop here
                    velocity.y = 0.0f

                    //indexes are top-left remember, due to how it's rendered and such.
                    val tileBottom = topY.toFloat()
                    desiredPosition.y = tileBottom + sizeMeters.y * 0.5f + epsilon
                    break
                } // else noop, move freely
            }
        }

        mJump.ifPresent(entity) {
            it.canJump = canJump
        }

        return desiredPosition
    }

    private fun maybeSendEntityMoved(entity: Int) {
        val entities = oreWorld.artemisWorld.entities(allOf(PlayerComponent::class))

        entities.forEach { player ->
            val cPlayer = mPlayer.get(player)

            //            if (cPlayer.loadedViewport.contains(new Vector2(cSprite.sprite.getX(),
            // cSprite.sprite.getY()))) {

            serverNetworkSystem.sendEntityMoved(player, entity)

            //           }
        }
    }

}


