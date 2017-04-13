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

import aurelienribon.tweenengine.TweenManager
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.ore.infinium.OreSettings
import com.ore.infinium.OreWorld
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.OreSubSystem
import ktx.assets.file

@Wire
class BackgroundRenderSystem(world: World,
                             private val oreWorld: OreWorld,
                             private val camera: Camera)
    : OreSubSystem() {

    val backgroundAtlas: TextureAtlas = TextureAtlas(file("packed/backgrounds.atlas"))

    private lateinit var batch: SpriteBatch

    private lateinit var mSprite: ComponentMapper<SpriteComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>

    private lateinit var tagManager: TagManager
    private lateinit var tweenManager: TweenManager

//    private lateinit var defaultShader: ShaderProgram
//    private lateinit var spriteLightMapBlendShader: ShaderProgram

    override fun initialize() {
        //fixme hack this should be in init i think and everything else changed from lateinit to val
        batch = SpriteBatch()
//        defaultShader = batch.shader
//
//        val tileLightMapBlendVertex = file("shaders/spriteLightMapBlend.vert").readString()
//        val tileLightMapBlendFrag = file("shaders/spriteLightMapBlend.frag").readString()
//
//        spriteLightMapBlendShader = ShaderProgram(tileLightMapBlendVertex, tileLightMapBlendFrag)
//        check(spriteLightMapBlendShader.isCompiled) { "tileLightMapBlendShader compile failed: ${spriteLightMapBlendShader.log}" }
//        spriteLightMapBlendShader.setUniformi("u_lightmap", 1)
//
//        spriteLightMapBlendShader.use {
//            spriteLightMapBlendShader.setUniformi("u_lightmap", 1)
//        }

    }

    override fun dispose() {
        batch.dispose()
    }

    override fun begin() {
//        batch.projectionMatrix = camera.combined
//        batch.color = Color.RED
    }

    override fun processSystem() {
//        batch.setProjectionMatrix(oreWorld.camera.combined);
        //tweenManager.update(world.getDelta())

        renderBackground()
        //restore color
    }

    override fun end() {
    }

    private fun renderBackground() {

        val region = oreWorld.atlas.findRegion("player-32x64")

        batch.begin()
//        Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0)
//        FrameBuffer.unbind()

        batch.draw(region, 0f, 0f, OreSettings.width.toFloat(),
                   OreSettings.height.toFloat())

        batch.end()
    }

    private fun renderEntities(delta: Float) {
        //todo need to exclude blocks?
//        val entities = world.entities(allOf(SpriteComponent::class))
//
//        var cSprite: SpriteComponent
//
//        for (i in entities.indices) {
//            val entity = entities.get(i)
//
//            val cItem = mItem.opt(entity)
//            //don't draw in-inventory or dropped items
//            if (cItem != null && cItem.state != ItemComponent.State.InWorldState) {
//                //hack
//                continue
//            }
//
//            cSprite = mSprite.get(entity)
//
//            if (!cSprite.visible) {
//                continue
//            }
//
//            //assert(cSprite.sprite != null) { "sprite is null" }
//            assert(cSprite.sprite.texture != null) { "sprite has null texture" }
//
//            var placementGhost = false
//
//            val tag = tagManager.getTagNullable(world.getEntity(entity))
//            if (tag != null && tag == "itemPlacementOverlay") {
//
//                placementGhost = true
//
//                if (cSprite.placementValid) {
//                    batch.setColor(0f, 1f, 0f, 0.6f)
//                } else {
//                    batch.setColor(1f, 0f, 0f, 0.6f)
//                }
//            }
//
//            val x = cSprite.sprite.rect.x
//            val y = cSprite.sprite.y + cSprite.sprite.height * 0.5f
//
//            //flip the sprite when drawn, by using negative height
//            val scaleX = 1f
//            val scaleY = 1f
//
//            val width = cSprite.sprite.width
//            val height = -cSprite.sprite.height
//
//            val originX = width * 0.5f
//            val originY = height * 0.5f
//
//            //this prevents some jiggling of static items when player is moving, when the objects pos is
//            // not rounded to a reasonable flat number,
//            //but for the player it means they jiggle on all movement.
//            //batch.draw(cSprite.sprite, MathUtils.floor(x * 16.0f) / 16.0f, MathUtils.floor(y * 16.0f) /
//            // 16.0f,
//            //            originX, originY, width, height, scaleX, scaleY, rotation);
//
//            batch.draw(cSprite.sprite, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
//
//            //reset color for next run
//            if (placementGhost) {
//                batch.setColor(1f, 1f, 1f, 1f)
//            }
//        }
    }
}


