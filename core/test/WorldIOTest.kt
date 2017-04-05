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

import com.ore.infinium.OreWorld
import org.junit.Ignore
import org.junit.Test

class WorldIOTest {
    internal var world = OreWorld(null, null, OreWorld.WorldInstanceType.Server, OreWorld.WorldSize.TestTiny)

    @Test
    @Ignore
    @Throws(Exception::class)
    fun saveWorld() {
//        val worldSize = OreWorld.WorldSize.TestTiny
//        //setup
//        for (x in 0 until worldSize.width) {
//            for (y in 0 until worldSize.height) {
//                world.setBlockType(x, y, OreBlock.BlockType.Diamond.oreValue)
//            }
//        }
//
//        world.worldIO.saveWorld()
//
//        //verify read back
//        for (x in 0 until worldSize.width) {
//            for (y in 0 until worldSize.height) {
//                assertEquals(OreBlock.BlockType.Diamond.oreValue, world.blockType(x, y),
//                             "blocks array read back failed.")
//            }
//        }
    }

    //WorldGenerator.generateWorldAndOutputImage()
}
