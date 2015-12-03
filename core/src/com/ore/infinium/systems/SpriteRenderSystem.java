package com.ore.infinium.systems;

import com.artemis.*;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
@Wire
public class SpriteRenderSystem extends BaseSystem implements RenderSystemMarker {
    public static int spriteCount;

    private OreWorld m_world;
    private SpriteBatch m_batch;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    public SpriteRenderSystem(OreWorld world) {
        m_world = world;
    }

    @Override
    protected void initialize() {
        m_batch = new SpriteBatch();
    }

    @Override
    protected void dispose() {
        m_batch.dispose();
    }

    @Override
    protected void begin() {
        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.begin();
    }

    @Override
    protected void processSystem() {
        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);

        renderEntities(world.getDelta());
        renderDroppedEntities(world.getDelta());

    }

    @Override
    protected void end() {
        m_batch.end();
    }

    //fixme probably also droppedblocks?
    private void renderDroppedEntities(float delta) {
        //fixme obviously this is very inefficient...but dunno if it'll ever be an issue.
        AspectSubscriptionManager aspectSubscriptionManager = world.getAspectSubscriptionManager();
        EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent.class));
        IntBag entities = entitySubscription.getEntities();

        ItemComponent itemComponent;
        for (int i = 0; i < entities.size(); ++i) {
            itemComponent = itemMapper.getSafe(entities.get(i));
            //don't draw in-inventory or dropped items
            if (itemComponent == null || itemComponent.state != ItemComponent.State.DroppedInWorld) {
                continue;
            }

            SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));

            m_batch.draw(spriteComponent.sprite,
                         spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                         spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                         spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
        }
    }

    private void renderEntities(float delta) {
        //todo need to exclude blocks?
        AspectSubscriptionManager aspectSubscriptionManager = world.getAspectSubscriptionManager();
        EntitySubscription entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent.class));
        IntBag entities = entitySubscription.getEntities();

        ItemComponent itemComponent;
        SpriteComponent spriteComponent;

        for (int i = 0; i < entities.size(); ++i) {
            int entity = entities.get(i);

            itemComponent = itemMapper.getSafe(entity);
            //don't draw in-inventory or dropped items
            if (itemComponent != null && itemComponent.state != ItemComponent.State.InWorldState) {
                continue;
            }

            spriteComponent = spriteMapper.get(entity);

            if (!spriteComponent.visible) {
                continue;
            }

            assert spriteComponent.sprite != null;

            boolean placementGhost = false;

            String tag = world.getSystem(TagManager.class).getTag(world.getEntity(entity));
            if (tag.equals("itemPlacementOverlay")) {

                placementGhost = true;

                if (spriteComponent.placementValid) {
                    m_batch.setColor(0, 1, 0, 0.6f);
                } else {
                    m_batch.setColor(1, 0, 0, 0.6f);
                }
            }

            m_batch.draw(spriteComponent.sprite,
                         spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                         spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                         spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

            if (placementGhost) {
                m_batch.setColor(1, 1, 1, 1);
            }
        }
    }
}
