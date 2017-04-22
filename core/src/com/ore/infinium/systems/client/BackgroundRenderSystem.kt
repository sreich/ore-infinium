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
import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.ore.infinium.OreSettings
import com.ore.infinium.OreWorld
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.mapper
import ktx.assets.file

@Wire
class BackgroundRenderSystem(private val oreWorld: OreWorld,
                             private val camera: Camera)
    : BaseSystem(), RenderSystemMarker {

    val backgroundAtlas: TextureAtlas = TextureAtlas(file("packed/backgrounds.atlas"))

    private val batch = SpriteBatch()

    private val mSprite by mapper<SpriteComponent>()
    private val mItem by mapper<ItemComponent>()

    //    private lateinit var tagManager: TagManager
    private lateinit var tweenManager: TweenManager

    override fun initialize() {
    }

    override fun dispose() {
        batch.dispose()
    }

    override fun begin() {
    }

    override fun processSystem() {
        batch.projectionMatrix = camera.combined

        renderBackground()
    }

    override fun end() {
    }

    private fun renderBackground() {
        val skyBackground = backgroundAtlas.findRegion("background-sky")
        val debug = backgroundAtlas.findRegion("debug")

        batch.begin()
        batch.setColori(90, 142, 251, 255)
        batch.draw(debug, 0f, 0f, OreSettings.width.toFloat(),
                   OreSettings.height.toFloat())

        batch.color = Color.WHITE

        batch.draw(skyBackground, 0f, 0f, OreSettings.width.toFloat(),
                   OreSettings.height.toFloat())

        batch.end()
    }
}

private fun SpriteBatch.setColori(r: Int, g: Int, b: Int, a: Int) {
    this.setColor(r / 255f, g / 255f, b / 255f, a / 255f)
}


