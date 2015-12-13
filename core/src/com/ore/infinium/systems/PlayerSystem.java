package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreTimer;
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
public class PlayerSystem extends IteratingSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;

    private OreTimer chunkTimer = new OreTimer();

    public PlayerSystem(OreWorld world) {
        super(Aspect.one(PlayerComponent.class));

        m_world = world;

    }

    @Override
    protected void inserted(int entityId) {
        super.inserted(entityId);

        //client does nothing as of yet, with this
        if (m_world.isClient()) {
            return;
        }

        //initial spawn, send region
        calculateLoadedViewport(entityId);
        sendPlayerBlockRegion(entityId);
    }

    @Override
    protected void removed(int entityId) {
        super.removed(entityId);

    }

    @Override
    protected void process(int entityId) {
        if (m_world.isClient()) {
            return;
        }

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions

        //should never ever, ever happen.
        assert spriteMapper.has(entityId) && playerMapper.has(entityId);

        SpriteComponent spriteComponent = spriteMapper.getSafe(entityId);
        PlayerComponent playerComponent = playerMapper.get(entityId);

        com.badlogic.gdx.math.Rectangle viewportRect = playerComponent.loadedViewport.rect;
        float x = spriteComponent.sprite.getX();
        float y = spriteComponent.sprite.getY();

        //fixme fixme, we'll fix this when we get to chunking and come up with a proper solution
        if (chunkTimer.milliseconds() > 600) {
            calculateLoadedViewport(entityId);
            chunkTimer.reset();
        }
    }

    private void calculateLoadedViewport(int playerEntity) {
        PlayerComponent playerComponent = playerMapper.get(playerEntity);
        SpriteComponent spriteComponent = spriteMapper.get(playerEntity);

        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        Vector2 center = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        loadedViewport.centerOn(center);

        m_networkServerSystem.sendPlayerLoadedViewportMoved(playerEntity);
        sendPlayerBlockRegion(playerEntity);
    }

    private void sendPlayerBlockRegion(int playerEntity) {
        PlayerComponent playerComponent = playerMapper.get(playerEntity);
        LoadedViewport loadedViewport = playerComponent.loadedViewport;

        LoadedViewport.PlayerViewportBlockRegion region = loadedViewport.blockRegionInViewport();

        m_networkServerSystem.sendPlayerBlockRegion(playerEntity, region.x, region.y, region.width, region.height);
    }

}
