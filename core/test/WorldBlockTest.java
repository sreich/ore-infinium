import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.OreBlock;
import com.ore.infinium.OreWorld;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/
public class WorldBlockTest {
    OreWorld world = new OreWorld(null, null, OreWorld.WorldInstanceType.Server);

    @Before
    public void createWorldAndinitializeBlocksArray() {
        world.initializeBlocksArray();
    }

    @Test
    public void testBlockArrayInitialized() {
        assertNotNull(world.blocks);
    }

    @Test
    public void testBlockSolid() throws Exception {
        assertTrue(true);

        world.setBlockType(500, 500, OreBlock.BlockType.NullBlockType);
        assertFalse(world.isBlockSolid(500, 500));

        world.setBlockType(100, 100, OreBlock.BlockType.CopperBlockType);
        assertTrue(world.isBlockSolid(100, 100));
    }

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

    @Test
    public void testBlockAtSafely() throws Exception {
        assertEquals(world.blockTypeSafely(600, 600), world.blockType(600, 600));
        //test that it should wrap to the bounds of the array
        assertEquals(world.blockTypeSafely(-1, 0), world.blockType(0, 0));
        assertEquals(world.blockTypeSafely(0, -1), world.blockType(0, 0));
    }

    public void createArtemisWorld() {
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
    public void testblah() throws Exception {
    }
}
