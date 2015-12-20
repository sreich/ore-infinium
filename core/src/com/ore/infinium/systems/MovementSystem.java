package com.ore.infinium.systems;

import com.artemis.*;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire(failOnNull = false)
public class MovementSystem extends IteratingSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;
    private NetworkClientSystem m_networkClientSystem;

    private TagManager m_tagManager;

    public MovementSystem(OreWorld world) {
        super(Aspect.all(SpriteComponent.class, VelocityComponent.class));
        m_world = world;

    }

    @Override
    protected void setWorld(World world) {
        super.setWorld(world);
    }

    @Override
    protected void initialize() {

    }

    @Override
    protected void process(int entityId) {
        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions
        simulate(entityId, this.getWorld().delta);

        if (m_world.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            int mainPlayer = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();
            SpriteComponent playerSprite = spriteMapper.get(mainPlayer);
            m_world.m_camera.position.set(playerSprite.sprite.getX(), playerSprite.sprite.getY(), 0);
            m_world.m_camera.update();

            m_networkClientSystem.sendPlayerMoved();
        }
    }

    private void simulate(int entity, float delta) {
        //fixme maybe make a dropped component?
        if (m_world.worldInstanceType == OreWorld.WorldInstanceType.Server) {
            ItemComponent itemComponent = itemMapper.getSafe(entity);
            if (itemComponent != null && itemComponent.state == ItemComponent.State.DroppedInWorld) {
                simulateDroppedItem(entity, delta);
            }

            //server doesn't process past here. client tells us where they are.
            //fixme, though we do need to eventually at least half-ass verify it, which
            //means doing it on server as well
            return;
        }

        //it isn't a player or npc or anything that can be controlled
        if (!controlMapper.has(entity)) {
            return;
        }

        //fixme handle noclip
        final SpriteComponent spriteComponent = spriteMapper.get(entity);
        final Vector2 origPosition = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());

        final VelocityComponent velocityComponent = velocityMapper.get(entity);

        final Vector2 oldVelocity = new Vector2(velocityComponent.velocity);
        Vector2 newVelocity = new Vector2(oldVelocity);

        final Vector2 desiredDirection = controlMapper.get(entity).desiredDirection;

        //acceleration due to gravity
        Vector2 acceleration = new Vector2(desiredDirection.x * PlayerComponent.movementSpeed, OreWorld.GRAVITY_ACCEL);

        JumpComponent jumpComponent = jumpMapper.get(entity);
        if (jumpComponent.canJump && jumpComponent.shouldJump) {

            if (jumpComponent.jumpTimer.milliseconds() >= jumpComponent.jumpInterval) {
                //good to jump, actually do it now.
                jumpComponent.jumpTimer.reset();

                acceleration.y = -PlayerComponent.jumpVelocity;
            }
        }

        jumpComponent.canJump = false;
        jumpComponent.shouldJump = false;

        newVelocity = newVelocity.add(acceleration.x * delta, acceleration.y * delta);

        final float epsilon = 0.00001f;
        if (Math.abs(newVelocity.x) < epsilon && Math.abs(newVelocity.y) < epsilon) {
            //gets small enough velocity, cease movement/sleep object.
            newVelocity.set(0.0f, 0.0f);
        }

        newVelocity.x *= 0.8f;

        //    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2
        // (maxMovementSpeed, 9999999999999));//(9.8f) * 4.0));
        //    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2
        // (maxMovementSpeed, (9.8f ) * 4.0));

        //clamp both axes between some max/min values..
        Vector2 dt = new Vector2(delta, delta);
        //        newVelocity.x = MathUtils.clamp(newVelocity.x, -PlayerComponent.maxMovementSpeed, PlayerComponent
        // .maxMovementSpeed);
        //        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World
        // .GRAVITY_ACCEL_CLAMP);

        ///////// velocity verlet integration
        // http://lolengine.net/blog/2011/12/14/understanding-motion-in-games

        //fixme if i do 0.5f * delta, it doesn't move at all??
        // * delta
        //OLD CODE desiredPosition = ((origPosition.xy() + (oldVelocity + newVelocity)) * glm::vec2(0.5f) * dt);

        //TODO: add threshold to nullify velocity..so we don't infinitely move and thus burn through ticks/packets

        velocityComponent.velocity.set(newVelocity.x, newVelocity.y);

        // newVelocity is now invalid, note.
        Vector2 desiredPosition = origPosition.add(oldVelocity.add(newVelocity.scl(0.5f * delta)));
        final Vector2 finalPosition = performCollision(desiredPosition, entity);

        spriteComponent.sprite.setPosition(finalPosition.x, finalPosition.y);
        //        Gdx.app.log("player pos", finalPosition.toString());

        //FIXME: do half-ass friction, to feel better than this. and then when movement is close to 0, 0 it.
    }

    private void simulateDroppedItem(int item, float delta) {
        ItemComponent itemComponent = itemMapper.get(item);
        assert itemComponent.state == ItemComponent.State.DroppedInWorld;

        SpriteComponent itemSpriteComponent = spriteMapper.get(item);
        VelocityComponent itemVelocityComponent = velocityMapper.get(item);

        Vector2 itemPosition = new Vector2(itemSpriteComponent.sprite.getX(), itemSpriteComponent.sprite.getY());

        int x = (int) itemPosition.x;
        int y = (int) itemPosition.y;

        int playerEntityWhoDropped = m_world.playerForID(itemComponent.playerIdWhoDropped);

        VelocityComponent playerVelocityComponent = velocityMapper.get(playerEntityWhoDropped);
        Vector2 playerVelocity = new Vector2(playerVelocityComponent.velocity);

        Vector2 acceleration = new Vector2(0.0f, OreWorld.GRAVITY_ACCEL);

        if (itemComponent.justDropped) {
            //acceleration.x += Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);
            acceleration.x += 2;//Math.max(playerVelocity.x * 0.5f, World.GRAVITY_ACCEL);
            acceleration.y += -OreWorld.GRAVITY_ACCEL * 8.0f;

            //only add player velocity the firstEntity tick, as soon as they drop it.
            itemComponent.justDropped = false;
        }

        final Vector2 itemOldVelocity = new Vector2(itemVelocityComponent.velocity);
        Vector2 itemNewVelocity = new Vector2(itemVelocityComponent.velocity);

        itemNewVelocity.add(acceleration);

        itemNewVelocity.x *= 0.95f;

        itemNewVelocity.x =
                MathUtils.clamp(itemNewVelocity.x, -PlayerComponent.maxMovementSpeed, PlayerComponent.maxMovementSpeed);
        //        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World
        // .GRAVITY_ACCEL_CLAMP);
        itemNewVelocity.y =
                MathUtils.clamp(itemNewVelocity.y, -OreWorld.GRAVITY_ACCEL_CLAMP * 10, OreWorld.GRAVITY_ACCEL_CLAMP);

        //clamp both axes between some max/min values..
        //    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2
        // (maxMovementSpeed, 9.8f /10.0f));

        //reset velocity once it gets small enough, and consider it non-moved.
        float epsilon = 0.00001f;
        if (Math.abs(itemNewVelocity.x) < epsilon && Math.abs(itemNewVelocity.y) < epsilon) {
            itemNewVelocity.setZero();
        } else {
            itemVelocityComponent.velocity.set(itemNewVelocity);
            Vector2 desiredPosition = itemPosition.add(itemOldVelocity.add(itemNewVelocity.scl(0.5f * delta)));
            Vector2 finalPosition = performCollision(desiredPosition, item);

            itemSpriteComponent.sprite.setPosition(finalPosition.x, finalPosition.y);
            maybeSendEntityMoved(item);
        }
    }

    /**
     * @param desiredPosition
     *         position we'd like to be at, given an integrated velocity + position
     * @param entity
     *
     * @return the actual new position, after collision (if any).
     */
    private Vector2 performCollision(Vector2 desiredPosition, int entity) {
        boolean canJump = false;

        final SpriteComponent spriteComponent = spriteMapper.get(entity);
        final VelocityComponent velocityComponent = velocityMapper.get(entity);
        final Vector2 velocity = velocityComponent.velocity;
        final Vector2 sizeMeters = new Vector2(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

        //FIXME: this whole thing needs a way better solution, it's horrible.
        final float epsilon = 1 * 0.01f;
        int leftX = (int) ((desiredPosition.x - sizeMeters.x * 0.5f));
        int rightX = (int) ((desiredPosition.x + (sizeMeters.x * 0.5f)));
        int topY = (int) ((desiredPosition.y - sizeMeters.y * 0.5f));
        int bottomY = (int) ((desiredPosition.y + (sizeMeters.y * 0.5f)));

        int oldTopY = topY;

        if (velocity.y > 0.0f) {
            //we are not falling/moving up
            topY += 1;
        }

        boolean collision = false;
        if (velocity.x > 0.0f) {
            //try moving right, only loop over tiles on the right side(inclusive, remember)
            for (int y = topY; y <= bottomY - 1; ++y) {
                if (m_world.isBlockSolid(rightX, y)) {
                    velocity.x = 0.0f;
                    collision = true;

                    float tileRight = (rightX - 0);

                    //fixme: super small threshold to prevent sticking to right side,
                    //i dont know why this isn't the same case as all the others. i feel like something
                    //is wrong somewhere else..doesn't make sense.
                    desiredPosition.x = tileRight - (sizeMeters.x * 0.5f) - epsilon;
                    break;
                } // else noop, move freely
            }
        } else if (velocity.x < 0.0f) {
            //try moving left, only loop over tiles on the left side
            for (int y = topY; y <= bottomY - 1; ++y) {
                if (m_world.isBlockSolid(leftX, y)) {

                    velocity.x = 0.0f;
                    collision = true;

                    float tileLeft = (leftX + 1);
                    desiredPosition.x = tileLeft + (sizeMeters.x * 0.5f) + epsilon;
                    break;
                } // else noop, move freely
            }
        }

        topY = oldTopY;
        // recalculate startx, etc. now that we reperformed x position
        // y was not touched, so no need
        leftX = (int) ((desiredPosition.x - (sizeMeters.x * 0.5f)));
        rightX = (int) ((desiredPosition.x + (sizeMeters.x * 0.5f)));
        collision = false;

        //qCDebug(ORE_IMPORTANT) << "y collision test: bottomy: " << bottomY << " leftX: " << leftX << " topY: " <<
        // topY << " rightX: " << rightX;

        if (velocity.y > 0.0f) {
            //try moving down, only loop over tiles on the bottom side(inclusive, remember)
            for (int x = leftX; x <= rightX; ++x) {
                if (m_world.isBlockSolid(x, bottomY + 0)) {
                    canJump = true;

                    //collision occured, stop here
                    velocity.y = 0.0f;
                    collision = true;

                    //indexes are top-left remember, due to how it's rendered and such.
                    float tileTop = (bottomY);
                    desiredPosition.y = tileTop - (sizeMeters.y * 0.5f);
                    break;
                } // else noop, move freely
            }
        } else if (velocity.y < 0.0f) {
            //try moving up, only loop over tiles on the bottom side(inclusive, remember)
            for (int x = leftX; x <= rightX; ++x) {
                if (m_world.isBlockSolid(x, topY - 1)) {
                    //collision occured, stop here
                    velocity.y = 0.0f;
                    collision = true;

                    //indexes are top-left remember, due to how it's rendered and such.
                    float tileBottom = topY;
                    desiredPosition.y = tileBottom + (sizeMeters.y * 0.5f) + epsilon;
                    break;
                } // else noop, move freely
            }
        }

        JumpComponent jumpComponent = jumpMapper.getSafe(entity);
        if (jumpComponent != null) {
            jumpComponent.canJump = canJump;
        }

        return desiredPosition;
    }

    private void maybeSendEntityMoved(int entity) {
        AspectSubscriptionManager aspectSubscriptionManager = m_world.m_artemisWorld.getAspectSubscriptionManager();
        EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all(PlayerComponent.class));
        IntBag entities = entitySubscription.getEntities();

        for (int i = 0; i < entities.size(); ++i) {
            int player = entities.get(i);
            PlayerComponent playerComponent = playerMapper.get(player);

            //            if (playerComponent.loadedViewport.contains(new Vector2(spriteComponent.sprite.getX(),
            // spriteComponent.sprite.getY()))) {

            m_networkServerSystem.sendEntityMoved(player, entity);

            //           }
        }
    }

}
