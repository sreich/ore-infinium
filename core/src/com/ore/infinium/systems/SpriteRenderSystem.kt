package com.ore.infinium.systems

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenEquations
import aurelienribon.tweenengine.TweenManager
import aurelienribon.tweenengine.equations.Sine
import com.artemis.Aspect
import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.ore.infinium.OreWorld
import com.ore.infinium.SpriteTween
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable
import com.ore.infinium.util.getTagNullable
import com.ore.infinium.util.indices

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
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
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire
class SpriteRenderSystem(private val m_world: OreWorld) : BaseSystem(), RenderSystemMarker {
    private var m_batch: SpriteBatch? = null

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_tagManager: TagManager

    private lateinit var m_tweenManager: TweenManager

    override fun initialize() {
        m_batch = SpriteBatch()
        m_tweenManager = TweenManager()
        Tween.registerAccessor(Sprite::class.java, SpriteTween())
        //default is 3, but color requires 4 (rgba)
        Tween.setCombinedAttributesLimit(4)
    }

    override fun dispose() {
        m_batch!!.dispose()
    }

    override fun begin() {
        m_batch!!.projectionMatrix = m_world.m_camera.combined
        //       m_batch.begin();
    }

    override fun processSystem() {
        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);

        m_tweenManager.update(world.getDelta())

        m_batch!!.begin()
        renderEntities(world.getDelta())
        m_batch!!.end()

        m_batch!!.begin()
        renderDroppedEntities(world.getDelta())
        m_batch!!.end()
        //restore color
        m_batch!!.color = Color.WHITE

    }

    override fun end() {
        //        m_batch.end();
    }

    //fixme probably also droppedblocks?
    private fun renderDroppedEntities(delta: Float) {
        //fixme obviously this is very inefficient...but dunno if it'll ever be an issue.
        val aspectSubscriptionManager = world.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent::class.java))
        val entities = entitySubscription.entities

        var itemComponent: ItemComponent?
        for (i in entities.indices) {
            //hack fixmeasap
            if (entities.get(i) == 55) {
                val c = 2
            }

            itemComponent = itemMapper.getNullable(entities.get(i))
            //don't draw in-inventory or not dropped items
            if (itemComponent == null || itemComponent.state != ItemComponent.State.DroppedInWorld) {
                continue
            }

            val spriteComponent = spriteMapper.get(entities.get(i))

            if (!m_tweenManager.containsTarget(spriteComponent.sprite)) {

                Timeline.createSequence().push(
                        Tween.to(spriteComponent.sprite, SpriteTween.SCALE, 2.8f).target(0.2f, 0.2f).ease(
                                Sine.IN)).push(
                        Tween.to(spriteComponent.sprite, SpriteTween.SCALE, 2.8f).target(.5f, .5f).ease(
                                Sine.OUT)).repeatYoyo(Tween.INFINITY, 0.0f).start(m_tweenManager)

                val glow = Color.GOLDENROD
                Tween.to(spriteComponent.sprite, SpriteTween.COLOR, 2.8f).target(glow.r, glow.g, glow.b, 1f).ease(
                        TweenEquations.easeInOutSine).repeatYoyo(Tween.INFINITY, 0.0f).start(m_tweenManager)
            }

            /*
            m_batch.draw(spriteComponent.sprite,
                         spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                         spriteComponent.sprite.getY() + (spriteComponent.sprite.getHeight() * 0.5f),
                         spriteComponent.sprite.getWidth(), -spriteComponent.sprite.getHeight());
            */
            m_batch!!.color = spriteComponent.sprite.color

            val x = spriteComponent.sprite.x - spriteComponent.sprite.width * 0.5f
            val y = spriteComponent.sprite.y + spriteComponent.sprite.height * 0.5f

            //flip the sprite when drawn, by using negative height
            val scaleX = spriteComponent.sprite.scaleX
            val scaleY = spriteComponent.sprite.scaleY

            val width = spriteComponent.sprite.width
            val height = -spriteComponent.sprite.height

            val originX = width * 0.5f
            val originY = height * 0.5f
            //            spriteComponent.sprite.setScale(Interpolation.bounce.apply(0.0f, 0.5f, scaleX));

            m_batch!!.draw(spriteComponent.sprite, MathUtils.floor(x * 16.0f) / 16.0f,
                           MathUtils.floor(y * 16.0f) / 16.0f, originX, originY, width, height, scaleX, scaleY,
                           rotation)
        }
    }

    private fun renderEntities(delta: Float) {
        //todo need to exclude blocks?
        val aspectSubscriptionManager = world.aspectSubscriptionManager
        val entitySubscription = aspectSubscriptionManager.get(Aspect.all(SpriteComponent::class.java))
        val entities = entitySubscription.entities

        var itemComponent: ItemComponent?
        var spriteComponent: SpriteComponent

        for (i in entities.indices) {
            val entity = entities.get(i)

            itemComponent = itemMapper.getNullable(entity)
            //don't draw in-inventory or dropped items
            if (itemComponent != null && itemComponent.state != ItemComponent.State.InWorldState) {
                //hack
                continue
            }

            spriteComponent = spriteMapper.get(entity)

            if (!spriteComponent.visible) {
                continue
            }

            //assert(spriteComponent.sprite != null) { "sprite is null" }
            assert(spriteComponent.sprite.texture != null) { "sprite has null texture" }

            var placementGhost = false

            val tag = m_tagManager.getTagNullable(world.getEntity(entity))
            if (tag != null && tag == "itemPlacementOverlay") {

                placementGhost = true

                if (spriteComponent.placementValid) {
                    m_batch!!.setColor(0f, 1f, 0f, 0.6f)
                } else {
                    m_batch!!.setColor(1f, 0f, 0f, 0.6f)
                }
            }

            val x = spriteComponent.sprite.x - spriteComponent.sprite.width * 0.5f
            val y = spriteComponent.sprite.y + spriteComponent.sprite.height * 0.5f

            //flip the sprite when drawn, by using negative height
            val scaleX = 1f
            val scaleY = 1f

            val width = spriteComponent.sprite.width
            val height = -spriteComponent.sprite.height

            val originX = width * 0.5f
            val originY = height * 0.5f

            //this prevents some jiggling of static items when player is moving, when the objects pos is
            // not rounded to a reasonable flat number,
            //but for the player it means they jiggle on all movement.
            //m_batch.draw(spriteComponent.sprite, MathUtils.floor(x * 16.0f) / 16.0f, MathUtils.floor(y * 16.0f) /
            // 16.0f,
            //            originX, originY, width, height, scaleX, scaleY, rotation);

            m_batch!!.draw(spriteComponent.sprite, x, y, originX, originY, width, height, scaleX, scaleY, rotation)

            //reset color for next run
            if (placementGhost) {
                m_batch!!.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    companion object {
        var spriteCount: Int = 0

        internal var rotation: Float = 0f
    }

}
