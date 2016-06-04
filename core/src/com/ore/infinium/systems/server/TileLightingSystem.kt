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

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.components.ItemComponent
import com.ore.infinium.components.PlayerComponent

@Wire
class TileLightingSystem(private val m_world: OreWorld) : BaseSystem() {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>

    private var initialized = false

    override fun initialize() {
    }

    companion object {
        /**
         * the max number of light levels we have for each tile.
         * we could do way more, but it'd probably cost more a lot
         * more calculation wise..which isn't a big deal, but
         * we might want to use the rest of the byte for something else.
         * haven't decided what for
         */
        const val MAX_TILE_LIGHT_LEVEL: Byte = 5
    }

    /**
     * for first-run initialization,
     * runs through the entire world tiles and finds and sets light levels
     * of each tile according to access to sunlight.
     *
     * doesn't factor in actual lights, just the global light (sunlight)
     */
    private fun computeWorldTileLighting() {
        //TODO incorporate sunlight..this is all theoretical approaches.
        //check if light is greater than sunlight and if so don't touch it..
        //sets the flag to indicate it is caused by sunlight

        //todo max y should be a reasonable base level, not far below ground
        for (y in 0..200 - 1) {
            for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
                if (!m_world.isBlockSolid(x, y) && m_world.blockWallType(x, y) == OreBlock.WallType.Air.oreValue) {
                    //including the one that we first find that is solid
//                    if (m_world.blockLightLevel(x, y) == 0.toByte()) {
                    m_world.setBlockLightLevel(x, y, MAX_TILE_LIGHT_LEVEL)
                    //                   }

                    //ambient/sunlight
                    //            diamondSunlightFloodFill(x, y, MAX_TILE_LIGHT_LEVEL)
                }
            }
        }
        for (y in 0..200 - 1) {
            for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
                if (!m_world.isBlockSolid(x, y) && m_world.blockWallType(x, y) == OreBlock.WallType.Air.oreValue) {
                    //including the one that we first find that is solid
                    val lightLevel = m_world.blockLightLevel(x, y)

                    //ambient/sunlight
                    diamondSunlightFloodFill(x - 1, y, lightLevel)
                    diamondSunlightFloodFill(x + 1, y, lightLevel)
                    diamondSunlightFloodFill(x, y + 1, lightLevel)
                    diamondSunlightFloodFill(x, y - 1, lightLevel)
//                    diamondSunlightFloodFill(x, y, lightLevel)
                    //                   diamondSunlightFloodFill(x, y, lightLevel)
                }
            }
        }
    }

    private fun diamondSunlightFloodFill(x: Int, y: Int, lastLightLevel: Byte) {
        if (y == 50) {
            val x = 2
        }
        if (m_world.blockXSafe(x) != x || m_world.blockXSafe(y) != y) {
            //out of world bounds, abort
            return
        }

        val blockType = m_world.blockType(x, y)

        val lightAttenuation = when (blockType) {
            OreBlock.BlockType.Air.oreValue -> 0
            else -> 1
        }

        //light bleed off value
        val newLightLevel = (lastLightLevel - lightAttenuation).toByte()

        val currentLightLevel = m_world.blockLightLevel(x, y)

        //don't overwrite previous light values that were greater
//        if (newLightLevel <= currentLightLevel - 1) {
        if (newLightLevel <= currentLightLevel) {
            return
        }

        m_world.setBlockLightLevel(x, y, newLightLevel)

        diamondSunlightFloodFill(x - 1, y, newLightLevel)
        diamondSunlightFloodFill(x + 1, y, newLightLevel)
        diamondSunlightFloodFill(x, y - 1, newLightLevel)
        diamondSunlightFloodFill(x, y + 1, newLightLevel)
    }

    override fun processSystem() {
        if (!initialized) {
                computeWorldTileLighting()
                initialized = true
        }
    }

}
