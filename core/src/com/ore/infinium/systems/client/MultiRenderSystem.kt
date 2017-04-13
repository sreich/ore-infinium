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

import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.ore.infinium.OreWorld
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.oreInject
import com.ore.infinium.util.system

@Wire(failOnNull = false)
class MultiRenderSystem(private val camera: OrthographicCamera,
                        private val fullscreenCamera: Camera,
                        private val oreWorld: OreWorld)
    : BaseSystem(), RenderSystemMarker {
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tagManager by system<TagManager>()

    lateinit var spriteRenderSystem: SpriteRenderSystem
    lateinit var tileRenderSystem: TileRenderSystem
    lateinit var liquidRenderSystem: LiquidRenderSystem
    lateinit var backgroundRenderSystem: BackgroundRenderSystem

    init {
    }

    override fun initialize() {
//        backgroundRenderSystem = BackgroundRenderSystem(camera = fullscreenCamera, oreWorld = oreWorld, world = world)
        tileRenderSystem = TileRenderSystem(camera = camera, fullscreenCamera = fullscreenCamera, oreWorld = oreWorld)
//        spriteRenderSystem = SpriteRenderSystem(world = world, oreWorld = oreWorld, camera = camera,
//                                                tileLightMapFbo = tileRenderSystem.tileLightMapFbo)
//        liquidRenderSystem = LiquidRenderSystem(camera = camera, oreWorld = oreWorld, world = world,
//                                                tileRenderSystem = tileRenderSystem)

//        world.oreInject(backgroundRenderSystem)
//        world.oreInject(spriteRenderSystem)
        world.oreInject(tileRenderSystem)
//        world.oreInject(liquidRenderSystem)

//        backgroundRenderSystem.initialize()
//        spriteRenderSystem.initialize()
        tileRenderSystem.initialize()
//        liquidRenderSystem.initialize()
    }

    //fixme god awful(prefer a list), but it works for now
    override fun dispose() {
//        backgroundRenderSystem.dispose()
//        spriteRenderSystem.dispose()
        tileRenderSystem.dispose()
//        liquidRenderSystem.dispose()
    }

    override fun begin() {
//        backgroundRenderSystem.begin()
//        spriteRenderSystem.begin()
        tileRenderSystem.begin()
//        liquidRenderSystem.begin()
    }

    override fun end() {
//        backgroundRenderSystem.end()
//        spriteRenderSystem.end()
        tileRenderSystem.end()
//        liquidRenderSystem.end()
    }

    override fun processSystem() {
        if (!clientNetworkSystem.connected) {
            return
        }

        render(world.getDelta())
    }

    fun render(elapsed: Float) {
        //hack this isn't going to like this if i call ::process i don't think
        //since that system isn't actually added to the world for processing, it's
        //just our own class. maybe not derive from it?
//        backgroundRenderSystem.processSystem()
        tileRenderSystem.processSystem()
//        spriteRenderSystem.processSystem()
//        liquidRenderSystem.processSystem()
    }
}
