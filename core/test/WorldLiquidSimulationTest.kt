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

import com.ore.infinium.OreBlock
import com.ore.infinium.OreWorld
import com.ore.infinium.systems.server.LiquidSimulationSystem
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class WorldLiquidSimulationTest {
    internal var world = OreWorld(null, null, OreWorld.WorldInstanceType.Server)

    @Test
    @Ignore
    @Before
    fun initWorld() {
        //fill in a space
        for (x in 0 until 20) {
            for (y in 0 until 20) {
                world.setBlockType(x, y, OreBlock.BlockType.Dirt.oreValue)
            }
        }

        //hollow out the center
        for (x in 1 until 19) {
            for (y in 0 until 19) {
                world.setBlockType(x, y, OreBlock.BlockType.Air.oreValue)
            }
        }
    }

    @Test
    @Ignore
    fun simLiquid() {
        println("--------- Before water ----------")
        printLiquidLevels()
        println("--------- /Before water ----------")

        world.setLiquidLevelWaterNotEmpty(2, 2, 8)

        println("--------- Water Added (unsimulated) ----------")
        printLiquidLevels()
        println("--------- /Water Added (unsimulated) ----------")

        println("--------- Water Simulated ----------")
        //sim it a few times to settle it out
        repeat(200) {
            processLiquidRange()
        }

        //check our results, ensure it settled out properly
        printLiquidLevels()
    }

    private fun printLiquidLevels() {
        //ascii print of the world liquid levels
        for (y in 0 until 20) {
            for (x in 0 until 20) {
                var s = " | "
                if (world.isBlockLiquid(x, y)) {
                    s += world.liquidLevel(x, y).toString()
                } else {
                    val blockType = world.blockType(x, y)
                    s += when(blockType) {
                        OreBlock.BlockType.Air.oreValue -> "A"
                        OreBlock.BlockType.Dirt.oreValue -> "D"
                        else -> "?"
                    }
                }

                print(s)
            }

            println() }
    }

    fun processLiquidRange() {
        val liquidSystem = LiquidSimulationSystem(world)
        for (y in 20 downTo 0) {
            for (x in 0 until 20) {
                if (world.blockType(x, y) == OreBlock.BlockType.Water.oreValue) {
                    liquidSystem.processLiquidTile(x, y)
                }
            }
        }
    }
}
