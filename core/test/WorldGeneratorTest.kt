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

import com.artemis.World
import com.artemis.WorldConfigurationBuilder
import com.badlogic.gdx.utils.GdxNativesLoader
import com.ore.infinium.OreWorld
import com.ore.infinium.WorldGenerator
import com.ore.infinium.systems.server.LiquidSimulationSystem
import org.junit.Ignore
import org.junit.Test

class WorldGeneratorTest {


    @Test
    @Ignore
    @Throws(Exception::class)
    fun generateWorldAndOutputImage() {
        GdxNativesLoader.load()
        val world = OreWorld(client = null, server = null, worldInstanceType = OreWorld.WorldInstanceType.Server)
        world.artemisWorld = World(WorldConfigurationBuilder().with(LiquidSimulationSystem(world)).build())
        val worldgen = WorldGenerator(world = world)

        worldgen.generateWorld(OreWorld.WORLD_SIZE)
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun generateWorldAndOutputMultipleImages() {
        GdxNativesLoader.load()
//        WorldGenerator.generate1()
        //       WorldGenerator.generateWorldAndOutputMultipleImages(numberOfImages = 500)
    }

}
