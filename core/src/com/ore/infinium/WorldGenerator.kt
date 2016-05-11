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
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.utils.PerformanceCounter
import com.ore.infinium.components.FloraComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.util.nextInt
import com.sudoplay.joise.module.*
import org.lwjgl.util.Point
import java.util.*

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


    /*
    struct OreInfiniumBlock {
        public string Name;
        public byte BlockID;
        public Color Color;
        public int DepthMax;
        public int DepthMin;
        public double DepthMinRarity;
        public double DepthMaxRarity;
        public int ExpansionTurns;
        public double HorizontalExpansionPct;
        public double VerticalExpansionPct;
        public double RarityAtDepth(int depth) {
            if (depth < DepthMin || depth > DepthMax)
                return 0;
            else
                return DepthMinRarity + ((DepthMaxRarity - DepthMinRarity) * ((double)(depth - DepthMin) / (double)(DepthMax - DepthMin)));
        }
    }
    */

    /*
    OreInfiniumBlock[] blocks;

    /// <summary>
    /// Generate a byte map
    /// </summary>
    /// <param name="width">Map width</param>
    /// <param name="height">Map height</param>
    /// <param name="surfaceLevel">Background surface level</param>
    /// <param name="seed">RNG seed</param>
    /// <returns>A 2D byte array containing the mapped BlockID's for the generated terrain</returns>
    byte[,] GenerateOreInfiniumMap(int width, int height, int surfaceLevel, int seed)
    {
        blocks = new OreInfiniumBlock[] { //items 0-2 are assumed for terrain, and not included in ore generation
        new OreInfiniumBlock() {
            Name = "Air",
            Color = Color.Transparent
        },
        new OreInfiniumBlock() {
            Name = "Stone",
            Color = Color.Gray
        },
        new OreInfiniumBlock() {
            Name = "Water",
            Color = Color.Blue
        },
        new OreInfiniumBlock() {
            Name = "Lava",
            Color = Color.Orange
        },
        new OreInfiniumBlock() {
            Name = "Ore1",
            BlockID = 4,
            Color = Color.Purple,
            DepthMin = surfaceLevel,
            DepthMax = height,
            DepthMinRarity = 0.002, //same density at all depths
            DepthMaxRarity = 0.002,
            ExpansionTurns = 300,
            VerticalExpansionPct = 0.25,
            HorizontalExpansionPct = 0.25
        },
        new OreInfiniumBlock() {
            Name = "Ore2",
            BlockID = 5,
            Color = Color.Green,
            DepthMin = 0,
            DepthMax = height,
            DepthMinRarity = 0.004, //common at surface, gets less dense as you go deeper
            DepthMaxRarity = 0.002,
            ExpansionTurns = 250,
            VerticalExpansionPct = 0.1,
            HorizontalExpansionPct = 0.2
        },
        new OreInfiniumBlock() {
            Name = "Ore3",
            BlockID = 6,
            Color = Color.Yellow,
            DepthMin = 200,
            DepthMax = height,
            DepthMinRarity = 0.001, //gets slightly more dense as you go deeper
            DepthMaxRarity = 0.002,
            ExpansionTurns = 100,
            VerticalExpansionPct = 0.2,
            HorizontalExpansionPct = 0.2
        },
        new OreInfiniumBlock() {
            Name = "Ore5",
            BlockID = 7,
            Color = Color.Red,
            DepthMin = height / 2,
            DepthMax = height,
            DepthMinRarity = 0.0, //doesn't appear for the top half of the map, gets more dense as you go down from there
            DepthMaxRarity = 0.001,
            ExpansionTurns = 10,
            VerticalExpansionPct = 0.4,
            HorizontalExpansionPct = 0.2
        },
        new OreInfiniumBlock() {
            BlockID = 7,
            DepthMin = surfaceLevel + 100,
            DepthMax = surfaceLevel + 200,
            DepthMinRarity = 0.0001,
            DepthMaxRarity = 0.0001,
            ExpansionTurns = 20,
            VerticalExpansionPct = 0.1,
            HorizontalExpansionPct = 0.4
        }
    };
        byte[,] map = new byte[width, height];
        */


    fun generate2(seed: Long = -1, worldWidth: Int = 1000, worldHeight: Int = 1000) {
        val rand = RandomXS128()


        //hack
        val surfaceLevel = 40

        var typeSpaceLeft = 0
        var typeRange = 0.0
        var lastX = rand.nextInt(surfaceLevel - 5, surfaceLevel + 5)

        //first pass, generates the surface
        for (x in 0..worldWidth) {
            if (typeSpaceLeft == 0) {
                typeSpaceLeft = rand.nextInt(50, 300);
                typeRange = rand.nextDouble() * 3.0;
            }

            lastX += ((rand.nextDouble() * typeRange * 2.0) - typeRange).toInt();

            for (y in 0..worldHeight) {
                if (y < lastX) {
                    //map[x, y] = 0;
                } else {
                    //map[x, y] = 1;
                }
            }
            typeSpaceLeft--;
        }

        //number of caves
        val mapCaveRatio = (worldWidth * worldHeight) / 1000
        var caveCount = mapCaveRatio
        //which caves will have water

        val lakePoints = mutableListOf<Point>()

        //percent chance of each generated cave having a lake
        val lakePct = 0.05

        //second pass generates rough caves

        while (caveCount > 0) {

            val cx = rand.nextInt(worldWidth)
            val cy = rand.nextInt(worldHeight)

            var maxTurns = 500

            val caveHorizPct = rand.nextDouble() * 0.8
            val caveVertPct = rand.nextDouble() * 0.5

            val depthFactor = 1.0 - (cy / worldHeight);

            var caveTurns = (maxTurns * depthFactor).toInt();

            val pts = mutableListOf<Point>()

            if (m_world.blockType(cx, cy) == 1.toByte()) {
                pts.add(Point(cx, cy));
                if (rand.nextDouble() <= lakePct) {
                    lakePoints.add(Point(cx, cy));
                }
            }

            while (caveTurns > 0 && pts.size > 0) {

                val bx = pts[0].x
                val by = pts[0].y

                m_world.setBlockType(bx, by, 0);

                if (rand.nextDouble() <= caveHorizPct && bx > 0 && m_world.blockType(bx - 1, by) == 1.toByte()) {
                    pts.add(Point(bx - 1, by));
                    m_world.setBlockType(bx - 1, by, 0);
                }

                if (rand.nextDouble() <= caveHorizPct && bx < (worldWidth - 1) && m_world.blockType(bx + 1,
                                                                                                    by) == 1.toByte()) {
                    pts.add(Point(bx + 1, by));
                    m_world.setBlockType(bx + 1, by, 0);
                }

                if (rand.nextDouble() <= caveVertPct && by > 0 && m_world.blockType(bx, by - 1) == 1.toByte()) {
                    pts.add(Point(bx, by - 1));
                    m_world.setBlockType(bx, by - 1, 0);
                }

                if (rand.nextDouble() <= caveVertPct && by < (worldHeight - 1) && m_world.blockType(bx,
                                                                                                    by + 1) == 1.toByte()) {
                    pts.add(Point(bx, by + 1));
                    m_world.setBlockType(bx, by + 1, 0);
                }

                caveTurns--;
/*
                if (caveTurns == 0 || pts.size == 1) {
                    //50% chance to start a new cave system from the first point still available
                    if (rand.nextDouble() < 0.5) {
                        pts.removerange(1, pts.size - 1);
                        maxTurns /= 2;
                        caveTurns = (maxTurns * depthFactor).toInt();
                    }
                } else {
                    pts.removeAt(0);
                }
                */
            }

            caveCount--;
        }

        val volcPoints = mutableListOf<Point>()
        val volcRoot = Point(500, 1000);
        volcPoints.add(volcRoot);

        var volcChamberTurns = 1000;
        val volcHorizPct = 0.75
        val volcVertPct = 0.5;

        //main volcano chamber
        while (volcChamberTurns > 0 && volcPoints.size > 0) {
            val bx = volcPoints[0].x
            val by = volcPoints[0].y;

            m_world.setBlockType(bx, by, 3);

            if (rand.nextDouble() <= volcHorizPct && bx > 0 && m_world.blockType(bx - 1, by) == 1.toByte()) {
                volcPoints.add(Point(bx - 1, by));
                m_world.setBlockType(bx - 1, by, 3);
            }

            if (rand.nextDouble() <= volcHorizPct && bx < (worldWidth - 1) && m_world.blockType(bx + 1,
                                                                                                by) == 1.toByte()) {
                volcPoints.add(Point(bx + 1, by));
                m_world.setBlockType(bx + 1, by, 3);
            }

            if (rand.nextDouble() <= volcVertPct && by > 0 && m_world.blockType(bx, by - 1) == 1.toByte()) {
                volcPoints.add(Point(bx, by - 1));
                m_world.setBlockType(bx, by - 1, 3);
            }

            if (rand.nextDouble() <= volcVertPct && by < (worldHeight - 1) && m_world.blockType(bx,
                                                                                                by + 1) == 1.toByte()) {
                volcPoints.add(Point(bx, by + 1));
                m_world.setBlockType(bx, by + 1, 3);
            }

            volcChamberTurns--;
            volcPoints.removeAt(0);

        }

        volcPoints.clear();
        volcPoints.add(volcRoot);

        while (volcPoints.size > 0) {

        }

        /*
        //terrain smoothing
        byte[,] tmp = new byte[width, height];
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) tmp[x, y] = map[x, y];

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++) {
            int n = 0;
            if (x > 0 && tmp[x - 1, y] == 1) n++; //left
            if (x < (width - 1) && tmp[x + 1, y] == 1) n++; //right
            if (y > 0 && tmp[x, y - 1] == 1) n++; //top
            if (y < (height - 1) && tmp[x, y + 1] == 1) n++; //bottom
            if (x > 0 && y > 0 && tmp[x - 1, y - 1] == 1) n++; //top left
            if (x < (width - 1) && y > 0 && tmp[x + 1, y] == 1) n++; //top right
            if (x > 0 && y < (height - 1) && tmp[x - 1, y + 1] == 1) n++; //bottom left
            if (x < (width - 1) && y < (height - 1) && tmp[x + 1, y + 1] == 1) n++; //bottom right

            if (tmp[x, y] == 0 && n >= 5)
                map[x, y] = 1;
            if (tmp[x, y] == 1 && n <= 2)
                map[x, y] = 0;
        }
        }
        tmp = null;

        for (int x = 0; x < width; x++)
        {
            if (map[x, surfaceLevel] == 0)
                lakePts.Add(new Point(x, surfaceLevel));
        }

        foreach (Point p in lakePts)
        {
            List<Point> pts = new List<Point>();
            pts.Add(new Point(p.X, p.Y));

            while (pts.Count > 0) {
                int lx = pts[0].X, ly = pts[0].Y;

                if (lx > 0 && map[lx - 1, ly] == 0 && !pts.Contains(new Point(lx - 1, ly)))
                pts.Add(new Point(lx - 1, ly));

                if (lx < (width - 1) && map[lx + 1, ly] == 0 && !pts.Contains(new Point(lx + 1, ly)))
                pts.Add(new Point(lx + 1, ly));

                if (ly < (height - 1) && map[lx, ly + 1] == 0 && !pts.Contains(new Point(lx, ly + 1)))
                pts.Add(new Point(lx, ly + 1));

                map[lx, ly] = 2;
                pts.RemoveAt(0);
            }


        }

        for (int y = 0; y < height; y++)
        {
            double depthFactor = (double)y / (double)height;
            for (int x = 0; x < width; x++) {
            for (int i = 4; i < blocks.Length; i++) {
            OreInfiniumBlock b = blocks[i];
            if (map[x, y] == 1) {
                if (R.NextDouble() <= b.RarityAtDepth(y)) {
                    map[x, y] = b.BlockID;
                    List<Point> pts = new List<Point>();
                    pts.Add(new Point(x, y));
                    int turns = b.ExpansionTurns;

                    //grow a vein
                    while (turns > 0 && pts.Count > 0) {
                        int bx = pts[0].X, by = pts[0].Y;

                        if (R.NextDouble() <= b.HorizontalExpansionPct && bx > 0 && map[bx - 1, by] == 1 && map[bx - 1, by] == 1) {
                            pts.Add(new Point(bx - 1, by));
                            map[bx - 1, by] = b.BlockID;
                        }

                        if (R.NextDouble() <= b.HorizontalExpansionPct && bx < (width - 1) && map[bx + 1, by] == 1 && map[bx + 1, by] == 1) {
                            pts.Add(new Point(bx + 1, by));
                            map[bx + 1, by] = b.BlockID;
                        }

                        if (R.NextDouble() <= b.VerticalExpansionPct && by > 0 && map[bx, by - 1] == 1 && map[bx, by - 1] == 1) {
                            pts.Add(new Point(bx, by - 1));
                            map[bx, by - 1] = b.BlockID;
                        }

                        if (R.NextDouble() <= b.VerticalExpansionPct && by < (height - 1) && map[bx, by + 1] == 1 && map[bx, by + 1] == 1) {
                            pts.Add(new Point(bx, by + 1));
                            map[bx, by + 1] = b.BlockID;
                        }

                        pts.RemoveAt(0);
                        turns--;
                    }

                    //if vein didn't expand at all, remove it. no single blocks
                    if (turns == b.ExpansionTurns - 1)
                        map[x, y] = 1;

                }
            }
        }
        }
        }
    }
    */
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
        fun generate1(worldSize: WorldSize = WorldSize.Small) {
            val handle = FileHandle("test/generated/worldgeneration.png")
            val pixmap = Pixmap(worldSize.width, worldSize.height, Pixmap.Format.RGB888)


            print("server world gen, worldgen starting")
            val counter = PerformanceCounter("test")
            counter.start()

            val random = Random()
//            val seed = random.nextLong()
            val seed2 = 34247L
//            print("seed was $seed")

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
            lowlandShapeFractal.setNumOctaves(2)
            lowlandShapeFractal.setFrequency(0.25)
            lowlandShapeFractal.seed = seed2

            // lowland_autocorrect
            val lowlandAutoCorrect = ModuleAutoCorrect(0.0, 1.0)
            lowlandAutoCorrect.setSource(lowlandShapeFractal)
            lowlandAutoCorrect.calculate()

            // lowland_scale
            val lowlandScale = ModuleScaleOffset()
            lowlandScale.setScale(0.125)
            lowlandScale.setOffset(-0.45)
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
            mountainScale.setScale(0.45)
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
            groundSelect.setThreshold(0.5)
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
            val finalModule = groundSelect
            for (x in 0..worldSize.width) {
                for (y in 0..worldSize.height) {

                    val xRatio = worldSize.width.toDouble() / worldSize.height.toDouble()
                    val value = finalModule.get(x.toDouble() / worldSize.width.toDouble() * xRatio,
                                                y.toDouble() / worldSize.height.toDouble())

                    //val expectedMax = worldHeight.toDouble()

                    val final = value//(value / expectedMax)
                    val r = final.toFloat()
                    val g = r
                    val b = r

                    pixmap.setColor(r, g, b, 1f)
                    pixmap.drawPixel(x, y)
                }
            }

            counter.stop()
            val s = "total world gen took (incl transitioning, etc): ${counter.current} seconds"
            print(s)

            PixmapIO.writePNG(handle, pixmap)

            /*

           int seed = 422344882;

            width = 1000; //8400
            height = 1000; //2400

            final float SCALE = 1f;
            float px, py;

            final int Open = 1;
            final int Dirt = 2;
            final int Stone = 3;
            final int SemiRare = 4;
            final int Rare = 5;
            final int Bedrock = 6;

            final int Constant1 = 1;
            final int Constant0 = 0;

            final double SEMIRARE_DENSITY = 0.6;
            final double RARE_DENSITY = 0.0009;
            final double RARE_GRADIENT_SCALE = 1.;

            /////////////////////////////////////////////////////

            ModuleGradient mainGradient = new ModuleGradient();
            mainGradient.setGradient(0, 0, 0, 0.5);

            ModuleScaleOffset mainGradientRemap = new ModuleScaleOffset();
            mainGradientRemap.setSource(mainGradient);
            mainGradientRemap.setScale(0.5);
            mainGradientRemap.setOffset(0.5);

            ModuleFractal semiRareFBM = new ModuleFractal(ModuleFractal.FractalType.FBM,
                    ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC);
            semiRareFBM.setSeed(seed);
            semiRareFBM.setNumOctaves(4);
            semiRareFBM.setFrequency(2);

            ModuleScaleOffset semiRareFBMRemap = new ModuleScaleOffset();
            semiRareFBMRemap.setSource(semiRareFBM);
            semiRareFBMRemap.setScale(0.5);
            semiRareFBMRemap.setOffset(0.5);

            ModuleSelect semiRareSelect = new ModuleSelect();
            semiRareSelect.setControlSource(semiRareFBMRemap);
            semiRareSelect.setLowSource(SemiRare);
            semiRareSelect.setHighSource(Stone);
            semiRareSelect.setThreshold(SEMIRARE_DENSITY);
            semiRareSelect.setFalloff(0);

            ///////////////////////////////////////////////////////////////////////

            ModuleFractal rareFBM = new ModuleFractal(ModuleFractal.FractalType.FBM, ModuleBasisFunction.BasisType.GRADIENT,
                    ModuleBasisFunction.InterpolationType.QUINTIC);
            rareFBM.setSeed(seed);
            rareFBM.setNumOctaves(3);
            rareFBM.setFrequency(3);

            ModuleScaleOffset rareFBMRemap = new ModuleScaleOffset();
            rareFBMRemap.setSource(rareFBM);
            rareFBMRemap.setScale(0.5);
            rareFBMRemap.setOffset(0.5);

            ModuleScaleOffset rareFBMScale = new ModuleScaleOffset();
            rareFBMRemap.setSource(rareFBMRemap);
            rareFBMRemap.setScale(RARE_GRADIENT_SCALE);
            rareFBMRemap.setOffset(0);

            ModuleCombiner rareMult = new ModuleCombiner(ModuleCombiner.CombinerType.MULT);
            rareMult.setSource(0, rareFBMScale);
            rareMult.setSource(1, mainGradientRemap);

            ModuleScaleOffset rareMultScale = new ModuleScaleOffset();
            rareFBMRemap.setSource(rareMult);
            rareFBMRemap.setScale(RARE_DENSITY);
            rareFBMRemap.setOffset(0.0);

            ModuleSelect rareSelect = new ModuleSelect();
            rareSelect.setControlSource(rareMultScale);
            rareSelect.setLowSource(semiRareSelect);
            rareSelect.setHighSource(Rare);
            rareSelect.setThreshold(0.5);
            rareSelect.setFalloff(0);

            Module finalGen = rareSelect;

             */

        }
    }
}

