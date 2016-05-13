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

package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.utils.PerformanceCounter
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.components.FloraComponent
import com.ore.infinium.components.SpriteComponent
import com.sudoplay.joise.module.*
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import kotlin.concurrent.thread

@Wire
class WorldGenerator(private val m_world: OreWorld) {
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>

    fun generateWorld() {

        Gdx.app.log("server world gen", "worldgen starting")
        val counter = PerformanceCounter("test")
        counter.start()

        generateOres()
        generateGrassTiles()
        generateTrees()

        counter.stop()
        val s = "total world gen took (incl transitioning, etc): $counter.current seconds"
        Gdx.app.log("", s)
    }

    private fun generateTrees() {
        /*
        var bottomY = (pos.y + (size.y * 0.5f)).toInt()
        val leftX = (pos.x - (size.x * 0.5f)).toInt().coerceIn(0, WORLD_SIZE_X)
        val rightX = (pos.x + (size.x * 0.5f)).toInt().coerceIn(0, WORLD_SIZE_Y)
        for (y in bottomY..WORLD_SIZE_Y)
        {
            for (x in leftX..rightX) {

                if (isBlockSolid(x, y)) {
                    //can't proceed, we'll leave it where it last wasn't solid
                    return
                }

            }

            //full row was found to be lying on empty stuff,  move down
            //until we hit solid, and then abort
            pos.y = (y.toFloat() - size.y * 0.5f) + 1
        }
        */

        val rand = RandomXS128()
        //        for (int y = 0; y < WORLD_SIZE_Y; ++y) {
        //       }
        //randomRange(, 20, rand)

        /*
        we want to start at the top, at a random sample, work our way down,
        checking each block we move the potential tree down, looking for
         */

        var treePlanted = true
        var tree: Int? = null
        for (x in 0..OreWorld.WORLD_SIZE_X - 50 step 4) {

            //we reuse the previous tree, if not planted, since we have to spawn them to know how big they
            //may end up being. but we have to know the size to know where to place them,
            //or if their placement is even valid!!
            if (treePlanted) {
                //todo randomize tree sizes
                tree = m_world.createWoodenTree(FloraComponent.TreeSize.Large)
            }

            val spriteComponent = spriteMapper.get(tree!!)
            val halfTreeHeight = spriteComponent.sprite.height

            treeY@ for (y in 0..OreWorld.WORLD_SIZE_Y - 50) {
                val treeX = x.toFloat()
                val treeY = y.toFloat()

                when (m_world.isEntityFullyGrounded(entityX = treeX, entityY = treeY,
                                                    entityWidth = spriteComponent.sprite.width,
                                                    entityHeight = spriteComponent.sprite.height)) {
                    OreWorld.EntitySolidGroundStatus.FullyEmpty -> {
                    }

                    OreWorld.EntitySolidGroundStatus.PartiallyGrounded -> {
                        //fail here. abort, can't grow a tree
                        break@treeY
                    }

                    OreWorld.EntitySolidGroundStatus.FullySolid -> {
                        spriteComponent.sprite.setPosition(treeX, treeY)
                        treePlanted = true
                        //found our tree, already planted at this y value. skip
                        break@treeY
                    }
                }
            }
        }

        if (!treePlanted) {
            //last tree, couldn't find a spot for it..delete
            m_world.m_artemisWorld.delete(tree!!)
        }

    }

    /**
     * world gen, generates the initial grass of the world
     */
    private fun generateGrassTiles() {
        for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
            var y = 0
            while (y < OreWorld.WORLD_SIZE_Y) {
                val blockType = m_world.blockType(x, y)

                //fixme check biomes and their ranges
                //fill the surface/exposed dirt blocks with grass blocks
                if (blockType == OreBlock.BlockType.DirtBlockType) {
                    val topBlockType = m_world.blockTypeSafely(x, y - 1)

                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        m_world.setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock)

                        y = OreWorld.WORLD_SIZE_Y
                    }
                }
                ++y
            }
        }

        for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
            for (y in 0..OreWorld.WORLD_SIZE_Y - 1) {
                val blockType = m_world.blockType(x, y)

                if (blockType == OreBlock.BlockType.DirtBlockType && m_world.blockHasFlag(x, y,
                                                                                          OreBlock.BlockFlags.GrassBlock)) {

                    val topBlockType = m_world.blockTypeSafely(x, y - 1)
                    //OreBlock bottomBlock = blockTypeSafely(x, y + 1);
                    //OreBlock bottomLeftBlock = blockTypeSafely(x - 1, y + 1);
                    //OreBlock bottomRightBlock = blockTypeSafely(x + 1, y + 1);

                    //                    boolean leftEmpty =

                    //grows grass here
                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        m_world.setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock)
                    }
                }
            }
        }
    }

    private fun generateOres() {
        for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
            for (y in 0..OreWorld.WORLD_SIZE_Y - 1) {

                m_world.setBlockType(x, y, OreBlock.BlockType.NullBlockType)
                m_world.setBlockWallType(x, y, OreBlock.WallType.NullWallType)

                //create some sky
                if (y <= m_world.seaLevel()) {
                    continue
                }

                //              boolean underground = true;

                //hack MathUtils.random(0, 3)
                when (2) {
                    0 -> m_world.setBlockType(x, y, OreBlock.BlockType.NullBlockType)

                    1 -> m_world.setBlockType(x, y, OreBlock.BlockType.DirtBlockType)
                //fixme, simulate only dirt for now. blocks[index].type = Block.BlockType.StoneBlockType;
                    2 -> m_world.setBlockType(x, y, OreBlock.BlockType.DirtBlockType)
                }

                //                if (underground) {
                m_world.setBlockWallType(x, y, OreBlock.WallType.DirtUndergroundWallType)
                //               }

                //                blocks[dragSourceIndex].wallType = Block::Wall
            }
        }
        //        for (int x = 0; x < WORLD_SIZE_X; ++x) {
        //            for (int y = seaLevel(); y < WORLD_SIZE_Y; ++y) {
        //                Block block = blockAt(x, y);
        //                block.type = Block.BlockType.DirtBlockType;
        //            }
        //        }
    }


    enum class WorldSize(val width: Int, val height: Int) {
        Smallest(2048, 1500),
        Small(4000, 1500),
        Medium(6400, 1800),
        Large(8400, 2400)
    }

    /**
     * starbound world sizes:
     * smallest: 2048x1504
     * average: 4000x4000
     * large: 6016x4000
     * massive: 8000x4992
     *
     * terraria world sized:
     * small: 4200x1200, sky limit 360-370 above underground level
     * medium: 6400x1800, 450-600 blocks above underground(sky)
     * large: 8400x2400, sky limit: 800-900 blocks above ground
     *
     */
    companion object {
        /**
         * how many worker threads are running write now.
         * 0 indicates we are all done, each one counted
         * down their latch and returned
         */
        var workerThreadsRemainingLatch: CountDownLatch? = null

        fun generateThreaded(worldSize: WorldSize = WorldSize.Small) {
            val threadCount = 8

            workerThreadsRemainingLatch = CountDownLatch(threadCount)

            val random = Random()
//            val seed = random.nextLong()
//            val seed2 = 34247L

            var seed2 = random.nextLong()
            seed2 = 7582270557303510727
            println("seed was $seed2")

            val imageArray = FloatArray(worldSize.width * worldSize.height)

            println("server world gen, worldgen starting")

            val counter = PerformanceCounter("world gen")
            counter.start()

            for (thread in 1..threadCount) {
                thread { generate1(worldSize, thread, threadCount, seed2, imageArray) }
            }

            //halt until all threads come back up. remember, for client-hosted server,
            //user clicks button on client thread, client thread calls into server code, drops off
            // (not network) params for world gen, server eventually picks it up,
            // calls into this main generation method,
            //passes parameters the client gave it. client then wants to know the status, for loading bars etc.
            //it'll want to know how many threads are going on. it can probably just call right into the
            //server code, then into the world generator, and find that information, and set appropriate
            //ui values
            workerThreadsRemainingLatch!!.await()

            counter.stop()
            val s = "total world gen took (incl transitioning, etc): ${counter.current} seconds"
            println(s)

            writeWorldPng(imageArray, worldSize)
        }

        private fun writeWorldPng(imageArray: FloatArray, worldSize: WorldGenerator.WorldSize) {
            //           val handle = FileHandle("test/generated/worldgeneration.png")
            //          val pixmap = Pixmap(worldSize.width, worldSize.height, Pixmap.Format.RGB888)

            val bufferedImage = BufferedImage(worldSize.width, worldSize.height, BufferedImage.TYPE_INT_RGB);
            val graphics = bufferedImage.graphics;

            for (x in 0..worldSize.width - 1) {
                for (y in 0..worldSize.height - 1) {

                    val final = imageArray[x * worldSize.height + y]
                    val r = final
                    val g = r
                    val b = r
                    bufferedImage.setRGB(x, y, Color(r, g, b).rgb)
//                    pixmap.setColor(r, g, b, 1f)
                    //                   pixmap.drawPixel(x, y)
                }
            }

            //create guideline in output image to help world gen
            // tuning, for sky, to not go above/below too much
            //         pixmap.setColor(1f, 0f, 1f, 1f)


            graphics.color = Color.magenta;
            graphics.drawLine(0, 200, worldSize.width, 200)

            graphics.font = Font("SansSerif", Font.PLAIN, 12);
            graphics.drawString("y=200", 10, 190);

            ImageIO.write(bufferedImage, "png", File("test/generated/worldgeneration.png"));
        }

//public Font(@Nullable java.lang.String s,
//            @org.intellij.lang.annotations.MagicConstant(flags={java.awt.Font.PLAIN, java.awt.Font.BOLD, java.awt.Font.ITALIC}) int i,
//            int i1)

        fun generate1(worldSize: WorldSize,
                      threadNumber: Int,
                      threadCount: Int,
                      seed2: Long, imageArray: FloatArray
                     ) {

            println("...thread $threadNumber started generation")

            val counter = PerformanceCounter("world gen thread $threadNumber")
            counter.start()

            /*
         * ground_gradient
         */

            // ground_gradient
            val groundGradient = ModuleGradient()
            groundGradient.setGradient(0.0, 0.0, 0.0, 1.0)

            /*
         * lowland
         */

            // lowland_shape_fractal
            val lowlandShapeFractal = ModuleFractal(ModuleFractal.FractalType.BILLOW,
                                                    ModuleBasisFunction.BasisType.GRADIENT,
                                                    ModuleBasisFunction.InterpolationType.QUINTIC)
            lowlandShapeFractal.setNumOctaves(8)
            lowlandShapeFractal.setFrequency(0.85)
            lowlandShapeFractal.seed = seed2

            // lowland_autocorrect
            val lowlandAutoCorrect = ModuleAutoCorrect(0.0, 1.0)
            lowlandAutoCorrect.setSource(lowlandShapeFractal)
            lowlandAutoCorrect.calculate()

            // lowland_scale
            val lowlandScale = ModuleScaleOffset()
            lowlandScale.setScale(0.125)
            lowlandScale.setOffset(-0.15)
            lowlandScale.setSource(lowlandAutoCorrect)

            // lowland_y_scale
            val lowlandYScale = ModuleScaleDomain()
            lowlandYScale.setScaleY(0.0)
            lowlandYScale.setSource(lowlandScale)

            // lowland_terrain
            val lowlandTerrain = ModuleTranslateDomain()
            lowlandTerrain.setAxisYSource(lowlandYScale)
            lowlandTerrain.setSource(groundGradient)

            /*
         * highland
         */

            // highland_shape_fractal
            val highlandShapeFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                                                     ModuleBasisFunction.BasisType.GRADIENT,
                                                     ModuleBasisFunction.InterpolationType.QUINTIC)
            highlandShapeFractal.setNumOctaves(4)
            highlandShapeFractal.setFrequency(2.0)
            highlandShapeFractal.seed = seed2

            // highland_autocorrect
            val highlandAutoCorrect = ModuleAutoCorrect(-1.0, 1.0)
            highlandAutoCorrect.setSource(highlandShapeFractal)
            highlandAutoCorrect.calculate()

            // highland_scale
            val highlandScale = ModuleScaleOffset()
            highlandScale.setScale(0.25)
            highlandScale.setOffset(0.0)
            highlandScale.setSource(highlandAutoCorrect)

            // highland_y_scale
            val highlandYScale = ModuleScaleDomain()
            highlandYScale.setScaleY(0.0)
            highlandYScale.setSource(highlandScale)

            // highland_terrain
            val highlandTerrain = ModuleTranslateDomain()
            highlandTerrain.setAxisYSource(highlandYScale)
            highlandTerrain.setSource(groundGradient)

            /*
         * mountain
         */

            // mountain_shape_fractal
            val mountainShapeFractal = ModuleFractal(ModuleFractal.FractalType.RIDGEMULTI,
                                                     ModuleBasisFunction.BasisType.GRADIENT,
                                                     ModuleBasisFunction.InterpolationType.QUINTIC)
            mountainShapeFractal.setNumOctaves(8)
            mountainShapeFractal.setFrequency(1.0)
            mountainShapeFractal.seed = seed2

            // mountain_autocorrect
            val mountainAutoCorrect = ModuleAutoCorrect(-1.0, 1.0)
            mountainAutoCorrect.setSource(mountainShapeFractal)
            mountainAutoCorrect.calculate()

            // mountain_scale
            val mountainScale = ModuleScaleOffset()
            mountainScale.setScale(0.25)
            mountainScale.setOffset(0.15)
            mountainScale.setSource(mountainAutoCorrect)

            // mountain_y_scale
            val mountainYScale = ModuleScaleDomain()
            mountainYScale.setScaleY(0.1)
            mountainYScale.setSource(mountainScale)

            // mountain_terrain
            val mountainTerrain = ModuleTranslateDomain()
            mountainTerrain.setAxisYSource(mountainYScale)
            mountainTerrain.setSource(groundGradient)

            /*
         * terrain
         */

            // terrain_type_fractal
            val terrainTypeFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                                                   ModuleBasisFunction.BasisType.GRADIENT,
                                                   ModuleBasisFunction.InterpolationType.QUINTIC)
            terrainTypeFractal.setNumOctaves(3)
            terrainTypeFractal.setFrequency(0.125)
            terrainTypeFractal.seed = seed2

            // terrain_autocorrect
            val terrainAutoCorrect = ModuleAutoCorrect(0.0, 1.0)
            terrainAutoCorrect.setSource(terrainTypeFractal)
            terrainAutoCorrect.calculate()

            // terrain_type_y_scale
            val terrainTypeYScale = ModuleScaleDomain()
            terrainTypeYScale.setScaleY(0.0)
            terrainTypeYScale.setSource(terrainAutoCorrect)

            // terrain_type_cache
            val terrainTypeCache = ModuleCache()
            terrainTypeCache.setSource(terrainTypeYScale)

            // highland_mountain_select
            val highlandMountainSelect = ModuleSelect()
            highlandMountainSelect.setLowSource(highlandTerrain)
            highlandMountainSelect.setHighSource(mountainTerrain)
            highlandMountainSelect.setControlSource(terrainTypeCache)
            highlandMountainSelect.setThreshold(0.65)
            highlandMountainSelect.setFalloff(0.2)

            // highland_lowland_select
            val highlandLowlandSelect = ModuleSelect()
            highlandLowlandSelect.setLowSource(lowlandTerrain)
            highlandLowlandSelect.setHighSource(highlandMountainSelect)
            highlandLowlandSelect.setControlSource(terrainTypeCache)
            highlandLowlandSelect.setThreshold(0.25)
            highlandLowlandSelect.setFalloff(0.15)

            // highland_lowland_select_cache
            val highlandLowlandSelectCache = ModuleCache()
            highlandLowlandSelectCache.setSource(highlandLowlandSelect)

            // ground_select
            val groundSelect = ModuleSelect()
            groundSelect.setLowSource(0.0)
            groundSelect.setHighSource(1.0)
            groundSelect.setThreshold(0.10)
            groundSelect.setControlSource(highlandLowlandSelectCache)

            /*
         * cave
         */

            /*
            // cave_shape
            val caveShape = ModuleFractal(ModuleFractal.FractalType.RIDGEMULTI, ModuleBasisFunction.BasisType.GRADIENT,
                                          ModuleBasisFunction.InterpolationType.QUINTIC)
            caveShape.setNumOctaves(1)
            caveShape.setFrequency(8.0)
            caveShape.seed = seed

            // cave_attenuate_bias
            val caveAttenuateBias = ModuleBias(0.825)
            caveAttenuateBias.setSource(highlandLowlandSelectCache)

            // cave_shape_attenuate
            val caveShapeAttenuate = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            caveShapeAttenuate.setSource(0, caveShape)
            caveShapeAttenuate.setSource(1, caveAttenuateBias)

            // cave_perturb_fractal
            val cavePerturbFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                                                   ModuleBasisFunction.BasisType.GRADIENT,
                                                   ModuleBasisFunction.InterpolationType.QUINTIC)
            cavePerturbFractal.setNumOctaves(6)
            cavePerturbFractal.setFrequency(3.0)
            cavePerturbFractal.seed = seed

            // cave_perturb_scale
            val cavePerturbScale = ModuleScaleOffset()
            cavePerturbScale.setScale(0.25)
            cavePerturbScale.setOffset(0.0)
            cavePerturbScale.setSource(cavePerturbFractal)

            // cave_perturb
            val cavePerturb = ModuleTranslateDomain()
            cavePerturb.setAxisXSource(cavePerturbScale)
            cavePerturb.setSource(caveShapeAttenuate)

            // cave_select
            val caveSelect = ModuleSelect()
            caveSelect.setLowSource(1.0)
            caveSelect.setHighSource(0.0)
            caveSelect.setControlSource(cavePerturb)
            caveSelect.setThreshold(0.8)
            caveSelect.setFalloff(0.0)
            */

            /*
         * final
         */

            // ground_cave_multiply
            /*
            val groundCaveMultiply = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            groundCaveMultiply.setSource(0, caveSelect)
            groundCaveMultiply.setSource(1, groundSelect)
*/
            /*
            when (threadNumber) {
            //first half
                1 -> {
                    startX = 0
                    endX = (worldSize.width / threadCount)
                }
            //2nd half
                2 -> {
                    startX = (worldSize.width / threadCount)
                    endX = worldSize.width
                }
            }
            */

            val partitionedWidth = worldSize.width / threadCount

            val startX = (threadNumber - 1) * partitionedWidth
            val endX = startX + partitionedWidth

            val finalModule = groundSelect
            for (x in startX..endX - 1) {
                for (y in 0..worldSize.height - 1) {


                    val xRatio = worldSize.width.toDouble() / worldSize.height.toDouble()
                    val value = finalModule.get(x.toDouble() / worldSize.width.toDouble() * xRatio,
                                                y.toDouble() / worldSize.height.toDouble())

                    if (value > 0.0) {
                    }

                    val index = x * worldSize.height + y

                    imageArray[index] = value.toFloat()
                }
            }

            counter.stop()
            println("thread $threadNumber finished generation in ${counter.current} s at ${TimeUtils.millis()} ms")

            workerThreadsRemainingLatch!!.countDown()

        }
    }
}

