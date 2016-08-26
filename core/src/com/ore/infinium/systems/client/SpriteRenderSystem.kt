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

package com.ore.infinium.systems.client

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenEquations
import aurelienribon.tweenengine.TweenManager
import aurelienribon.tweenengine.equations.Sine
import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.utils.IntBag
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ore.infinium.OreWorld
import com.ore.infinium.SpriteTween
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.util.*

@Wire
class SpriteRenderSystem(private val oreWorld: OreWorld) : BaseSystem(), RenderSystemMarker {
    private lateinit var batch: SpriteBatch

    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()

    private val tagManager by system<TagManager>()
    private lateinit var tweenManager: TweenManager

    override fun initialize() {
        batch = SpriteBatch()
        tweenManager = TweenManager()
        Tween.registerAccessor(Sprite::class.java, SpriteTween())
        //default is 3, but color requires 4 (rgba)
        Tween.setCombinedAttributesLimit(4)

        world.aspectSubscriptionManager.get(allOf(SpriteComponent::class)).addSubscriptionListener(
                SpriteEntityListener())
    }

    override fun dispose() {
        batch.dispose()
    }

    override fun begin() {
        batch.projectionMatrix = oreWorld.camera.combined
        //       batch.begin();
    }

    override fun processSystem() {
        //        batch.setProjectionMatrix(oreWorld.camera.combined);
        tweenManager.update(world.getDelta())

        batch.begin()
        renderEntities(world.getDelta())
        batch.end()

        batch.begin()
        renderDroppedEntities(world.getDelta())
        batch.end()
        //restore color
        batch.color = Color.WHITE

    }

    override fun end() {
        //        batch.end();
    }

    //fixme probably also droppedblocks?
    private fun renderDroppedEntities(delta: Float) {
        //fixme obviously this is very inefficient...but dunno if it'll ever be an issue.
        val entities = world.entities(allOf(SpriteComponent::class))

        var cItem: ItemComponent?
        for (i in entities.indices) {
            cItem = mItem.opt(entities.get(i))
            //don't draw in-inventory or not dropped items
            if (cItem == null || cItem.state != ItemComponent.State.DroppedInWorld) {
                continue
            }

            val cSprite = mSprite.get(entities.get(i))

            glowDroppedSprite(cSprite.sprite)

            /*
            batch.draw(cSprite.sprite,
                         cSprite.sprite.getX() - (cSprite.sprite.getWidth() * 0.5f),
                         cSprite.sprite.getY() + (cSprite.sprite.getHeight() * 0.5f),
                         cSprite.sprite.getWidth(), -cSprite.sprite.getHeight());
            */
            batch.color = cSprite.sprite.color

            val x = cSprite.sprite.x - cSprite.sprite.width * 0.5f
            val y = cSprite.sprite.y + cSprite.sprite.height * 0.5f

            //flip the sprite when drawn, by using negative height
            val scaleX = cSprite.sprite.scaleX
            val scaleY = cSprite.sprite.scaleY

            val width = cSprite.sprite.width
            val height = -cSprite.sprite.height

            val originX = width * 0.5f
            val originY = height * 0.5f
            //            cSprite.sprite.setScale(Interpolation.bounce.apply(0.0f, 0.5f, scaleX));

            batch.draw(cSprite.sprite, (x * 16.0f).floor() / 16.0f,
                       (y * 16.0f).floor() / 16.0f, originX, originY, width, height, scaleX, scaleY,
                       rotation)
        }
    }

    private fun glowDroppedSprite(sprite: Sprite) {
        if (!tweenManager.containsTarget(sprite)) {

            Timeline.createSequence().push(
                    Tween.to(sprite, SpriteTween.SCALE, 2.8f).target(0.2f, 0.2f).ease(
                            Sine.IN)).push(
                    Tween.to(sprite, SpriteTween.SCALE, 2.8f).target(.5f, .5f).ease(
                            Sine.OUT)).repeatYoyo(Tween.INFINITY, 0.0f).start(tweenManager)

            val glowColor = Color.GOLDENROD
            Tween.to(sprite, SpriteTween.COLOR, 2.8f).target(glowColor.r, glowColor.g, glowColor.b, 1f).ease(
                    TweenEquations.easeInOutSine).repeatYoyo(Tween.INFINITY, 0.0f).start(tweenManager)
        }
    }

    private fun renderEntities(delta: Float) {
        //todo need to exclude blocks?
        val entities = world.entities(allOf(SpriteComponent::class))

        var cSprite: SpriteComponent

        for (i in entities.indices) {
            val entity = entities.get(i)

            val cItem = mItem.opt(entity)
            //don't draw in-inventory or dropped items
            if (cItem != null && cItem.state != ItemComponent.State.InWorldState) {
                //hack
                continue
            }

            cSprite = mSprite.get(entity)

            if (!cSprite.visible) {
                continue
            }

            //assert(cSprite.sprite != null) { "sprite is null" }
            assert(cSprite.sprite.texture != null) { "sprite has null texture" }

            var placementGhost = false

            val tag = tagManager.getTagNullable(world.getEntity(entity))
            if (tag != null && tag == "itemPlacementOverlay") {

                placementGhost = true

                if (cSprite.placementValid) {
                    batch.setColor(0f, 1f, 0f, 0.6f)
                } else {
                    batch.setColor(1f, 0f, 0f, 0.6f)
                }
            }

            val x = cSprite.sprite.rect.x
            val y = cSprite.sprite.y + cSprite.sprite.height * 0.5f

            //flip the sprite when drawn, by using negative height
            val scaleX = 1f
            val scaleY = 1f

            val width = cSprite.sprite.width
            val height = -cSprite.sprite.height

            val originX = width * 0.5f
            val originY = height * 0.5f

            //this prevents some jiggling of static items when player is moving, when the objects pos is
            // not rounded to a reasonable flat number,
            //but for the player it means they jiggle on all movement.
            //batch.draw(cSprite.sprite, MathUtils.floor(x * 16.0f) / 16.0f, MathUtils.floor(y * 16.0f) /
            // 16.0f,
            //            originX, originY, width, height, scaleX, scaleY, rotation);

            batch.draw(cSprite.sprite, x, y, originX, originY, width, height, scaleX, scaleY, rotation)

            //reset color for next run
            if (placementGhost) {
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    inner class SpriteEntityListener : OreEntitySubscriptionListener {
        override fun inserted(entities: IntBag) {
            super.inserted(entities)

            entities.toMutableList().forEach {
                mSprite.get(it).sprite.apply {
//                    color = Color.WHITE
                    //fixme for some reason seems sprites get an alpha of like 0.9967 or so...
                    //which is weird, because i'm not aware of changing that *anywhere*
                }

            }
        }
    }

    companion object {
        var spriteCount: Int = 0

        internal var rotation: Float = 0f
    }
}


