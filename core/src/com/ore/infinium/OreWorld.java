package com.ore.infinium;

import com.artemis.*;
import com.artemis.managers.PlayerManager;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;
import com.artemis.utils.reflect.ClassReflection;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.*;
import com.ore.infinium.systems.profiler.ProfilerSystem;

import java.util.HashMap;

/**
 * ***************************************************************************
 * Copyright (C) 2014, 2015 by Shaun Reich <sreich02@gmail.com>              *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
public class OreWorld {
    public static final float GRAVITY_ACCEL = 0.5f;
    public static final float GRAVITY_ACCEL_CLAMP = 0.5f;

    public static final float BLOCK_SIZE_PIXELS = 16.0f;

    public static final int WORLD_SIZE_X = 2400; //2400
    public static final int WORLD_SIZE_Y = 8400; //8400
    public static final int WORLD_SEA_LEVEL = 50;

    /**
     * indicates an invalid entity id
     */
    public static final int ENTITY_INVALID = -1;

    /**
     * looks up the texture prefix name for each block type. e.g. DirtBlockType -> "dirt", etc.
     */
    public static final HashMap<Byte, BlockAttributes> blockAttributes = new HashMap<>();

    static {
        blockAttributes.put(OreBlock.BlockType.NullBlockType,
                            new BlockAttributes("", BlockAttributes.Collision.False, BlockAttributes.BlockCategory.Null,
                                                (short) 0));
        blockAttributes.put(OreBlock.BlockType.DirtBlockType,
                            new BlockAttributes("dirt", BlockAttributes.Collision.True,
                                                BlockAttributes.BlockCategory.Dirt, (short) 200));
        blockAttributes.put(OreBlock.BlockType.StoneBlockType,
                            new BlockAttributes("stone", BlockAttributes.Collision.True,
                                                BlockAttributes.BlockCategory.Ore, (short) 300));
    }

    //each unit is 1 block(16x16 px), in the game world
    //public OreBlock[] blocks;
    public byte[] blocks;

    //fixme players really should be always handled by the system..and i suspect a lot of logic can be handled by
    // them alone.
    public OreServer m_server;
    public AssetManager assetManager;
    public OreClient m_client;
    public OrthographicCamera m_camera;

    //fixme remove in favor of the render system
    public TextureAtlas m_atlas;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<AirGeneratorComponent> airGeneratorMapper;
    private ComponentMapper<ToolComponent> toolMapper;
    private ComponentMapper<AirComponent> airMapper;
    private ComponentMapper<HealthComponent> healthMapper;
    private ComponentMapper<LightComponent> lightMapper;
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper;
    private ComponentMapper<PowerConsumerComponent> powerConsumerMapper;
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper;

    private boolean m_noClipEnabled;

    public static final String s_itemPlacementOverlay = "itemPlacementOverlay";
    public static final String s_crosshair = "crosshair";
    public static final String s_mainPlayer = "mainPlayer";

    public World m_artemisWorld;

    /**
     * who owns/is running this exact world instance. If it is the server, or a client.
     * Note that if the connection type is only a client, obviously a server
     * world type will never exist
     */
    public enum WorldInstanceType {
        Client,
        Server,
        ClientHostingServer
    }

    public WorldInstanceType worldInstanceType;

    /**
     * The main world, shared between both client and server, core to a lot of basic
     * shared functionality, as well as stuff that doesn't really belong elsewhere,
     * creates the artemis world and handles processing, as well as shutting down
     * (when told to do so)
     *
     * @param client
     *         never null..
     * @param server
     *         null if it is only a client, if both client and server are valid, the
     *         this is a local hosted server, (aka singleplayer, or self-hosting)
     */
    public OreWorld(OreClient client, OreServer server, WorldInstanceType worldInstanceType) {

        this.worldInstanceType = worldInstanceType;
        m_client = client;
        m_server = server;

        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        //        blocks = new OreBlock[WORLD_SIZE_Y * WORLD_SIZE_X];
        blocks = new byte[WORLD_SIZE_Y * WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT];
    }

    void init() {
        assert isHotspotOptimizationEnabled() : "error, hotspot optimization (artemis-odb weaving) is not enabled";

        if (worldInstanceType == WorldInstanceType.Client ||
            worldInstanceType == WorldInstanceType.ClientHostingServer) {
            float width = OreSettings.getInstance().width / BLOCK_SIZE_PIXELS;
            float height = OreSettings.getInstance().height / BLOCK_SIZE_PIXELS;
            m_camera = new OrthographicCamera(width, height);//30, 30 * (h / w));
            m_camera.setToOrtho(true, width, height);

            m_atlas = new TextureAtlas(Gdx.files.internal("packed/entities.atlas"));

            //we don't generate the block world with noise, just init it so it isn't
            //it isn't null
            initializeBlocksArray();

            //note although it may look like it.. order for render/logic ones..actually doesn't matter, their base
            // class dictates this.
            m_artemisWorld = new World(
                    new WorldConfigurationBuilder().register(new GameLoopSystemInvocationStrategy(25, false))
                                                   .with(new TagManager())
                                                   .with(new PlayerManager())
                                                   .with(new MovementSystem(this))
                                                   .with(new NetworkClientSystem(this))
                                                   .with(new ServerPowerCircuitSystem(this))
                                                   .with(new InputSystem(m_camera, this))
                                                   .with(new EntityOverlaySystem(this))
                                                   .with(new PlayerSystem(this))
                                                   .with(new GameTickSystem(this))
                                                   .with(new ClientBlockDiggingSystem(this, m_client))
                                                   .with(new TileRenderSystem(m_camera, this))
                                                   .with(new SpriteRenderSystem(this))
                                                   .with(new DebugTextRenderSystem(m_camera, this))
                                                   .with(new PowerOverlayRenderSystem(this, m_client.m_stage,
                                                                                      m_client.m_skin))
                                                   .with(new TileTransitionSystem(m_camera, this))
                                                   .with(new ProfilerSystem(m_client.m_skin))
                                                   .build());
            //b.dependsOn(WorldConfigurationBuilder.Priority.LOWEST + 1000,ProfilerSystem.class);

            //inject the mappers into the world, before we start doing things
            m_artemisWorld.inject(this, true);

            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
        } else if (worldInstanceType == WorldInstanceType.Server) {
            m_artemisWorld = new World(new WorldConfigurationBuilder().with(new TagManager())
                                                                      .with(new PlayerManager())
                                                                      .with(new MovementSystem(this))
                                                                      .with(new ServerPowerCircuitSystem(this))
                                                                      .with(new GameTickSystem(this))
                                                                      .with(new GrassBlockSystem(this))
                                                                      .with(new ServerBlockDiggingSystem(this))
                                                                      .with(new PlayerSystem(this))
                                                                      .with(new NetworkServerSystem(this, m_server))
                                                                      .register(new GameLoopSystemInvocationStrategy(25,
                                                                                                                     true))
                                                                      .build());
            //inject the mappers into the world, before we start doing things
            m_artemisWorld.inject(this, true);

            generateWorld();
        }

        //        assetManager = new AssetManager();
        //        TextureAtlas m_blockAtlas = assetManager.get("data/", TextureAtlas.class);
        //        assetManager.finishLoading();

        //        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);
    }

    private boolean isHotspotOptimizationEnabled() {
        // hotspot optimization replaces (amongst other steps) references to entityprocessingsystem with entitysystem.
        // so we can determine this optimization by EntityProcessingSystem missing from our system's hierarchy.
        //this is for artemis-odb optimization, ing annotations. it helps inline some calls
        return !ClassReflection.isAssignableFrom(EntityProcessingSystem.class, NetworkClientSystem.class);
    }

    public void initServer() {
    }

    /**
     * @param playerName
     * @param connectionId
     *
     * @return
     */
    public int createPlayer(String playerName, int connectionId) {
        int playerEntity = m_artemisWorld.create();
        SpriteComponent playerSprite = spriteMapper.create(playerEntity);
        velocityMapper.create(playerEntity);

        PlayerComponent playerComponent = playerMapper.create(playerEntity);
        playerComponent.connectionPlayerId = connectionId;
        //fixme fixme, should be consolidated w/ sprite's noclip...or should it?? make mention, is sprite present on
        // the server?? at least the component, maybe not inner sprite
        playerComponent.noClip = m_noClipEnabled;

        playerComponent.playerName = playerName;
        playerComponent.loadedViewport.setRect(
                new Rectangle(0, 0, LoadedViewport.MAX_VIEWPORT_WIDTH, LoadedViewport.MAX_VIEWPORT_HEIGHT));
        playerComponent.loadedViewport.centerOn(new Vector2(playerSprite.sprite.getX(), playerSprite.sprite.getY()));

        playerSprite.sprite.setSize(2, 3);
        controlMapper.create(playerEntity);

        playerSprite.textureName = "player1Standing1";
        playerSprite.category = SpriteComponent.EntityCategory.Character;
        jumpMapper.create(playerEntity);

        HealthComponent healthComponent = healthMapper.create(playerEntity);
        healthComponent.health = healthComponent.maxHealth;

        AirComponent airComponent = airMapper.create(playerEntity);
        airComponent.air = airComponent.maxAir;

        return playerEntity;
    }

    private void generateWorld() {
        PerformanceCounter counter = new PerformanceCounter("test");
        counter.start();

        generateOres();
        generateGrassTiles();

        counter.stop();
        String s = String.format("total world gen took (incl transitioning, etc): %s seconds", counter.current);
        Gdx.app.log("", s);

    }

    /**
     * world gen, generates the initial grass of the world
     */
    private void generateGrassTiles() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {
                final byte blockType = blockType(x, y);

                //fixme check biomes and their ranges
                //fill the surface/exposed dirt blocks with grass blocks
                if (blockType == OreBlock.BlockType.DirtBlockType) {
                    final byte topBlockType = blockTypeSafely(x, y - 1);

                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock);

                        y = WORLD_SIZE_Y;
                    }
                }
            }
        }

        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {
                byte blockType = blockType(x, y);

                if (blockType == OreBlock.BlockType.DirtBlockType &&
                    blockHasFlag(x, y, OreBlock.BlockFlags.GrassBlock)) {

                    byte topBlockType = blockTypeSafely(x, y - 1);
                    //OreBlock bottomBlock = blockTypeSafely(x, y + 1);
                    //OreBlock bottomLeftBlock = blockTypeSafely(x - 1, y + 1);
                    //OreBlock bottomRightBlock = blockTypeSafely(x + 1, y + 1);

                    //                    boolean leftEmpty =

                    //grows grass here
                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock);
                    }
                }
            }
        }
    }

    public void initializeBlocksArray() {
        //NEEDED?? hack
            /*
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                int index = x * WORLD_SIZE_Y + y;
                blocks[index] = new OreBlock();
                blocks[index].type = OreBlock.BlockType.NullBlockType;
            }
        }
        */
    }

    private void generateOres() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                setBlockType(x, y, OreBlock.BlockType.NullBlockType);
                setBlockWallType(x, y, OreBlock.WallType.NullWallType);

                //create some sky
                if (y <= seaLevel()) {
                    continue;
                }

                //              boolean underground = true;

                switch (MathUtils.random(0, 3)) {
                    case 0:
                        setBlockType(x, y, OreBlock.BlockType.NullBlockType);
                        break;

                    case 1:
                        setBlockType(x, y, OreBlock.BlockType.DirtBlockType);
                        break;
                    case 2:
                        //fixme, simulate only dirt for now. blocks[index].type = Block.BlockType.StoneBlockType;
                        setBlockType(x, y, OreBlock.BlockType.DirtBlockType);
                        break;
                }

                //                if (underground) {
                setBlockWallType(x, y, OreBlock.WallType.DirtUndergroundWallType);
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

    /**
     * safely return a block at x, y, clamped at world bounds
     *
     * @param x
     * @param y
     *
     * @return
     */
    public byte blockTypeSafely(int x, int y) {
        x = MathUtils.clamp(x, 0, WORLD_SIZE_X - 1);
        y = MathUtils.clamp(y, 0, WORLD_SIZE_Y - 1);
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE];
    }

    /**
     * take a possibly-unsafe x block index,
     * and return a safe (clamped) one
     *
     * @param x
     *
     * @return
     */
    public int blockXSafe(int x) {
        return MathUtils.clamp(x, 0, (WORLD_SIZE_X) - 1);
    }

    /**
     * take a possibly-unsafe y block index,
     * and return a safe (clamped) one
     *
     * @param y
     *
     * @return
     */
    public int blockYSafe(int y) {
        return MathUtils.clamp(y, 0, (WORLD_SIZE_Y) - 1);
    }

    //blocks[(x * 2400 + y) * 4 + i] where i = 0, 1, 2 or 3
    public byte blockType(int x, int y) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE :
                String.format("blockType index out of range. x: %d, y: %d", x, y);

        //todo can change it to bitshift if we want to...the jvm should already know to do this though..but idk if it
        // will do it
        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE];
    }

    public byte blockWallType(int x, int y) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE :
                String.format("blockWallType index out of range. x: %d, y: %d", x, y);

        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE];
    }

    public byte blockMeshType(int x, int y) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE :
                String.format("blockMeshType index out of range. x: %d, y: %d", x, y);
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE];
    }

    public byte blockFlags(int x, int y) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS :
                String.format("blockFlags index out of range. x: %d, y: %d", x, y);
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS];
    }

    public boolean blockHasFlag(int x, int y, byte flag) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS :
                String.format("blockHasFlag index out of range. x: %d, y: %d", x, y);
        return (blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] &
                flag) != 0;
    }

    public void setBlockType(int x, int y, byte type) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE :
                String.format("setBlockType index out of range. x: %d, y: %d", x, y);

        //todo can change it to bitshift if we want to...the jvm should already know to do this though..but idk
        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE] = type;
    }

    public void setBlockWallType(int x, int y, byte wallType) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE :
                String.format("setBlockWallType index out of range. x: %d, y: %d", x, y);

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE] = wallType;
    }

    public void setBlockMeshType(int x, int y, byte meshType) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE :
                String.format("setBlockMeshType index out of range. x: %d, y: %d", x, y);
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE] = meshType;
    }

    /**
     * overwrites the blocks current flags, to now be the provided flags
     *
     * @param x
     * @param y
     * @param flags
     */
    public void setBlockFlags(int x, int y, byte flags) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS :
                String.format("setBlockFlags index out of range. x: %d, y: %d", x, y);
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] = flags;
    }

    /**
     * disable a block flag
     *
     * @param x
     * @param y
     * @param flagToEnable
     */
    public void unsetBlockFlag(int x, int y, byte flagToEnable) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS :
                String.format("enableBlockFlags index out of range. x: %d, y: %d", x, y);

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] &= ~flagToEnable;
    }

    /**
     * enable a block flag
     *
     * @param x
     * @param y
     * @param flagToEnable
     */
    public void setBlockFlag(int x, int y, byte flagToEnable) {
        assert x >= 0 && y >= 0 &&
               x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
               y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS :
                String.format("enableBlockFlags index out of range. x: %d, y: %d", x, y);

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] |= flagToEnable;
    }

    /**
     * properly destroys the block in the array (sets meshtype, flags etc to defaults)
     * must be called when destroying a block, so it looks like it was dug.
     */
    public void destroyBlock(int x, int y) {
        setBlockType(x, y, OreBlock.BlockType.NullBlockType);
        setBlockMeshType(x, y, (byte) 0);
        //wall type doesn't get nulled out. i think that's what we want
        setBlockFlags(x, y, (byte) 0);
    }

    public boolean isBlockSolid(int x, int y) {
        boolean solid = true;

        byte type = blockType(x, y);

        if (type == OreBlock.BlockType.NullBlockType) {
            solid = false;
        }

        return solid;
    }

    /**
     * Safely shutdown the world, disposing of all the systems
     * Each system should be designed such that it can safely shut itself down without
     * having to interface with other systems. Though some exceptions may apply
     */
    public void shutdown() {
        m_artemisWorld.dispose();
    }

    /**
     * main world processing,
     * will handle all logic/render processing,
     * as it delegates this to the ECS, which handles
     * ordering and so on.
     */
    public void process() {
        m_artemisWorld.process();
    }

    /**
     * Attempts to place a block at position with the type, can fail. If it succeeds it will *not*
     * notify anything (network wise). If it succeeds, it will take care of destroying e.g. nearby grass,
     * and doing whatever else may need to be done on nearby conditions
     *
     * @param x
     * @param y
     * @param placedBlockType
     *         block type to change it to
     *
     * @return true if placement succeeded.
     */
    public boolean attemptBlockPlacement(int x, int y, byte placedBlockType) {
        byte blockType = blockTypeSafely(x, y);

        //attempt to place one if the area is empty
        if (blockType == OreBlock.BlockType.NullBlockType) {
            setBlockType(x, y, placedBlockType);

            int bottomBlockX = x;
            int bottomBlockY = y + 1;
            if (blockHasFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock)) {
                //remove grass flag here.
                unsetBlockFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock);
            }

            return true;
        }
        //TODO: check collision with other entities...

        return false;
    }

    public Vector2 mousePositionWorldCoords() {
        //libgdx can and probably will return negative mouse coords..
        Vector3 mouse = new Vector3(Math.max(Gdx.input.getX(), 0), Math.max(Gdx.input.getY(), 0), 0f);
        Vector3 finalMouse = m_camera.unproject(mouse);

        return new Vector2(finalMouse.x, finalMouse.y);
    }

    public void alignPositionToBlocks(Vector2 pos, Vector2 size) {
        float x = MathUtils.floor(pos.x);
        float y = MathUtils.floor(pos.y);

        //if size is odd,  it won't look aligned properly
        if (size.x % 2 == 1) {
            x -= 0.5f;
        }

        //odd
        if (size.y % 2 == 1) {
            y -= 0.5f;
        }

        pos.set(x, y);
    }

    public int seaLevel() {
        return WORLD_SEA_LEVEL;
    }

    /**
     * @param blockType
     */
    public int createBlockItem(byte blockType) {
        final int block = m_artemisWorld.create();
        velocityMapper.create(block);

        BlockComponent blockComponent = blockMapper.create(block);
        blockComponent.blockType = blockType;

        SpriteComponent blockSprite = spriteMapper.create(block);
        blockSprite.textureName = blockAttributes.get(blockComponent.blockType).textureName;

        blockSprite.sprite.setSize(1, 1);

        ItemComponent itemComponent = itemMapper.create(block);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        return block;
    }

    public int createLight() {
        int light = m_artemisWorld.create();

        velocityMapper.create(light);

        ItemComponent itemComponent = itemMapper.create(light);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        PowerDeviceComponent powerDeviceComponent = powerDeviceMapper.create(light);

        SpriteComponent sprite = spriteMapper.create(light);
        sprite.textureName = "light-yellow";

        sprite.sprite.setSize(1, 1);

        PowerConsumerComponent powerConsumerComponent = powerConsumerMapper.create(light);
        powerConsumerComponent.powerDemandRate = 100;

        return light;
    }

    public int createPowerGenerator() {
        int power = m_artemisWorld.create();

        velocityMapper.create(power);

        ItemComponent itemComponent = itemMapper.create(power);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        PowerDeviceComponent powerDeviceComponent = powerDeviceMapper.create(power);

        SpriteComponent sprite = spriteMapper.create(power);
        sprite.textureName = "air-generator-64x64";

        sprite.sprite.setSize(4, 4);

        PowerGeneratorComponent powerComponent = powerGeneratorMapper.create(power);
        powerComponent.powerSupplyRate = 100;

        return power;
    }

    public int createAirGenerator() {
        int air = m_artemisWorld.create();
        ItemComponent itemComponent = itemMapper.create(air);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        velocityMapper.create(air);

        PowerDeviceComponent power = powerDeviceMapper.create(air);

        SpriteComponent airSprite = spriteMapper.create(air);
        airSprite.textureName = "air-generator-64x64";

        airSprite.sprite.setSize(4, 4);

        AirGeneratorComponent airComponent = airGeneratorMapper.create(air);
        airComponent.airOutputRate = 100;

        return air;
    }

    public int createTree() {
        int tree = m_artemisWorld.create();
        ItemComponent itemComponent = itemMapper.create(tree);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        SpriteComponent airSprite = spriteMapper.create(tree);
        airSprite.textureName = "air-generator-64x64";

        airSprite.sprite.setSize(4, 4);

        AirGeneratorComponent airComponent = airGeneratorMapper.create(tree);
        airComponent.airOutputRate = 100;

        return tree;
    }

    /**
     * @param entity
     *         entity id
     *
     * @return true if the item can be placed where it currently resides, without any obstructions
     */
    public boolean isPlacementValid(int entity) {
        SpriteComponent spriteComponent = spriteMapper.get(entity);
        Vector2 pos = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        Vector2 size = new Vector2(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

        float epsilon = 0.001f;
        int startX = (int) ((pos.x - (size.x * 0.5f)) + epsilon);
        int startY = (int) ((pos.y - (size.y * 0.5f)) + epsilon);

        int endX = (int) ((pos.x + (size.x * 0.5f)));
        int endY = (int) ((pos.y + (size.y * 0.5f - epsilon)) + 1);

        if (!(startX >= 0 && startY >= 0 && endX <= WORLD_SIZE_X && endY <= WORLD_SIZE_Y)) {
            //fixme
            //not sure why, but this ends up giving me some way way invalid values. likely due to mouse being outside
            //of valid range, *somehow*. sometimes does it on startup etc
            return false;
        }

        //check collision against blocks first
        for (int x = startX; x < endX; ++x) {
            for (int y = startY; y < endY; ++y) {
                if (blockType(x, y) != OreBlock.BlockType.NullBlockType) {
                    return false;
                }
            }
        }

        //float x = Math.min(pos.x - (BLOCK_SIZE * 20), 0.0f);
        //float y = Math.min(pos.y - (BLOCK_SIZE * 20), 0.0f);
        //float x2 = Math.min(pos.x + (BLOCK_SIZE * 20), WORLD_SIZE_X * BLOCK_SIZE);
        //float y2 = Math.min(pos.y + (BLOCK_SIZE * 20), WORLD_SIZE_Y * BLOCK_SIZE);

        //check collision against entities
        IntBag entities =
                m_artemisWorld.getAspectSubscriptionManager().get(Aspect.all(SpriteComponent.class)).getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            //it's the item we're trying to place, don't count a collision with ourselves
            if (entities.get(i) == entity) {
                continue;
            }

            //ignore players, aka don't count them as colliding when placing static objects.
            //        if (e.has_component<PlayerComponent>()) {
            //            continue;
            //        }

            ItemComponent itemComponent = itemMapper.getSafe(entities.get(i));
            if (itemComponent != null) {
                // items that are dropped in the world are considered non colliding
                if (itemComponent.state == ItemComponent.State.DroppedInWorld) {
                    continue;
                }
            }

            //            if ( m_artemisWorld.getSystem(TagManager.class).getTag(entities.get(i)) != null) {
            //           }

            SpriteComponent entitySpriteComponent = spriteMapper.get(entities.get(i));
            // possible colliding object is not meant to be collided with. skip it/don't count it
            if (entitySpriteComponent.noClip) {
                continue;
            }

            if (entityCollides(entities.get(i), entity)) {
                return false;
            }
        }

        return true;
    }

    private boolean entityCollides(int first, int second) {
        SpriteComponent spriteComponent1 = spriteMapper.get(first);
        SpriteComponent spriteComponent2 = spriteMapper.get(second);

        Vector2 pos1 = new Vector2(spriteComponent1.sprite.getX(), spriteComponent1.sprite.getY());
        Vector2 pos2 = new Vector2(spriteComponent2.sprite.getX(), spriteComponent2.sprite.getY());

        Vector2 size1 = new Vector2(spriteComponent1.sprite.getWidth(), spriteComponent1.sprite.getHeight());
        Vector2 size2 = new Vector2(spriteComponent2.sprite.getWidth(), spriteComponent2.sprite.getHeight());

        float epsilon = 0.0001f;

        float left1 = pos1.x - (size1.x * 0.5f) + epsilon;
        float right1 = pos1.x + (size1.x * 0.5f) - epsilon;
        float top1 = pos1.y - (size1.y * 0.5f) + epsilon;
        float bottom1 = pos1.y + (size1.y * 0.5f) - epsilon;

        float left2 = pos2.x - (size2.x * 0.5f) + epsilon;
        float right2 = pos2.x + (size2.x * 0.5f) - epsilon;

        float top2 = pos2.y - (size2.y * 0.5f) + epsilon;
        float bottom2 = pos2.y + (size2.y * 0.5f) - epsilon;

        boolean collides = !(left2 > right1 || right2 < left1 || top2 > bottom1 || bottom2 < top1);

        return collides;
    }

    public static void log(String tag, String message) {
        java.time.LocalDateTime datetime = java.time.LocalDateTime.now();
        String time = datetime.format(java.time.format.DateTimeFormatter.ofPattern("HH:m:s:S"));

        Gdx.app.log(tag, message + " [" + time + " ]");
    }

    public void loadSparseBlockUpdate(Network.SparseBlockUpdate update) {
        //log("sparse block update", "loaded, count: " + update.blocks.size);

        for (Network.SingleSparseBlock sparseBlock : update.blocks) {
            int x = sparseBlock.x;
            int y = sparseBlock.y;
            setBlockType(x, y, sparseBlock.block.type);
            setBlockWallType(x, y, sparseBlock.block.wallType);
            setBlockFlags(x, y, sparseBlock.block.flags);
        }
    }

    public void loadBlockRegion(Network.BlockRegion region) {

        int sourceIndex = 0;
        for (int y = region.y; y <= region.y2; ++y) {
            for (int x = region.x; x <= region.x2; ++x) {
                setBlockType(x, y, region.blocks[sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                                 Network.BlockRegion.BLOCK_FIELD_INDEX_TYPE]);
                setBlockWallType(x, y, region.blocks[sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                                     Network.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE]);
                setBlockFlags(x, y, region.blocks[sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT +
                                                  Network.BlockRegion.BLOCK_FIELD_INDEX_FLAGS]);

                ++sourceIndex;
            }
        }

        //log("client block region", String.format("loaded %s tile index", sourceIndex));

        //fixme should re transition tiles in this area
    }

    /**
     * Clone everything about the entity and adds it to the engine/world
     *
     * @param sourceEntity
     *         to clone
     *
     * @return the cloned entity
     */
    public int cloneEntity(int sourceEntity) {
        int clonedEntity = m_artemisWorld.create();

        //sorted alphabetically for your pleasure
        if (airMapper.has(sourceEntity)) {
            AirComponent sourceComponent = airMapper.get(sourceEntity);
            AirComponent component = airMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (airGeneratorMapper.has(sourceEntity)) {
            AirGeneratorComponent sourceComponent = airGeneratorMapper.get(sourceEntity);
            AirGeneratorComponent component = airGeneratorMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (blockMapper.has(sourceEntity)) {
            BlockComponent sourceComponent = blockMapper.get(sourceEntity);
            BlockComponent component = blockMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (controlMapper.has(sourceEntity)) {
            ControllableComponent sourceComponent = controlMapper.get(sourceEntity);
            ControllableComponent component = controlMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (healthMapper.has(sourceEntity)) {
            HealthComponent sourceComponent = healthMapper.get(sourceEntity);
            HealthComponent component = healthMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (itemMapper.has(sourceEntity)) {
            ItemComponent sourceComponent = itemMapper.get(sourceEntity);
            ItemComponent component = itemMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (jumpMapper.has(sourceEntity)) {
            JumpComponent sourceComponent = jumpMapper.get(sourceEntity);
            JumpComponent component = jumpMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        //player, unneeded
        assert playerMapper.getSafe(sourceEntity) == null;

        if (spriteMapper.has(sourceEntity)) {
            SpriteComponent sourceComponent = spriteMapper.get(sourceEntity);
            SpriteComponent component = spriteMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);

            if (worldInstanceType != WorldInstanceType.Server) {
                component.sprite.setRegion(m_atlas.findRegion(component.textureName));
            }
        }

        if (toolMapper.has(sourceEntity)) {
            ToolComponent sourceComponent = toolMapper.get(sourceEntity);
            ToolComponent component = toolMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (lightMapper.has(sourceEntity)) {
            LightComponent sourceComponent = lightMapper.get(sourceEntity);
            LightComponent component = lightMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (velocityMapper.has(sourceEntity)) {
            VelocityComponent sourceComponent = velocityMapper.get(sourceEntity);
            VelocityComponent component = velocityMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (powerDeviceMapper.has(sourceEntity)) {
            PowerDeviceComponent sourceComponent = powerDeviceMapper.get(sourceEntity);
            PowerDeviceComponent component = powerDeviceMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (powerConsumerMapper.has(sourceEntity)) {
            PowerConsumerComponent sourceComponent = powerConsumerMapper.get(sourceEntity);
            PowerConsumerComponent component = powerConsumerMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        if (powerGeneratorMapper.has(sourceEntity)) {
            PowerGeneratorComponent sourceComponent = powerGeneratorMapper.get(sourceEntity);
            PowerGeneratorComponent component = powerGeneratorMapper.create(clonedEntity);
            component.copyFrom(sourceComponent);
        }

        return clonedEntity;
    }

    /**
     * gets the player entity that corresponds to this player id.
     *
     * @param playerId
     *         the playerid of the player
     *
     * @return the player entity
     */
    public int playerEntityForPlayerID(int playerId) {
        IntBag entities =
                m_artemisWorld.getAspectSubscriptionManager().get(Aspect.all(PlayerComponent.class)).getEntities();
        PlayerComponent playerComponent;
        for (int i = 0; i < entities.size(); ++i) {
            playerComponent = playerMapper.get(entities.get(i));
            if (playerComponent.connectionPlayerId == playerId) {
                return entities.get(i);
            }
        }

        throw new IllegalStateException("player id attempted to be obtained from item, but this player does not exist");
    }

    //fixme better way to do key and mouse events. i'd like to just have systems be able to sign up,
    //and they can process that in there. or maybe this should be in the client..after all, a server has no key events
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            PowerOverlayRenderSystem powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem.class);
            if (powerOverlaySystem.overlayVisible) {
                powerOverlaySystem.leftMouseClicked();
            }

            return true;
        } else {
            //right
            PowerOverlayRenderSystem powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem.class);
            if (powerOverlaySystem.overlayVisible) {
                powerOverlaySystem.rightMouseClicked();
            }

        }

        return false;
    }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) {
            return false;
        }

        PowerOverlayRenderSystem powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem.class);
        if (powerOverlaySystem.overlayVisible) {
            powerOverlaySystem.leftMouseReleased();
            return true;
        } else {
        }

        return false;
    }

    /**
     * gets a list of components this entity has. Mostly for debug
     *
     * @param entity
     *
     * @return
     */
    public Bag<Component> getComponentsForEntity(int entity) {
        assert m_artemisWorld != null;

        Bag<Component> bag = new Bag<>();
        m_artemisWorld.getEntity(entity).getComponents(bag);

        return bag;
    }

    public static class BlockAttributes {
        public String textureName; //e.g. "dirt", "stone", etc.
        /**
         * whether or not things should collide with this block
         */
        Collision collision;

        public BlockCategory category;

        /**
         * max starting health of the block
         */
        public float blockTotalHealth;

        //if this type is a type of ore (like stone, copper, ...)
        public enum BlockCategory {
            Null,
            Dirt,
            Ore
            //liquid
        }

        public enum Collision {
            True,
            False
        }

        BlockAttributes(String textureName, Collision collides, BlockCategory category, short blockTotalHealth) {
            this.textureName = textureName;
            this.collision = collides;
            this.category = category;
            this.blockTotalHealth = blockTotalHealth;
        }
    }
}
