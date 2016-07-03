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

package com.ore.infinium.systems

import com.artemis.annotations.Wire
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.kartemis.KIteratingSystem
import com.ore.infinium.systems.server.ServerNetworkSystem

@Wire(failOnNull = false)
class PlayerSystem(private val oreWorld: OreWorld) : KIteratingSystem() {

    private val mPlayer = require<PlayerComponent>()
    private val mSprite = mapper<SpriteComponent>()
    private val mControl = mapper<ControllableComponent>()
    private val mItem = mapper<ItemComponent>()
    private val mVelocity = mapper<VelocityComponent>()
    private val mJump = mapper<JumpComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    private val chunkTimer = OreTimer()

    override fun inserted(entityId: Int) {
        super.inserted(entityId)

        //client does nothing as of yet, with this
        if (oreWorld.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            return
        }

        //initial spawn, send region
        calculateLoadedViewport(entityId)
        sendPlayerBlockRegion(entityId)
    }

    override fun removed(entityId: Int) {
        super.removed(entityId)

    }

    override fun process(entityId: Int) {
        if (oreWorld.worldInstanceType != OreWorld.WorldInstanceType.Server) {
            return
        }

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions

        //should never ever, ever happen.
        assert(mSprite.has(entityId) && mPlayer.has(entityId))

        val spriteComponent = mSprite.get(entityId)
        val playerComponent = mPlayer.get(entityId)

        val viewportRect = playerComponent.loadedViewport.rect
        val x = spriteComponent.sprite.x
        val y = spriteComponent.sprite.y

        //fixme fixme, we'll fix this when we get to chunking and come up with a proper solution
        if (chunkTimer.milliseconds() > 600) {
            calculateLoadedViewport(entityId)
            chunkTimer.reset()
        }
    }

    private fun calculateLoadedViewport(playerEntity: Int) {
        val playerComponent = mPlayer.get(playerEntity)
        val spriteComponent = mSprite.get(playerEntity)

        val loadedViewport = playerComponent.loadedViewport

        val center = Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y)
        loadedViewport.centerOn(center)

        serverNetworkSystem.sendPlayerLoadedViewportMoved(playerEntity)

        //todo send only partials depending on direction they're traveling(distance from origin).
        sendPlayerBlockRegion(playerEntity)
    }

    private fun sendPlayerBlockRegion(playerEntity: Int) {
        val playerComponent = mPlayer.get(playerEntity)
        val loadedViewport = playerComponent.loadedViewport

        val region = loadedViewport.blockRegionInViewport()

        serverNetworkSystem.sendPlayerBlockRegion(playerEntity, region.x, region.y, region.width,
                                                    region.height)
    }

}
