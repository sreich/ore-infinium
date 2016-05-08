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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorldBlockTest {
    internal var world = OreWorld(null, null, OreWorld.WorldInstanceType.Server)

    @Before
    fun createWorldAndinitializeBlocksArray() {
    }

    @Test
    fun testBlockArrayInitialized() {
        assertNotNull(world.blocks)
    }

    @Test
    @Throws(Exception::class)
    fun testBlockSolid() {
        assertTrue(true)

        world.setBlockType(500, 500, OreBlock.BlockType.NullBlockType)
        assertFalse(world.isBlockSolid(500, 500))

        world.setBlockType(100, 100, OreBlock.BlockType.CopperBlockType)
        assertTrue(world.isBlockSolid(100, 100))
    }

    /*
    //tile origin is top left
    @Test
    public void testBlockAtPositionExactly() throws Exception {
        //our marker block. the rest of the ones in the world are NullBlockType
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        assertEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(10.0f, 10.0f)));
    }

    @Test
    public void testBlockAtPositionOneBlockRight() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        //ensure one block to the right is different(that we're on the right unit scale or whatever)
        assertNotEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(11.0f, 10.0f)));
    }

    @Test
    public void testBlockAtPositionOneBlockLeft() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        //ensure one block to left is different
        assertNotEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(9.0f, 10.0f)));
    }

    @Test
    public void testBlockAtPositionPartialLeft() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        assertNotEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(10.0f - (0.1f), 10.0f)));
    }

    @Test
    public void testBlockAtPositionPartialAbove() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        assertNotEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(10.0f, 10.0f - (0.1f))));
    }

    @Test
    public void testBlockAtPositionPartialBelow() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        //ensure half a block below, we are still on the same block
        assertEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(10.0f, 10.0f + (0.4f))));
    }

    @Test
    public void testBlockAtPositionPartialBottomRight() {
        world.setBlockType(10, 10, OreBlock.BlockType.CopperBlockType);

        //ensure 0.9 a block right-down, we are still on the same block
        assertEquals(world.blockType(10, 10), world.blockTypeAtPosition(new Vector2(10.0f + (0.9f), 10.0f + (0.9f))));
    }
    */

    @Test
    @Throws(Exception::class)
    fun testBlockAtSafely() {
        assertEquals(world.blockTypeSafely(600, 600), world.blockType(600, 600))
        //test that it should wrap to the bounds of the array
        assertEquals(world.blockTypeSafely(-1, 0), world.blockType(0, 0))
        assertEquals(world.blockTypeSafely(0, -1), world.blockType(0, 0))
    }

    fun createArtemisWorld() {
        /*
        OreWorld world = new OreWorld(null, null);
        world.m_artemisWorld = new World(new WorldConfigurationBuilder().dependsOn(ProfilerPlugin.class)
                                                                        .with(new NetworkClientSystem(this))
                                                                        .with(new PowerCircuitSystem(this))
                                                                        .with(new DebugTextRenderSystem(m_camera, this))
                                                                        .build());
        //inject the mappers into the world, before we start doing things
        world.m_artemisWorld.inject(this, true);
        */

    }

    @Test
    @Throws(Exception::class)
    fun testblah() {
    }
}
