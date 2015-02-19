package com.ore.infinium.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.World;
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
public class PlayerSystem extends EntitySystem implements EntityListener {
    private World m_world;

    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ControllableComponent> controlMapper = ComponentMapper.getFor(ControllableComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<JumpComponent> jumpMapper = ComponentMapper.getFor(JumpComponent.class);


    public PlayerSystem(World world) {
        m_world = world;
    }

    public void addedToEngine(Engine engine) {
        engine.addEntityListener(Family.all(PlayerComponent.class).get(), this);
    }

    public void removedFromEngine(Engine engine) {

    }

    public void update(float delta) {
        if (m_world.isClient()) {
            return;
        }

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(PlayerComponent.class).get());

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions
        for (int i = 0; i < entities.size(); ++i) {
            SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));
            PlayerComponent playerComponent = playerMapper.get(entities.get(i));

            if (spriteComponent == null || spriteComponent.sprite == null || playerComponent == null || playerComponent.lastLoadedRegion == null) {
                continue; //hack, not sure why but occasional NPE's happen..on something
            }

            if (Vector2.dst(spriteComponent.sprite.getX(), spriteComponent.sprite.getY(), playerComponent.lastLoadedRegion.x, playerComponent.lastLoadedRegion.y)
                    > 10.0f) {
                //HACK, dunno why 20. need something sane.
                calculateLoadedViewport(entities.get(i));
            }
        }
    }

    private void calculateLoadedViewport(Entity entity) {
        PlayerComponent playerComponent = playerMapper.get(entity);
        SpriteComponent spriteComponent = spriteMapper.get(entity);


        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        Vector2 center = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        loadedViewport.centerOn(center);
        playerComponent.lastLoadedRegion = center;

        m_world.m_server.sendPlayerLoadedViewportMoved(entity);
        sendPlayerBlockRegion(entity);
    }

    private void sendPlayerBlockRegion(Entity entity) {
        PlayerComponent playerComponent = playerMapper.get(entity);
        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        int x = (int) (Math.max(0.0f, loadedViewport.rect.x - loadedViewport.rect.width) / World.BLOCK_SIZE);
        int y = (int) (Math.max(0.0f, loadedViewport.rect.y - loadedViewport.rect.height) / World.BLOCK_SIZE);
        int x2 = (int) (Math.min(World.BLOCK_SIZE * World.WORLD_COLUMNCOUNT, loadedViewport.rect.x + loadedViewport.rect.width) / World.BLOCK_SIZE);
        int y2 = (int) (Math.min(World.BLOCK_SIZE * World.WORLD_ROWCOUNT, loadedViewport.rect.y + loadedViewport.rect.height) / World.BLOCK_SIZE);

        m_world.m_server.sendPlayerBlockRegion(entity, x, y, x2, y2);
    }

    @Override
    public void entityAdded(Entity entity) {
        if (m_world.isClient()) {
            return;
        }

        //initial spawn, send region
        calculateLoadedViewport(entity);
        sendPlayerBlockRegion(entity);
    }

    @Override
    public void entityRemoved(Entity entity) {

    }
}
