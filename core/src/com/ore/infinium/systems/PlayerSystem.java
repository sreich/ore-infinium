package com.ore.infinium.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreTimer;
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

    @Override
    public void addedToEngine(Engine engine) {
        engine.addEntityListener(Family.all(PlayerComponent.class).get(), this);
    }

    @Override
    public void removedFromEngine(Engine engine) {

    }

    private OreTimer chunkTimer = new OreTimer();
    @Override
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

            if (spriteComponent == null || spriteComponent.sprite == null || playerComponent == null) {
                continue; //hack, not sure why but occasional NPE's happen..on something
            }

            com.badlogic.gdx.math.Rectangle viewportRect = playerComponent.loadedViewport.rect;
            float x = spriteComponent.sprite.getX();
            float y = spriteComponent.sprite.getY();

            //fixme hack, we'll fix this when we get to chunking and come up with a proper solution
            if (chunkTimer.milliseconds() > 600) {
                calculateLoadedViewport(entities.get(i));
                chunkTimer.reset();
            }

        }
    }

    private void calculateLoadedViewport(Entity player) {
        PlayerComponent playerComponent = playerMapper.get(player);
        SpriteComponent spriteComponent = spriteMapper.get(player);

        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        Vector2 center = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        loadedViewport.centerOn(center);

        m_world.m_server.sendPlayerLoadedViewportMoved(player);
        sendPlayerBlockRegion(player);
    }

    private void sendPlayerBlockRegion(Entity player) {
        PlayerComponent playerComponent = playerMapper.get(player);
        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        LoadedViewport.PlayerViewportBlockRegion region = loadedViewport.blockRegionInViewport();
        m_world.m_server.sendPlayerBlockRegion(player, region.x, region.y, region.width, region.height);
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
