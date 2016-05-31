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

                    if (topBlockType == OreBlock.BlockType.AirBlockType) {
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
                    if (topBlockType == OreBlock.BlockType.AirBlockType) {
                        m_world.setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock)
                    }
                }
            }
        }
    }

    private fun generateOres() {
        for (x in 0..OreWorld.WORLD_SIZE_X - 1) {
            for (y in 0..OreWorld.WORLD_SIZE_Y - 1) {

                m_world.setBlockType(x, y, OreBlock.BlockType.AirBlockType)
                m_world.setBlockWallType(x, y, OreBlock.WallType.AirWallType)

                //create some sky
                if (y <= m_world.seaLevel()) {
                    continue
                }

                //              boolean underground = true;

                //hack MathUtils.random(0, 3)
                when (2) {
                    0 -> m_world.setBlockType(x, y, OreBlock.BlockType.AirBlockType)

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


    class WorldGenOutputInfo(val worldSize: OreWorld.WorldSize, val seed: Long, val useUniqueImageName: Boolean) {
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
        Open (0),
        Sand(1),
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
                                 OreValues.Sand.oreValue to Color.ORANGE,
                                 OreValues.Stone.oreValue to Color.GRAY,
                                 OreValues.Copper.oreValue to Color2.NEON_CARROT,
                                 OreValues.Diamond.oreValue to Color2.TEAL,
                                 OreValues.Gold.oreValue to Color.YELLOW,
                                 OreValues.Coal.oreValue to Color.BLACK,
                                 OreValues.Silver.oreValue to Color2.SILVER,
                                 OreValues.Iron.oreValue to Color2.TERRA_COTTA,
                                 OreValues.Uranium.oreValue to Color2.LIME_GREEN,
                                 OreValues.Iron.oreValue to Color2.RED4,
                                 OreValues.Bedrock.oreValue to Color.CYAN,
                                 OreValues.Open.oreValue to Color.BLACK
                                )


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
    /*
    HACK no longer used since we hooked it up to physically generating the blocks world/array
    fun generateWorldAndOutputMultipleImages(worldSize: OreWorld.WorldSize = OreWorld.WorldSize.Small,
                                             threadCount: Int = 8,
                                             numberOfImages: Int) {

        for (i in 1..numberOfImages) {
            generateWorldAndOutputImage(worldSize, useUniqueImageName = true)
        }
    }
    */

    fun generateWorld2(worldSize: OreWorld.WorldSize = OreWorld.WorldSize.TestTiny) {
        val threadCount = 1

        workerThreadsRemainingLatch = CountDownLatch(threadCount)

        val random = Random()

        var seed = random.nextLong()
        /*
               */
//            seed =
        //                   -4058144727897976167
        seed = 5243159850199723543
        println("seed was $seed")

        println("server world gen, worldgen starting")

        val counter = PerformanceCounter("world gen")
        counter.start()

        for (thread in 1..threadCount) {
            thread { generateWorldThreaded(worldSize, thread, threadCount, seed) }
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

        val worldGenInfo = WorldGenOutputInfo(worldSize, seed, useUniqueImageName = false)
        writeWorldImage(worldGenInfo)
    }

//public Font(@Nullable java.lang.String s,
//            @org.intellij.lang.annotations.MagicConstant(flags={java.awt.Font.PLAIN, java.awt.Font.BOLD, java.awt.Font.ITALIC}) int i,
//            int i1)


    /**
     * threaded behind the scenes implementation of generating the world,
     * nobody outside of us should need to know that detail
     */
    private fun generateWorldThreaded(worldSize: OreWorld.WorldSize,
                                      threadNumber: Int,
                                      threadCount: Int,
                                      seed: Long) {

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
        highlandLowlandSelect.setThreshold(0.15) //.19 ?
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
        val finalOreModule = generateOres(worldSize, seed, groundCaveMultiply)

        ///////////////////////////////////////////////

        //hack: debugging
//        val genCaves = true

        var finalModule: Module = finalOreModule
        //       if (genCaves) {
        //          finalModule = groundCaveMultiply
        //     }

        outputGeneratedWorldToBlockArrayThreaded(finalModule, worldSize, threadCount, threadNumber)

        counter.stop()
        println("thread $threadNumber finished generation in ${counter.current} s at ${TimeUtils.millis()} ms")

        workerThreadsRemainingLatch!!.countDown()

    }

    /**
     * @return the final module of the ores
     */
    private fun generateOres(worldSize: OreWorld.WorldSize, seed: Long, groundCaveMultiply: ModuleCombiner): Module {

        /////////////////////////////////////////////////////
        val mainGradient = ModuleGradient()
        mainGradient.setGradient(0.0, 0.0, 0.0, 1.0)

        val copperFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                      ModuleBasisFunction.InterpolationType.QUINTIC)
        copperFBM.seed = seed
        copperFBM.setNumOctaves(4)
        copperFBM.setFrequency(450.0)

//            val copperFBMRemap = ModuleScaleOffset()
//            copperFBMRemap.setSource(copperFBM)
//            copperFBMRemap.setScale(0.5)
//            copperFBMRemap.setOffset(0.5)

        //copper or stone. higher density == more stone. fuck if i know why.
//            val COPPER_DENSITY = 0.58
        val COPPER_DENSITY = 0.2
        val copperSelect = ModuleSelect()
        copperSelect.setLowSource(OreValues.Stone.oreValue.toDouble())
        copperSelect.setHighSource(OreValues.Copper.oreValue.toDouble())
        copperSelect.setControlSource(copperFBM)
        copperSelect.setThreshold(COPPER_DENSITY)
        copperSelect.setFalloff(0.1)


        //////////////////////////////////////////////// COAL

        val coalFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                    ModuleBasisFunction.InterpolationType.QUINTIC)
        coalFBM.seed = seed + 1
        coalFBM.setNumOctaves(7)
        coalFBM.setFrequency(250.0)

        val coalSelect = ModuleSelect()
        coalSelect.setLowSource(copperSelect)
        coalSelect.setHighSource(OreValues.Coal.oreValue.toDouble())
        coalSelect.setControlSource(coalFBM)
        coalSelect.setThreshold(0.5)
        coalSelect.setFalloff(0.0)

        /////////////////////////////////////////////// IRON
        val ironFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                    ModuleBasisFunction.InterpolationType.QUINTIC)
        ironFBM.seed = seed + 2
        ironFBM.setNumOctaves(5)
        ironFBM.setFrequency(250.0)

        val ironSelect = ModuleSelect()
        ironSelect.setLowSource(coalSelect)
        ironSelect.setHighSource(OreValues.Iron.oreValue.toDouble())
        ironSelect.setControlSource(ironFBM)
        ironSelect.setThreshold(0.5)
        ironSelect.setFalloff(0.0)

        ///////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////// SILVER

        val silverFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                      ModuleBasisFunction.InterpolationType.QUINTIC)
        silverFBM.seed = seed + 3
        silverFBM.setNumOctaves(5)
        silverFBM.setFrequency(550.0)

        //limit this ore only part way down (vertically) the world, it's a slightly more rare tier
        val silverRestrictSelect = ModuleSelect()
        silverRestrictSelect.setLowSource(0.0)
        silverRestrictSelect.setHighSource(1.0)
        silverRestrictSelect.setControlSource(mainGradient)
        silverRestrictSelect.setThreshold(0.5)
        silverRestrictSelect.setFalloff(0.0)

        val silverRestrictMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
        silverRestrictMult.setSource(0, silverFBM)
        silverRestrictMult.setSource(1, silverRestrictSelect)

        val silverSelect = ModuleSelect()
        silverSelect.setLowSource(ironSelect)
        silverSelect.setHighSource(OreValues.Silver.oreValue.toDouble())
        silverSelect.setControlSource(silverRestrictMult)
        silverSelect.setThreshold(0.5)
        silverSelect.setFalloff(0.0)

        ////////////////////////////////////////////////////////////
        val goldFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                    ModuleBasisFunction.InterpolationType.QUINTIC)
        goldFBM.seed = seed + 4
        goldFBM.setNumOctaves(5)
        goldFBM.setFrequency(550.0)

        //limit this ore only part way down (vertically) the world, it's a slightly more rare tier
        val goldRestrictSelect = ModuleSelect()
        goldRestrictSelect.setLowSource(0.0)
        goldRestrictSelect.setHighSource(1.0)
        goldRestrictSelect.setControlSource(mainGradient)
        goldRestrictSelect.setThreshold(0.7)
        goldRestrictSelect.setFalloff(0.0)

        val goldRestrictMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
        goldRestrictMult.setSource(0, silverFBM)
        goldRestrictMult.setSource(1, silverRestrictSelect)

        val goldSelect = ModuleSelect()
        goldSelect.setLowSource(silverSelect)
        goldSelect.setHighSource(OreValues.Gold.oreValue.toDouble())
        goldSelect.setControlSource(goldRestrictMult)
        goldSelect.setThreshold(0.55)
        goldSelect.setFalloff(0.0)

        ////////////////////////////////////////////////////////////////////

        val uraniumFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                       ModuleBasisFunction.InterpolationType.QUINTIC)
        uraniumFBM.seed = seed + 5
        uraniumFBM.setNumOctaves(5)
        uraniumFBM.setFrequency(950.0)

        //limit this ore only part way down (vertically) the world, it's a slightly more rare tier
        val uraniumRestrictSelect = ModuleSelect()
        uraniumRestrictSelect.setLowSource(0.0)
        uraniumRestrictSelect.setHighSource(1.0)
        uraniumRestrictSelect.setControlSource(mainGradient)
        uraniumRestrictSelect.setThreshold(0.7)
        uraniumRestrictSelect.setFalloff(0.6)

        val uraniumRestrictMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
        uraniumRestrictMult.setSource(0, uraniumFBM)
        uraniumRestrictMult.setSource(1, uraniumRestrictSelect)

        val uraniumSelect = ModuleSelect()
        uraniumSelect.setLowSource(goldSelect)
        uraniumSelect.setHighSource(OreValues.Uranium.oreValue.toDouble())
        uraniumSelect.setControlSource(uraniumRestrictMult)
        uraniumSelect.setThreshold(0.5)
        uraniumSelect.setFalloff(0.0)

        ///////////////////////////////////////////////////////////////////////

        val diamondFBM = ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                                       ModuleBasisFunction.InterpolationType.QUINTIC)
        diamondFBM.seed = seed + 6
        diamondFBM.setNumOctaves(5)
        diamondFBM.setFrequency(650.0)

        //limit this ore only part way down (vertically) the world, it's a slightly more rare tier
        val diamondRestrictSelect = ModuleSelect()
        diamondRestrictSelect.setLowSource(0.0)
        diamondRestrictSelect.setHighSource(1.0)
        diamondRestrictSelect.setControlSource(mainGradient)
        diamondRestrictSelect.setThreshold(0.3)
        diamondRestrictSelect.setFalloff(0.8)

        val diamondRestrictMult = ModuleCombiner(ModuleCombiner.CombinerType.MULT)
        diamondRestrictMult.setSource(0, diamondFBM)
        diamondRestrictMult.setSource(1, diamondRestrictSelect)

        val diamondSelect = ModuleSelect()
        diamondSelect.setLowSource(uraniumSelect)
        diamondSelect.setHighSource(OreValues.Diamond.oreValue.toDouble())
        diamondSelect.setControlSource(diamondRestrictMult)
        diamondSelect.setThreshold(0.65)
        diamondSelect.setFalloff(0.0)

        ////////////////////////////// DIRT
        val dirtGradient = ModuleGradient()
        dirtGradient.setGradient(0.0, 0.0, 0.0, 1.0)

        val DIRT_THRESHOLD = 0.32

        val dirtRestrict = ModuleSelect()
        dirtRestrict.setControlSource(dirtGradient)
        dirtRestrict.setLowSource(0.2)
        dirtRestrict.setHighSource(1.0)
        dirtRestrict.setThreshold(DIRT_THRESHOLD)
        dirtRestrict.setFalloff(0.4)
        //dirtRestrict.setFalloff(0.00)

        val dirtSelect = ModuleSelect()
        dirtSelect.setControlSource(dirtRestrict)
        dirtSelect.setLowSource(OreValues.Dirt.oreValue.toDouble())
        dirtSelect.setHighSource(diamondSelect)
        dirtSelect.setThreshold(DIRT_THRESHOLD)
        dirtSelect.setFalloff(0.08)
        //dirtSelect.setFalloff(0.8)


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
        //oreCaveMultiply.setSource(1, dirtSelect)
        oreCaveMultiply.setSource(1, dirtSelect)

//            val finalGen = rareFBMRemap
//            val finalGen = rareSelect
        //var finalGen: Module = dirtSelect
        var finalGen: Module = dirtSelect
        //var finalGen: Module = coalSelect

        val showCavesAndOres = true
        if (showCavesAndOres) {
            finalGen = oreCaveMultiply
        }

        return finalGen
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
     * samples from the final module, outputs it to the world array.
     * meant to be called in a threaded fashion. (since each module.get()
     * call on each of the indices in the world, takes a lot of time due
     * to all the module chaining)
     */
    private fun outputGeneratedWorldToBlockArrayThreaded(finalModule: Module,
                                                         worldSize: OreWorld.WorldSize,
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

                //NOTE: we truncate the double to a byte. we don't care if it's 3.0 or 3.1 for an ore value,
                //but obviously we need it to be a flat number.
                //the reasoning for this happening in the first place, is due to falloff and the range of the
                //modules, i believe.

                //           assert (value == value.toInt().toDouble()) {
                //              "output to world array, but units aren't in round numbers -- invalid ore types obtained from noise generator"
                //         }

                //things like water and stuff are never generated by noise. so it'll just be e.g. air
                m_world.setBlockType(x, y, value.toByte())
            }
        }
    }

    //fixme don't use relative upward, fix game so it doesn't require working dir
    //to be set to core/assets. i think, is the proper way??? ...makes that initial
    //ide config easier too
    private val WORLD_OUTPUT_IMAGE_BASE_PATH = "../saveData/worldImages/"

    /**
     * output the entire world to a png.
     *
     * right now only blocks are handled. in the future, more stuff will be done
     */
    private fun writeWorldImage(worldGenInfo: WorldGenOutputInfo) {
//hack         val xRatio = worldGenInfo.worldSize.width.toDouble() / worldSize.height.toDouble()

        val bufferedImage = BufferedImage(worldGenInfo.worldSize.width, worldGenInfo.worldSize.height,
                                          BufferedImage.TYPE_INT_RGB);
        val graphics = bufferedImage.graphics;

        for (x in 0..worldGenInfo.worldSize.width - 1) {
            for (y in 0..worldGenInfo.worldSize.height - 1) {
                val blockType = m_world.blockType(x, y).toInt()

                //if we fail to match a color, we just output its raw value, something's strange here.
                val colorForOre = OreNoiseColorMap[blockType]!!

                val final = colorForOre
                bufferedImage.setRGB(x, y, final.rgb)
            }
        }

        graphics.color = Color.magenta;
        graphics.drawLine(0, 200, worldGenInfo.worldSize.width, 200)

        graphics.font = Font("SansSerif", Font.PLAIN, 8);
        graphics.drawString("seed: ${worldGenInfo.seed}", 20, 10)

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

        writeWorldImageLegendImprint(bufferedImage)

        ImageIO.write(bufferedImage, "png", File(fileUrl));
    }

}


