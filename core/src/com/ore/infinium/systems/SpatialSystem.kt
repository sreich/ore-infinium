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
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.ServerNetworkSystem
import com.ore.infinium.util.getNullable
import net.mostlyoriginal.api.utils.QuadTree

@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client,

 * for now this is only used by the server, so assumptions can be made based on that.
 */
class SpatialSystem(private val m_world: OreWorld) : IteratingSystem(Aspect.one(SpriteComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_serverNetworkSystem: ServerNetworkSystem

    var m_tree: QuadTree

    init {

        m_tree = QuadTree(0f, 0f, OreWorld.WORLD_SIZE_X.toFloat(), OreWorld.WORLD_SIZE_Y.toFloat())
    }

    override fun removed(entityId: Int) {
        m_tree.remove(entityId)
    }

    override fun inserted(entityId: Int) {
        val itemComponent = itemMapper.getNullable(entityId)
        if (itemComponent != null && itemComponent.state == ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return
        }

        val spriteComponent = spriteMapper.get(entityId)
        m_tree.insert(entityId, spriteComponent.sprite.x, spriteComponent.sprite.y,
                      spriteComponent.sprite.width, spriteComponent.sprite.height)
    }

    override fun process(entityId: Int) {
        val itemComponent = itemMapper.getNullable(entityId)
        if (itemComponent != null && itemComponent.state == ItemComponent.State.InInventoryState) {
            //ignore things in an inventory
            return
        }

        val spriteComponent = spriteMapper.get(entityId)
        m_tree.update(entityId, spriteComponent.sprite.x, spriteComponent.sprite.y,
                      spriteComponent.sprite.width, spriteComponent.sprite.height)
    }
}
