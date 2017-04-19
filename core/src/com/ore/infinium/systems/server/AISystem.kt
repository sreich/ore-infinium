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

package com.ore.infinium.systems.server

import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.ore.infinium.OreWorld
import com.ore.infinium.components.AIComponent
import com.ore.infinium.components.ControllableComponent
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.client.ClientNetworkSystem
import com.ore.infinium.util.anyOf
import com.ore.infinium.util.mapper
import com.ore.infinium.util.require
import com.ore.infinium.util.system
import mu.KLogging

@Wire(failOnNull = false)
class AISystem(private val oreWorld: OreWorld) : IteratingSystem(anyOf(AIComponent::class)) {
    companion object : KLogging()

    private val mAi by require<AIComponent>()
    private val mSprite by mapper<SpriteComponent>()
    private val mControl by mapper<ControllableComponent>()
    private val mItem by mapper<ItemComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()
    private val tagManager by system<TagManager>()

    override fun process(entityId: Int) {
        val cControl = mControl.get(entityId)
        cControl.desiredDirection.x = -1f

        val cSprite = mSprite.get(entityId)
        logger.debug { "bunny pos ${cSprite.sprite.x},${cSprite.sprite.y}" }
    }
}
