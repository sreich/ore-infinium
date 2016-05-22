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
import com.ore.infinium.util.Color2
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
        TestTiny(1024, 1024),
        Smallest(2048, 1500),
        Small(4000, 1500),
        Medium(6400, 1800),
        Large(8400, 2400),
        Huge(8400, 8400)
    }

    class WorldGenOutputInfo(val imageArray: FloatArray, val worldSize: WorldSize, val seed: Long, val useUniqueImageName: Boolean) {
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

        /**
         * generates @param numberOfImages number of worlds to generate, and each one
         * will get output as a unique image. for batch testing of world gen
         */
        fun generateWorldAndOutputMultipleImages(worldSize: WorldSize = WorldSize.Small,
                                                 threadCount: Int = 8,
                                                 numberOfImages: Int) {

            for (i in 1..numberOfImages) {
                generateWorldAndOutputImage(worldSize, useUniqueImageName = true)
            }
        }

        fun generateWorldAndOutputImage(worldSize: WorldSize = WorldSize.TestTiny,
                                        useUniqueImageName: Boolean = false) {
            val threadCount = 1

            workerThreadsRemainingLatch = CountDownLatch(threadCount)

            val random = Random()

            var seed = random.nextLong()
            /*
                   */
            seed =
                    -4054644727897976167
            println("seed was $seed")

            val imageArray = FloatArray(worldSize.width * worldSize.height)

            println("server world gen, worldgen starting")

            val counter = PerformanceCounter("world gen")
            counter.start()

            for (thread in 1..threadCount) {
                thread { generate1(worldSize, thread, threadCount, seed, imageArray) }
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

            val worldGenInfo = WorldGenOutputInfo(imageArray, worldSize, seed, useUniqueImageName)
            writeWorldPng(worldGenInfo)
        }

        private const val WORLD_OUTPUT_IMAGE_BASE_PATH = "saveData/worldImages/"

        private fun writeWorldPng(worldGenInfo: WorldGenOutputInfo) {
            val bufferedImage = BufferedImage(worldGenInfo.worldSize.width, worldGenInfo.worldSize.height,
                    BufferedImage.TYPE_INT_RGB);
            val graphics = bufferedImage.graphics;

            for (x in 0..worldGenInfo.worldSize.width - 1) {
                for (y in 0..worldGenInfo.worldSize.height - 1) {

                    val final = worldGenInfo.imageArray[x * worldGenInfo.worldSize.height + y]
                    val r = final
                    val g = r
                    val b = r
                    bufferedImage.setRGB(x, y, Color(r, g, b).rgb)
                }
            }

            graphics.color = Color.magenta;
            graphics.drawLine(0, 200, worldGenInfo.worldSize.width, 200)

            graphics.font = Font("SansSerif", Font.PLAIN, 8);
            graphics.drawString("seed: ${worldGenInfo.seed}", 0, 10)

            graphics.drawString("y=200", 10, 190);

            var fileUrl = WORLD_OUTPUT_IMAGE_BASE_PATH
            val dir = File(fileUrl)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            if (worldGenInfo.useUniqueImageName) {
                fileUrl += "worldgeneration-${worldGenInfo.seed}.png"
            } else {
                fileUrl += "worldgeneration.png"
            }

            ImageIO.write(bufferedImage, "png", File(fileUrl));
        }

//public Font(@Nullable java.lang.String s,
//            @org.intellij.lang.annotations.MagicConstant(flags={java.awt.Font.PLAIN, java.awt.Font.BOLD, java.awt.Font.ITALIC}) int i,
//            int i1)

        fun generate1(worldSize: WorldSize,
                      threadNumber: Int,
                      threadCount: Int,
                      seed: Long, imageArray: FloatArray
        ) {

            println("...thread $threadNumber started generation")

            val counter = PerformanceCounter("world gen thread $threadNumber")
            counter.start()

            //initial ground

            val groundGradient = ModuleGradient()
            groundGradient.setGradient(0.0, 0.0, 0.0, 1.0)

            ////////////////////////// lowland

            val lowlandShapeFractal = ModuleFractal(ModuleFractal.FractalType.BILLOW,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            lowlandShapeFractal.setNumOctaves(8)
            lowlandShapeFractal.setFrequency(8.85)
            lowlandShapeFractal.seed = seed

            val lowlandAutoCorrect = ModuleAutoCorrect(0.0, 1.0)
            lowlandAutoCorrect.setSource(lowlandShapeFractal)
            lowlandAutoCorrect.calculate()

            val lowlandScale = ModuleScaleOffset()
            lowlandScale.setScale(0.155)
            lowlandScale.setOffset(-0.13)
            lowlandScale.setSource(lowlandAutoCorrect)

            val lowlandYScale = ModuleScaleDomain()
            lowlandYScale.setScaleY(0.0)
            lowlandYScale.setSource(lowlandScale)

            val lowlandTerrain = ModuleTranslateDomain()
            lowlandTerrain.setAxisYSource(lowlandYScale)
            lowlandTerrain.setSource(groundGradient)

            ////////////////////////// highland
            val highlandShapeFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            highlandShapeFractal.setNumOctaves(8)
            highlandShapeFractal.setFrequency(9.0)
            highlandShapeFractal.seed = seed

            val highlandAutoCorrect = ModuleAutoCorrect(-1.0, 1.0)
            highlandAutoCorrect.setSource(highlandShapeFractal)
            highlandAutoCorrect.calculate()

            val highlandScale = ModuleScaleOffset()
            highlandScale.setScale(0.015)
            highlandScale.setOffset(0.0)
            highlandScale.setSource(highlandAutoCorrect)

            val highlandYScale = ModuleScaleDomain()
            highlandYScale.setScaleY(0.0)
            highlandYScale.setSource(highlandScale)

            val highlandTerrain = ModuleTranslateDomain()
            highlandTerrain.setAxisYSource(highlandYScale)
            highlandTerrain.setSource(groundGradient)

            /////////////////// mountain

            val mountainShapeFractal = ModuleFractal(ModuleFractal.FractalType.RIDGEMULTI,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            mountainShapeFractal.setNumOctaves(8)
            mountainShapeFractal.setFrequency(2.0)
            mountainShapeFractal.seed = seed

            val mountainAutoCorrect = ModuleAutoCorrect(-1.0, 1.0)
            mountainAutoCorrect.setSource(mountainShapeFractal)
            mountainAutoCorrect.calculate()

            val mountainScale = ModuleScaleOffset()
            mountainScale.setScale(0.10)
            mountainScale.setOffset(0.0)
            mountainScale.setSource(mountainAutoCorrect)

            val mountainYScale = ModuleScaleDomain()
            mountainYScale.setScaleY(0.5)
            mountainYScale.setSource(mountainScale)

            val mountainTerrain = ModuleTranslateDomain()
            mountainTerrain.setAxisYSource(mountainYScale)
            mountainTerrain.setSource(groundGradient)

            //////////////// terrain

            val terrainTypeFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            terrainTypeFractal.setNumOctaves(9)
            terrainTypeFractal.setFrequency(1.825)
            terrainTypeFractal.seed = seed

            val terrainAutoCorrect = ModuleAutoCorrect(0.0, 1.0)
            terrainAutoCorrect.setSource(terrainTypeFractal)
            terrainAutoCorrect.calculate()

            val terrainTypeYScale = ModuleScaleDomain()
            terrainTypeYScale.setScaleY(0.0)
            terrainTypeYScale.setSource(terrainAutoCorrect)

            val terrainTypeCache = ModuleCache()
            terrainTypeCache.setSource(terrainTypeYScale)

            val highlandMountainSelect = ModuleSelect()
            highlandMountainSelect.setLowSource(highlandTerrain)
            highlandMountainSelect.setHighSource(mountainTerrain)
            highlandMountainSelect.setControlSource(terrainTypeCache)
            highlandMountainSelect.setThreshold(0.55)
            highlandMountainSelect.setFalloff(0.2)

            val highlandLowlandSelect = ModuleSelect()
            highlandLowlandSelect.setLowSource(lowlandTerrain)
            highlandLowlandSelect.setHighSource(highlandMountainSelect)
            highlandLowlandSelect.setControlSource(terrainTypeCache)
            highlandLowlandSelect.setThreshold(0.15)
            highlandLowlandSelect.setFalloff(0.10)


            val highlandLowlandSelectCache = ModuleCache()
            highlandLowlandSelectCache.setSource(highlandLowlandSelect)

            val groundSelect = ModuleSelect()
            groundSelect.setLowSource(0.0)
            groundSelect.setHighSource(1.0)
            groundSelect.setThreshold(0.14)
            groundSelect.setControlSource(highlandLowlandSelectCache)

            //////////////// cave


            val caveShape = ModuleFractal(ModuleFractal.FractalType.RIDGEMULTI, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            caveShape.setNumOctaves(1)
            caveShape.setFrequency(8.0)
            caveShape.seed = seed

            val caveAttenuateBias = ModuleBias(0.95)
            caveAttenuateBias.setSource(highlandLowlandSelectCache)

            val caveShapeAttenuate = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            caveShapeAttenuate.setSource(0, caveShape)
            caveShapeAttenuate.setSource(1, caveAttenuateBias)

            val cavePerturbFractal = ModuleFractal(ModuleFractal.FractalType.FBM,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            cavePerturbFractal.setNumOctaves(6)
            cavePerturbFractal.setFrequency(3.0)
            cavePerturbFractal.seed = seed

            val cavePerturbScale = ModuleScaleOffset()
            cavePerturbScale.setScale(0.75)
            cavePerturbScale.setOffset(0.0)
            cavePerturbScale.setSource(cavePerturbFractal)

            val cavePerturb = ModuleTranslateDomain()
            cavePerturb.setAxisXSource(cavePerturbScale)
            cavePerturb.setSource(caveShapeAttenuate)

            val caveSelect = ModuleSelect()
            caveSelect.setLowSource(1.0)
            caveSelect.setHighSource(0.0)
            caveSelect.setControlSource(cavePerturb)
            caveSelect.setThreshold(0.9)
            caveSelect.setFalloff(0.0)

            //final step

            val groundCaveMultiply = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            groundCaveMultiply.setSource(0, caveSelect)
            groundCaveMultiply.setSource(1, groundSelect)

            //////////////////////////////////////////////////////////////////////////
            ///////////////////////////// ORE GENERATION
            generateOres(worldSize, seed, groundCaveMultiply)

            ///////////////////////////////////////////////

            val genCaves = true

            var finalModule: Module = groundSelect
            if (genCaves) {
                finalModule = groundCaveMultiply
            }

//            outputWorldToArray(finalModule, imageArray, worldSize, threadCount, threadNumber)

            counter.stop()
            println("thread $threadNumber finished generation in ${counter.current} s at ${TimeUtils.millis()} ms")

            workerThreadsRemainingLatch!!.countDown()

        }


        /**
         * integer values of ores, only for the scope of
         * the world generation. outside of here, it's referred
         * to as way different values. this is because of our
         * noise mapping/generation.
         * The mapping of ores to colors also is only useful for
         * world generation output image.
         *
         * TODO in the future it might be useful for rendering a mini map?
         * but..we'd have to refactor this all and hopefully share more code
         * but i'd have to see if the noise functions appreciate that. i do
         * not suspect they would.
         */
        enum class OreValues(val oreValue: Int) {
            Open (1),
            Dirt(2),
            Stone (3),
            Copper (4),
            Coal (5),
            Iron (6),
            Silver (7),
            Gold (8),
            Uranium (9),
            Diamond (10),
            Bedrock (11)
        }

        /**
         * map ores to a color so we can output the image
         */
        val OreNoiseColorMap = mapOf(OreValues.Dirt.oreValue to Color2.BROWN,
                                     OreValues.Stone.oreValue to Color.GRAY,
                                     OreValues.Copper.oreValue to Color2.NEON_CARROT,
                                     OreValues.Diamond.oreValue to Color2.TEAL,
                                     OreValues.Gold.oreValue to Color.YELLOW,
                                     OreValues.Coal.oreValue to Color.BLACK,
                                     OreValues.Silver.oreValue to Color2.SILVER,
                                     OreValues.Iron.oreValue to Color2.TERRA_COTTA,
                                     OreValues.Uranium.oreValue to Color2.LIME_GREEN,
                                     OreValues.Iron.oreValue to Color2.RED4,
                                     OreValues.Bedrock.oreValue to Color.CYAN
        )

        private fun generateOres(worldSize: WorldSize, seed: Long, groundCaveMultiply: ModuleCombiner) {

            /////////////////////////////////////////////////////
            val mainGradient = ModuleGradient()
            mainGradient.setGradient(0.0, 0.0, 0.0, 0.5)

            val mainGradientRemap = ModuleScaleOffset()
            mainGradientRemap.setSource(mainGradient)
            mainGradientRemap.setScale(0.5)
            mainGradientRemap.setOffset(0.5)

            val copperFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            copperFBM.seed = seed
            copperFBM.setNumOctaves(4)
            copperFBM.setFrequency(450.0)

            val copperFBMRemap = ModuleScaleOffset()
            copperFBMRemap.setSource(copperFBM)
            copperFBMRemap.setScale(0.5)
            copperFBMRemap.setOffset(0.5)

            //copper or stone. higher density == more stone. fuck if i know why.
//            val COPPER_DENSITY = 0.58
            val COPPER_DENSITY = 0.58
            val copperSelect = ModuleSelect()
            copperSelect.setControlSource(copperFBMRemap)
            copperSelect.setLowSource(OreValues.Stone.oreValue.toDouble())
            copperSelect.setHighSource(OreValues.Copper.oreValue.toDouble())
            copperSelect.setThreshold(COPPER_DENSITY)
            copperSelect.setFalloff(0.1)

            /////////////////////////////////////////////// IRON
            val ironFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            ironFBM.seed = seed
            ironFBM.setNumOctaves(5)
            ironFBM.setFrequency(250.0)

            val ironFBMRemap = ModuleScaleOffset()
            ironFBMRemap.setSource(ironFBM)
            ironFBMRemap.setScale(0.5)
            ironFBMRemap.setOffset(0.5)

            val IRON_GRADIENT_SCALE = 1.0
            val ironFBMScale = ModuleScaleOffset()
            ironFBMScale.setSource(ironFBMRemap)
            ironFBMScale.setScale(IRON_GRADIENT_SCALE)
            ironFBMScale.setOffset(0.0)

            /*
            val ironMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            ironMult.setSource(0, ironFBMScale)
            ironMult.setSource(1, ironGradientRemap)
            */

            val IRON_DENSITY = 0.7
            val ironMultScale = ModuleScaleOffset()
//            ironMultScale.setSource(ironMult)
            ironMultScale.setSource(ironFBMScale)
            ironMultScale.setScale(IRON_DENSITY)
            ironMultScale.setOffset(0.0)

            val ironSelect = ModuleSelect()
            ironSelect.setControlSource(ironMultScale)
            ironSelect.setLowSource(copperSelect)
            ironSelect.setHighSource(OreValues.Iron.oreValue.toDouble())
            ironSelect.setThreshold(0.5)
            ironSelect.setFalloff(0.0)

            //////////////////////////////////////////////// COAL
            val coalGradient = ModuleGradient()
//            coalGradient.setGradient(0.0, 0.0, 0.0, 0.5)
            coalGradient.setGradient(0.0, 0.0, 0.5, 0.0)

            val coalGradientRemap = ModuleScaleOffset()
            coalGradientRemap.setSource(coalGradient)
            coalGradientRemap.setScale(0.5)
            coalGradientRemap.setOffset(0.5)

            val coalFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            coalFBM.seed = seed
            coalFBM.setNumOctaves(7)
            coalFBM.setFrequency(250.0)

            val coalFBMRemap = ModuleScaleOffset()
            coalFBMRemap.setSource(coalFBM)
            coalFBMRemap.setScale(0.5)
            coalFBMRemap.setOffset(0.5)

            val COAL_GRADIENT_SCALE = 1.0
            val coalFBMScale = ModuleScaleOffset()
            coalFBMScale.setSource(coalFBMRemap)
            coalFBMScale.setScale(COAL_GRADIENT_SCALE)
            coalFBMScale.setOffset(0.0)

            val coalMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            coalMult.setSource(0, coalFBMScale)
            coalMult.setSource(1, coalGradientRemap)

            val COAL_DENSITY = 0.2
            val coalMultScale = ModuleScaleOffset()
            coalMultScale.setSource(coalMult)
//            coalMultScale.setSource(coalFBMScale)
            coalMultScale.setScale(COAL_DENSITY)
            coalMultScale.setOffset(0.0)

            val coalSelect = ModuleSelect()
            coalSelect.setControlSource(coalMultScale)
            coalSelect.setLowSource(ironSelect)
            coalSelect.setHighSource(OreValues.Coal.oreValue.toDouble())
            coalSelect.setThreshold(0.5)
            coalSelect.setFalloff(0.0)

            ///////////////////////////////////////////////////////////////////////

            /////////////////////////////////////////////// TODO: IRON

            /////////////////////////////////////////////////////////

            val silverFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            silverFBM.seed = seed
            silverFBM.setNumOctaves(5)
            silverFBM.setFrequency(550.0)

            val silverFBMRemap = ModuleScaleOffset()
            silverFBMRemap.setSource(silverFBM)
            silverFBMRemap.setScale(0.5)
            silverFBMRemap.setOffset(0.5)

            val SILVER_GRADIENT_SCALE = 1.0
            val silverFBMScale = ModuleScaleOffset()
            silverFBMScale.setSource(silverFBMRemap)
            silverFBMScale.setScale(SILVER_GRADIENT_SCALE)
            silverFBMScale.setOffset(0.0)

            val silverMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            silverMult.setSource(0, silverFBMScale)
            silverMult.setSource(1, mainGradientRemap)

            val SILVER_DENSITY = 0.2
            val silverMultScale = ModuleScaleOffset()
            silverMultScale.setSource(silverMult)
            silverMultScale.setScale(SILVER_DENSITY)
            silverMultScale.setOffset(0.0)

            val silverSelect = ModuleSelect()
            silverSelect.setControlSource(silverMultScale)
            silverSelect.setLowSource(coalSelect)
            silverSelect.setHighSource(OreValues.Silver.oreValue.toDouble())
            silverSelect.setThreshold(0.5)
            silverSelect.setFalloff(0.0)

            //////////////////////////////////////////////////////////// SILVER
            val goldFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            goldFBM.seed = seed
            goldFBM.setNumOctaves(5)
            goldFBM.setFrequency(550.0)

            val goldFBMRemap = ModuleScaleOffset()
            goldFBMRemap.setSource(goldFBM)
            goldFBMRemap.setScale(0.5)
            goldFBMRemap.setOffset(0.5)

            val GOLD_GRADIENT_SCALE = 1.0
            val goldFBMScale = ModuleScaleOffset()
            goldFBMScale.setSource(goldFBMRemap)
            goldFBMScale.setScale(GOLD_GRADIENT_SCALE)
            goldFBMScale.setOffset(0.0)

            val goldMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            goldMult.setSource(0, goldFBMScale)
            goldMult.setSource(1, mainGradientRemap)

            val GOLD_DENSITY = 0.5
            val goldMultScale = ModuleScaleOffset()
            goldMultScale.setSource(goldMult)
            goldMultScale.setScale(GOLD_DENSITY)
            goldMultScale.setOffset(0.0)

            val goldSelect = ModuleSelect()
            goldSelect.setControlSource(goldMultScale)
            goldSelect.setLowSource(silverSelect)
            goldSelect.setHighSource(OreValues.Gold.oreValue.toDouble())
            goldSelect.setThreshold(0.7)
            goldSelect.setFalloff(0.0)

            ////////////////////////////////////////////////////////////////////

            val uraniumFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            uraniumFBM.seed = seed
            uraniumFBM.setNumOctaves(5)
            uraniumFBM.setFrequency(650.0)

            val uraniumFBMRemap = ModuleScaleOffset()
            uraniumFBMRemap.setSource(uraniumFBM)
            uraniumFBMRemap.setScale(0.5)
            uraniumFBMRemap.setOffset(0.5)

            val URANIUM_GRADIENT_SCALE = 1.0
            val uraniumFBMScale = ModuleScaleOffset()
            uraniumFBMScale.setSource(uraniumFBMRemap)
            uraniumFBMScale.setScale(URANIUM_GRADIENT_SCALE)
            uraniumFBMScale.setOffset(0.0)

            val uraniumMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            uraniumMult.setSource(0, uraniumFBMScale)
            uraniumMult.setSource(1, mainGradientRemap)

            val URANIUM_DENSITY = 0.4
            val uraniumMultScale = ModuleScaleOffset()
            uraniumMultScale.setSource(uraniumMult)
            uraniumMultScale.setScale(URANIUM_DENSITY)
            uraniumMultScale.setOffset(0.0)

            val uraniumSelect = ModuleSelect()
            uraniumSelect.setControlSource(uraniumMultScale)
            uraniumSelect.setLowSource(goldSelect)
            uraniumSelect.setHighSource(OreValues.Uranium.oreValue.toDouble())
            uraniumSelect.setThreshold(0.5)
            uraniumSelect.setFalloff(0.0)

            ///////////////////////////////////////////////////////////////////////

            val diamondFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC)
            diamondFBM.seed = seed
            diamondFBM.setNumOctaves(5)
            diamondFBM.setFrequency(650.0)

            val diamondFBMRemap = ModuleScaleOffset()
            diamondFBMRemap.setSource(diamondFBM)
            diamondFBMRemap.setScale(0.5)
            diamondFBMRemap.setOffset(0.5)

            val DIAMOND_GRADIENT_SCALE = 1.0
            val diamondFBMScale = ModuleScaleOffset()
            diamondFBMScale.setSource(diamondFBMRemap)
            diamondFBMScale.setScale(DIAMOND_GRADIENT_SCALE)
            diamondFBMScale.setOffset(0.0)

            val diamondMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            diamondMult.setSource(0, diamondFBMScale)
            diamondMult.setSource(1, mainGradientRemap)

            val DIAMOND_DENSITY = 0.2
            val diamondMultScale = ModuleScaleOffset()
            diamondMultScale.setSource(diamondMult)
            diamondMultScale.setScale(DIAMOND_DENSITY)
            diamondMultScale.setOffset(0.0)

            val diamondSelect = ModuleSelect()
            diamondSelect.setControlSource(diamondMultScale)
            diamondSelect.setLowSource(uraniumSelect)
            diamondSelect.setHighSource(OreValues.Diamond.oreValue.toDouble())
            diamondSelect.setThreshold(0.5)
            diamondSelect.setFalloff(0.0)

            ////////////////////////////// DIRT
            //{name="DirtStoneSelect", type="select", main_source="MainGradientRemap", low_source="Dirt", high_source="RareSelect", threshold=DIRT_THRESHOLD, falloff=0},
            //{name="GroundSelect", type="select", main_source="MainGradientRemap", low_source="Open", high_source="DirtStoneSelect", threshold=0.000001, falloff=0},
            val dirtGradient = ModuleGradient()
            dirtGradient.setGradient(0.0, 0.0, 0.0, 0.5)

            val dirtGradientRemap = ModuleScaleOffset()
            dirtGradientRemap.setSource(mainGradient)
            dirtGradientRemap.setScale(0.5)
            dirtGradientRemap.setOffset(0.5)

            val DIRT_THRESHOLD = 0.70
            val dirtStoneSelect = ModuleSelect()
            dirtStoneSelect.setControlSource(dirtGradientRemap)
            dirtStoneSelect.setLowSource(OreValues.Dirt.oreValue.toDouble())
            dirtStoneSelect.setHighSource(diamondSelect)
            dirtStoneSelect.setThreshold(DIRT_THRESHOLD)
            dirtStoneSelect.setFalloff(0.05)

            /*
            not needed
            val groundSelect = ModuleSelect()
            groundSelect.setControlSource(mainGradientRemap)
            groundSelect.setLowSource(Open.toDouble())
            groundSelect.setHighSource(dirtStoneSelect)
            groundSelect.setThreshold(0.000000)
            groundSelect.setFalloff(0.0)
            */

            //now combine with the cave/world, to cut out all the places where
            //we do not want ores to be
            val oreCaveMultiply = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
            oreCaveMultiply.setSource(0, groundCaveMultiply)
            oreCaveMultiply.setSource(1, dirtStoneSelect)

//            val finalGen = rareFBMRemap
//            val finalGen = rareSelect
            var finalGen: Module = copperSelect

            val showCavesAndOres = false
            if (showCavesAndOres) {
                finalGen = oreCaveMultiply
            }

            var color: Color

            val bufferedImage = BufferedImage(worldSize.width, worldSize.height, BufferedImage.TYPE_INT_RGB);

            val xRatio = worldSize.width.toDouble() / worldSize.height.toDouble()

            for (x in 0..worldSize.width - 1) {
                for (y in 0..worldSize.height - 1) {
                    val result = finalGen.get(x.toDouble() / worldSize.width.toDouble() * xRatio,
                            y.toDouble() / worldSize.height.toDouble())

                    //print(",result $result")

                    //if we fail to match a color, we just output its raw value, something's strange here.
                    val colorForOre = OreNoiseColorMap.getOrElse(result.toInt(),
                            {
                                //should be < 1.0, if it's not we have
                                //no mapping here/bad ore setup
                                Color(result.toFloat(), result.toFloat(),
                                        result.toFloat())
                            })
                    bufferedImage.setRGB(x, y, colorForOre.rgb)
                }
            }

            writeWorldImageLegendImprint(bufferedImage)

            var fileUrl = WORLD_OUTPUT_IMAGE_BASE_PATH

            val dir = File(fileUrl)
            if (!dir.exists()) {
                dir.mkdirs()
            }


            fileUrl += "worldgeneration-ores.png"

            ImageIO.write(bufferedImage, "png", File(fileUrl));

            //FIXME: at the end of all of this, slap bedrock on the bottom (and sides) with just a simple loop, so they can't dig beyond the edges of the world
        }

        /**
         * renders a handy legend on the output world image
         */
        private fun writeWorldImageLegendImprint(bufferedImage: BufferedImage) {
            val graphics = bufferedImage.graphics
            graphics.font = Font("SansSerif", Font.PLAIN, 9);

            val leftX = 5
            val startY = 8
            var index = 0
            for ((oreValue, oreColor) in OreNoiseColorMap) {
                val y = startY + index * 8

                val oreLegendRectSize = 2

                graphics.color = oreColor
                graphics.fillRect(leftX, y, oreLegendRectSize, oreLegendRectSize)

                graphics.color = Color.MAGENTA

                val oreName = OreValues.values().first { it -> it.oreValue == oreValue }.name
                graphics.drawString(oreName, leftX + oreLegendRectSize * 2, y + 3)

                ++index
            }
        }

        /**
         * samples from the final module, outputs it to the world array
         */
        private fun outputWorldToArray(finalModule: Module,
                                       imageArray: FloatArray,
                                       worldSize: WorldGenerator.WorldSize,
                                       threadCount: Int,
                                       threadNumber: Int) {
            val partitionedWidth = worldSize.width / threadCount

            val startX = (threadNumber - 1) * partitionedWidth
            val endX = startX + partitionedWidth

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
        }

    }
}


