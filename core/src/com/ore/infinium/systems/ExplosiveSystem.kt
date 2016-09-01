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
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.components.ToolComponent
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.allOf
import com.ore.infinium.util.mapper
import com.ore.infinium.util.require
import com.ore.infinium.util.system

@Wire(failOnNull = false)
class ExplosiveSystem(private val oreWorld: OreWorld) : IteratingSystem(allOf()) {

    private val mItem by require<ItemComponent>()
    private val mTool by require<ToolComponent>()
    private val mSprite by mapper<SpriteComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    override fun process(entityId: Int) {
        val cItem = mItem.get(entityId)
        if (cItem.state != ItemComponent.State.InWorldState) {
            return
        }

        val cTool = mTool.get(entityId)
        if (cTool.type == ToolComponent.ToolType.Explosive) {
            processExplosive(entityId, cTool)
        }
    }

    private fun processExplosive(entityId: Int, cTool: ToolComponent) {
        if (cTool.explosiveArmed == false) {
            return
        }

        if (cTool.explosiveTimer.surpassed(cTool.explosiveTime)) {
            val cSprite = mSprite.get(entityId)
            //explode!
            val halfRadius = (cTool.explosiveRadius * 0.5f).toInt()
            val left = (cSprite.sprite.x - halfRadius).toInt()
            val right = (cSprite.sprite.x + halfRadius).toInt()
            val top = (cSprite.sprite.y - halfRadius).toInt()
            val bottom = (cSprite.sprite.y + halfRadius).toInt()

            for (x in left..right) {
                for (y in top..bottom) {
                    //todo drop blocks! may wanna push that functinoality into the world
                    //as well as destroy a block range function

                    oreWorld.destroyBlock(x, y)
                }
            }

            oreWorld.serverDestroyEntity(entityId)
            serverNetworkSystem.sendBlockRegionInterestedPlayers(left = left, right = right, top = top, bottom = bottom)
        }
    }
}
