package com.ore.infinium.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.Mappers;
import com.ore.infinium.World;
import com.ore.infinium.components.JumpComponent;
import com.ore.infinium.components.PlayerComponent;
import com.ore.infinium.components.SpriteComponent;
import com.ore.infinium.components.VelocityComponent;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich@kde.org>                    *
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
public class MovementSystem extends EntitySystem {
    private World m_world;

    public MovementSystem(World world) {
        m_world = world;
    }

    public void addedToEngine(Engine engine) {

    }

    public void removedFromEngine(Engine engine) {

    }

    public void update(float delta) {
        if (m_world.isServer()) {
            return;
        }

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(SpriteComponent.class, VelocityComponent.class).get());

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions
        for (Entity e : entities) {
            simulate(e, delta);
        }
    }

    private void simulate(Entity entity, float delta) {
        if (Mappers.control.get(entity) == null) {
            if (Mappers.item.get(entity) != null) {
                simulateDroppedItem(entity, delta);
            }

            return;
        }

        //fixme handle noclip
        final SpriteComponent spriteComponent = Mappers.sprite.get(entity);
        final Vector2 origPosition = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        final VelocityComponent velocityComponent = Mappers.velocity.get(entity);
        final Vector2 oldVelocity = velocityComponent.velocity;
        Vector2 newVelocity = oldVelocity;
        final Vector2 desiredDirection = Mappers.control.get(entity).desiredDirection;

        //acceleration due to gravity
        Vector2 acceleration = new Vector2(desiredDirection.x * PlayerComponent.movementSpeed, World.GRAVITY_ACCEL);

        JumpComponent jumpComponent = Mappers.jump.get(entity);
        if (jumpComponent.canJump && jumpComponent.shouldJump) {

            if (jumpComponent.jumpTimer.milliseconds() >= jumpComponent.jumpInterval) {
                //good to jump, actually do it now.
                jumpComponent.jumpTimer.reset();

                acceleration.y = PlayerComponent.jumpVelocity / 2.0f;
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

//    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2(maxMovementSpeed, 9999999999999));//(9.8f / PIXELS_PER_METER) * 4.0));
//    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2(maxMovementSpeed, (9.8f / PIXELS_PER_METER) * 4.0));

        //clamp both axes between some max/min values..
        Vector2 dt = new Vector2(delta, delta);
        newVelocity.x = MathUtils.clamp(newVelocity.x, -PlayerComponent.maxMovementSpeed, PlayerComponent.maxMovementSpeed);
        newVelocity.y = MathUtils.clamp(newVelocity.y, PlayerComponent.jumpVelocity, World.GRAVITY_ACCEL_CLAMP);

        ///////// velocity verlet integration
        // http://lolengine.net/blog/2011/12/14/understanding-motion-in-games
        Vector2 desiredPosition = origPosition.add(oldVelocity.add(newVelocity).scl(0.5f * delta));
        //OLD CODE desiredPosition = ((origPosition.xy() + (oldVelocity + newVelocity)) * glm::vec2(0.5f) * dt);

        //TODO: add threshold to nullify velocity..so we don't infinitely move and thus burn through ticks/packets

        // newVelocity will be adjusted according to the result of collisions
        velocityComponent.velocity.set(newVelocity);
        final Vector2 finalPosition = performCollision(desiredPosition, entity);

        spriteComponent.sprite.setPosition(finalPosition.x, finalPosition.y);
        //FIXME: do half-ass friction, to feel better than this. and then when movement is close to 0, 0 it.
    }

    private void simulateDroppedItem(Entity entity, float delta) {

    }

    /**
     * @param desiredPosition position we'd like to be at, given an integrated velocity + position
     * @param entity
     * @return the actual new position, after collision (if any).
     */
    private Vector2 performCollision(Vector2 desiredPosition, Entity entity) {
        boolean canJump = false;

        final SpriteComponent spriteComponent = Mappers.sprite.get(entity);
        final VelocityComponent velocityComponent = Mappers.velocity.get(entity);
        final Vector2 velocity = velocityComponent.velocity;
        final Vector2 sizeMeters = new Vector2(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

        //FIXME: this whole thing needs a way better solution, it's horrible.
        final float epsilon = World.BLOCK_SIZE * 0.01f;
        int leftX = (int) ((desiredPosition.x - sizeMeters.x * 0.5f) / World.BLOCK_SIZE);
        int rightX = (int) ((desiredPosition.x + (sizeMeters.x * 0.5f)) / World.BLOCK_SIZE);
        int topY = (int) ((desiredPosition.y - sizeMeters.y * 0.5f) / World.BLOCK_SIZE);
        int bottomY = (int) ((desiredPosition.y + (sizeMeters.y * 0.5f)) / World.BLOCK_SIZE);

        int oldTopY = topY;

        if ((velocity.y > 0.0f)) {
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

                    float tileRight = World.BLOCK_SIZE * (rightX - 0);

                    //HACK: super small threshold to prevent sticking to right side,
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

                    float tileLeft = World.BLOCK_SIZE * (leftX + 1);
                    desiredPosition.x = tileLeft + (sizeMeters.x * 0.5f) + epsilon;
                    break;
                } // else noop, move freely
            }
        }

        topY = oldTopY;
        // recalculate startx, etc. now that we reperformed x position
        // y was not touched, so no need
        leftX = (int) ((desiredPosition.x - (sizeMeters.x * 0.5f)) / World.BLOCK_SIZE);
        rightX = (int) ((desiredPosition.x + (sizeMeters.x * 0.5f)) / World.BLOCK_SIZE);
        collision = false;

        //qCDebug(ORE_IMPORTANT) << "y collision test: bottomy: " << bottomY << " leftX: " << leftX << " topY: " << topY << " rightX: " << rightX;

        if (velocity.y > 0.0f) {
            //try moving down, only loop over tiles on the bottom side(inclusive, remember)
            for (int x = leftX; x <= rightX; ++x) {
                if (m_world.isBlockSolid(x, bottomY + 0)) {
                    canJump = true;

                    //collision occured, stop here
                    velocity.y = 0.0f;
                    collision = true;

                    //indexes are top-left remember, due to how it's rendered and such.
                    float tileTop = World.BLOCK_SIZE * (bottomY);
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
                    float tileBottom = World.BLOCK_SIZE * (topY + 3);/// oh yeah btw this is a fucking hack, just in case this entire fucking function didn't seem like it, this + 3 definitely is.
                    desiredPosition.y = tileBottom + (sizeMeters.y * 0.5f) + epsilon;
                    break;
                } // else noop, move freely
            }
        }

        JumpComponent jumpComponent = Mappers.jump.get(entity);
        if (jumpComponent != null) {
            jumpComponent.canJump = canJump;
        }

        return desiredPosition;
    }

    private void simulateDroppedItem(Entity entity) {
    /*
    auto itemComponent = entity.component<ItemComponent>();
    if (itemComponent->state() != ItemComponent::State::DroppedInWorld) {
        return;
        //only interested in simulating gravity for dropped items
    }

    auto positionComponent = entity.component<PositionComponent>();
    auto sizeComponent = entity.component<SizeComponent>();

    auto velocityComponent = entity.component<VelocityComponent>();

    const auto itemPosition = positionComponent->position();
    const glm::vec2 itemVelocity = velocityComponent->velocity();

    int x = (itemPosition.x) / BLOCK_SIZE;
    int y = (itemPosition.y) / BLOCK_SIZE;

    auto playerWhoDropped = m_world->playerForID(itemComponent->playerIDWhoDropped());
    Q_ASSERT(playerWhoDropped.valid());

   // auto player = playerWhoDropped->getComponent<VelocityComponent>()->velocity().x;
    auto playerVelocity = m_world->m_players[0].component<VelocityComponent>();

    //NOTE: int he futre, gravity may not be a global constant
    glm::vec2 acceleration(0.0f, GRAVITY_ACCEL);

    if (itemComponent->justDropped()) {
        acceleration.x += glm::max<float>(playerVelocity->velocity().x * 0.5f, GRAVITY_ACCEL);
        acceleration.y += -GRAVITY_ACCEL * 4.0f;
        //qCDebug(ORE_IMPORTANT) << "ADDING just dropped accelerration x:" << acceleration.x;
        //only add player velocity the first tick, as soon as they drop it.
        itemComponent->setJustDropped(false);
    }


    glm::vec2 newVelocity = itemVelocity;

    newVelocity += acceleration;

    newVelocity.x *= 0.95;

    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2(maxMovementSpeed, GRAVITY_ACCEL_CLAMP));

    //clamp both axes between some max/min values..
//    newVelocity = glm::clamp(newVelocity, glm::vec2(-maxMovementSpeed, PLAYER_JUMP_VELOCITY), glm::vec2(maxMovementSpeed, 9.8f / PIXELS_PER_METER /10.0f));

    //reset velocity once it gets small enough, and consider it non-moved.
    float epsilon = 0.00001f;
    if (glm::abs(newVelocity.x) < epsilon && glm::abs(newVelocity.y) < epsilon) {
        newVelocity = glm::vec2(0.0f, 0.0f);
    } else {
        glm::vec3 desiredPosition = glm::vec3(itemPosition.xy() + (itemVelocity + newVelocity) * glm::vec2(0.5f) * glm::vec2(m_dt), itemPosition.z);
        const glm::vec3 finalPosition = performCollision(desiredPosition, sizeComponent->sizeMeters(), &newVelocity, nullptr);
        velocityComponent->setVelocity(newVelocity);

        //TODO: add threshold to nullify velocity..so we don't infinitely move and thus burn through ticks/packets

        //HACK: obviously
    //    positionComponent->setPosition(desiredPosition);

    //    const glm::vec3 finalPosition ;

        //qCDebug(ORE_IMPORTANT) << "dropped item opsition: " << desiredPosition << " Id: " << entity.getId() << " velcotiy: " << v;


        positionComponent->setPosition(finalPosition);
        maybeSendEntityMoved(entity);
    }
    */
    }

    private void maybeSendEntityMoved(Entity entity) {
    /*
    for (auto player : m_world->m_players) {
        auto playerComponent = player.component<PlayerComponent>();
        auto& viewport = playerComponent->loadedViewport();

        if (viewport.exists(entity)) {
            m_world->m_server->sendEntityMoved(player, entity);
        }
    }
    */
    }

}
