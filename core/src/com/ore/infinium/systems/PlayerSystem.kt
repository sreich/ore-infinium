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

import com.artemis.Aspect
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.OreTimer
import com.ore.infinium.OreWorld
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.mapper
import com.ore.infinium.util.require
import com.ore.infinium.util.system

@Wire(failOnNull = false)
class PlayerSystem(private val oreWorld: OreWorld) : IteratingSystem(Aspect.all()) {

    private val mPlayer by require<PlayerComponent>()
    private val mSprite by mapper<SpriteComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    private val chunkTimer = OreTimer()

    override fun inserted(entityId: Int) {
        super.inserted(entityId)

        //client does nothing as of yet, with this
        if (oreWorld.isClient()) {
            return
        }

        //initial spawn, send region
        calculateLoadedViewport(entityId)
        sendPlayerBlockRegion(entityId)
    }

    override fun process(entityId: Int) {
        if (oreWorld.isClient()) {
            return
        }

        //clients, for now, do their own collision stuff. mostly.
        //FIXME: clients should simulate their own player's collision with everything and tell the server its
        // position so it can broadcast.
        // but nothing else.
        //server will simulate everything else(except players), and broadcast positions

        //should never ever, ever happen.
        assert(mSprite.has(entityId) && mPlayer.has(entityId))

        val cSprite = mSprite.get(entityId)
        val cPlayer = mPlayer.get(entityId)

        val viewportRect = cPlayer.loadedViewport.rect
        val x = cSprite.sprite.x
        val y = cSprite.sprite.y

        //fixme fixme, we'll fix this when we get to chunking and come up with a proper solution
        if (chunkTimer.resetIfSurpassed(600L)) {
            calculateLoadedViewport(entityId)
        }
    }

    private fun calculateLoadedViewport(playerEntity: Int) {
        val cPlayer = mPlayer.get(playerEntity)
        val cSprite = mSprite.get(playerEntity)

        val loadedViewport = cPlayer.loadedViewport

        val center = Vector2(cSprite.sprite.x, cSprite.sprite.y)
        loadedViewport.centerOn(center, oreWorld)

        serverNetworkSystem.sendPlayerLoadedViewportMoved(playerEntity)

        //todo send only partials depending on direction they're traveling(distance from origin).
        sendPlayerBlockRegion(playerEntity)
    }

    fun sendPlayerBlockRegion(playerEntity: Int) {
        val cPlayer = mPlayer.get(playerEntity)
        val loadedViewport = cPlayer.loadedViewport

        val region = loadedViewport.blockRegionInViewport()

        serverNetworkSystem.sendPlayerBlockRegion(playerEntity, region.x, region.width, region.y,
                                                  region.height)
    }
}
