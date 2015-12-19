package com.ore.infinium.systems;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenEquations;
import aurelienribon.tweenengine.TweenManager;
import com.artemis.*;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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

    private TagManager m_tagManager;

    private TweenManager m_tweenManager;

    public SpriteRenderSystem(OreWorld world) {
        m_world = world;
    }

    @Override
    protected void initialize() {
        m_batch = new SpriteBatch();
        m_tweenManager = new TweenManager();
        Tween.registerAccessor(Sprite.class, new SpriteTween());
        //default is 3, but color requires 4 (rgba)
        Tween.setCombinedAttributesLimit(4);
    }

    @Override
    protected void dispose() {
        m_batch.dispose();
    }

    @Override
    protected void begin() {
        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        //       m_batch.begin();
    }

    @Override
    protected void processSystem() {
        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);

        m_tweenManager.update(world.getDelta());

        m_batch.begin();
        renderEntities(world.getDelta());
        m_batch.end();

        m_batch.begin();
        renderDroppedEntities(world.getDelta());
        m_batch.end();
        //restore color
        m_batch.setColor(Color.WHITE);

    }

    @Override
    protected void end() {
        //        m_batch.end();
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
            //don't draw in-inventory or not dropped items
            if (itemComponent == null || itemComponent.state != ItemComponent.State.DroppedInWorld) {
                continue;
            }

            SpriteComponent spriteComponent = spriteMapper.get(entities.get(i));

            if (!m_tweenManager.containsTarget(spriteComponent.sprite)) {
                Tween.to(spriteComponent.sprite, SpriteTween.SCALE, 2.8f)
                     .target(0.5f, 0.5f)
                     .ease(TweenEquations.easeInOutBack)
                     .repeatYoyo(Tween.INFINITY, 0.0f)
                     .start(m_tweenManager);
/*
                Tween.to(spriteComponent.sprite, SpriteTween.COLOR, .8f)
                     .target(0, 0, 1, 0.6f)
                     .ease(TweenEquations.easeInOutBack)
                     .repeatYoyo(Tween.INFINITY, 0.0f)
                     .start(m_tweenManager);
                     */
            }

            /*
            m_batch.draw(spriteComponent.sprite,
                         spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                         spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * 0.5f),
                         spriteComponent.sprite.getWidth(), -spriteComponent.sprite.getHeight());
            */
            m_batch.setColor(spriteComponent.sprite.getColor());

            float x = spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f);
            float y = spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * 0.5f);

            //flip the sprite when drawn, by using negative height
            float scaleX = spriteComponent.sprite.getScaleX();
            float scaleY = spriteComponent.sprite.getScaleY();

            float width = spriteComponent.sprite.getWidth();
            float height = -spriteComponent.sprite.getHeight();

            float originX = width * 0.5f;
            float originY = height * 0.5f;
            //            spriteComponent.sprite.setScale(Interpolation.bounce.apply(0.0f, 0.5f, scaleX));

            m_batch.draw(spriteComponent.sprite, MathUtils.floor(x * 16.0f) / 16.0f, MathUtils.floor(y * 16.0f) / 16.0f,
                         originX, originY, width, height, scaleX, scaleY, rotation);
        }
    }

    static float rotation;

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
                //hack
                continue;
            }

            spriteComponent = spriteMapper.get(entity);

            if (!spriteComponent.visible) {
                continue;
            }

            assert spriteComponent.sprite != null : "sprite is null";
            assert spriteComponent.sprite.getTexture() != null : "sprite has null texture";

            boolean placementGhost = false;

            String tag = m_tagManager.getTag(world.getEntity(entity));
            if (tag != null && tag.equals("itemPlacementOverlay")) {

                placementGhost = true;

                if (spriteComponent.placementValid) {
                    m_batch.setColor(0, 1, 0, 0.6f);
                } else {
                    m_batch.setColor(1, 0, 0, 0.6f);
                }
            }

            float x = spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f);
            float y = spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * 0.5f);

            //flip the sprite when drawn, by using negative height
            float scaleX = 1;
            float scaleY = 1;

            float width = spriteComponent.sprite.getWidth();
            float height = -spriteComponent.sprite.getHeight();

            float originX = width * 0.5f;
            float originY = height * 0.5f;

            m_batch.draw(spriteComponent.sprite, MathUtils.floor(x * 16.0f) / 16.0f, MathUtils.floor(y * 16.0f) / 16.0f,
                         originX, originY, width, height, scaleX, scaleY, rotation);

            //reset color for next run
            if (placementGhost) {
                m_batch.setColor(1, 1, 1, 1);
            }
        }
    }

}
