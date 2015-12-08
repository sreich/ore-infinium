import com.badlogic.gdx.math.Vector2;
import com.ore.infinium.Block;
import com.ore.infinium.OreWorld;
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
public class WorldTest {

    @Test
    public void testBlockArrayInitialized() {
        OreWorld world = new OreWorld(null, null);
        world.initializeBlocksArray();
        assertNotNull(world.blockAt(200, 200));
    }

    @Test
    public void testBlockSolid() throws Exception {
        assertTrue(true);

        OreWorld world = new OreWorld(null, null);
        world.initializeBlocksArray();

        world.blockAt(500, 500).type = Block.BlockType.NullBlockType;
        assertFalse(world.isBlockSolid(500, 500));

        world.blockAt(100, 100).type = Block.BlockType.CopperBlockType;
        assertTrue(world.isBlockSolid(100, 100));
    }

    @Test
    public void testBlockAtPosition() throws Exception {
        OreWorld world = new OreWorld(null, null);
        world.initializeBlocksArray();
        assertEquals(world.blockAt(10, 10),
                     world.blockAtPosition(new Vector2(OreWorld.BLOCK_SIZE * 10.0f, OreWorld.BLOCK_SIZE * 10.0f)));

        assertEquals(world.blockAt(10, 10), world.blockAtPosition(
                new Vector2(OreWorld.BLOCK_SIZE * 10.0f + (OreWorld.BLOCK_SIZE * 0.5f),
                            OreWorld.BLOCK_SIZE * 10.0f + (OreWorld.BLOCK_SIZE * 0.5f))));
    }

    @Test
    public void testBlockAtSafely() throws Exception {
        OreWorld world = new OreWorld(null, null);
        world.initializeBlocksArray();

        assertEquals(world.blockAtSafely(600, 600), world.blockAt(600, 600));
        //test that it should wrap to the bounds of the array
        assertEquals(world.blockAtSafely(-1, 0), world.blockAt(0, 0));
        assertEquals(world.blockAtSafely(0, -1), world.blockAt(0, 0));
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
