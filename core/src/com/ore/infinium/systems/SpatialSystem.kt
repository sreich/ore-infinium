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
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.ServerNetworkSystem
import net.mostlyoriginal.api.utils.QuadTree
import com.ore.infinium.util.require
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system
import com.ore.infinium.util.ifPresent

@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client,

 * for now this is only used by the server, so assumptions can be made based on that.
 */
class SpatialSystem(private val oreWorld: OreWorld) : IteratingSystem(Aspect.all()) {

    private val mSprite by require<SpriteComponent>()
    // private val mPlayer by mapper<PlayerComponent>()
    // private val mControl by mapper<ControllableComponent>()
    private val mItem by mapper<ItemComponent>()
    // private val mVelocity by mapper<VelocityComponent>()
    // private val mJump by mapper<JumpComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()

    var quadTree: QuadTree

    init {

        quadTree = QuadTree(0f, 0f, OreWorld.WORLD_SIZE_X.toFloat(), OreWorld.WORLD_SIZE_Y.toFloat())
    }

    override fun removed(entityId: Int) {
        quadTree.remove(entityId)
    }

    override fun inserted(entityId: Int) {

        // ignore things in an inventory
        mItem.ifPresent(entityId) {
            if (it.state == ItemComponent.State.InInventoryState)
                return@inserted
        }

        val cSprite = mSprite.get(entityId)
        quadTree.insert(entityId, cSprite.sprite.x, cSprite.sprite.y,
                      cSprite.sprite.width, cSprite.sprite.height)
    }

    override fun process(entityId: Int) {

        // ignore things in an inventory
        mItem.ifPresent(entityId) {
            if (it.state == ItemComponent.State.InInventoryState)
                return@process
        }

        val cSprite = mSprite.get(entityId)
        quadTree.update(entityId, cSprite.sprite.x, cSprite.sprite.y,
                      cSprite.sprite.width, cSprite.sprite.height)
    }
}
