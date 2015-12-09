package com.ore.infinium;

import com.artemis.*;
import com.artemis.managers.PlayerManager;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
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
import com.badlogic.gdx.utils.IntArray;
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
    public static final float PIXELS_PER_METER = 50.0f;
    public static final float GRAVITY_ACCEL = 9.8f / PIXELS_PER_METER / 3.0f;
    public static final float GRAVITY_ACCEL_CLAMP = 9.8f / PIXELS_PER_METER / 3.0f;

    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;

    public static final int WORLD_SIZE_X = 1000; //2400
    public static final int WORLD_SIZE_Y = 1000; //8400
    public static final int WORLD_SEA_LEVEL = 50;

    /**
     * indicates an invalid entity id
     */
    public static final int ENTITY_INVALID = -1;

    /**
     * looks up the texture prefix name for each block type. e.g. DirtBlockType -> "dirt", etc.
     */
    public static final HashMap<Byte, BlockStruct> blockTypes = new HashMap<>();

    static {
        blockTypes.put(Block.BlockType.NullBlockType,
                       new BlockStruct("", BlockStruct.Collision.False, BlockStruct.BlockCategory.Null));
        blockTypes.put(Block.BlockType.DirtBlockType,
                       new BlockStruct("dirt", BlockStruct.Collision.True, BlockStruct.BlockCategory.Dirt));
        blockTypes.put(Block.BlockType.StoneBlockType,
                       new BlockStruct("stone", BlockStruct.Collision.True, BlockStruct.BlockCategory.Ore));
    }

    public Block[] blocks;

    //fixme players really should be always handled by the system..and i suspect a lot of logic can be handled by
    // them alone.
    public IntArray m_players = new IntArray();
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
    public OreWorld(OreClient client, OreServer server) {

        m_client = client;
        m_server = server;

        blocks = new Block[WORLD_SIZE_Y * WORLD_SIZE_X];
    }

    void init() {
        assert isClient() ^ isServer();

        assert isHotspotOptimizationEnabled() : "error, hotspot optimization (artemis-odb weaving) is not enabled";

        if (isClient()) {

            m_camera = new OrthographicCamera(1600 / OreWorld.PIXELS_PER_METER,
                                              900 / OreWorld.PIXELS_PER_METER);//30, 30 * (h / w));
            m_camera.setToOrtho(true, 1600 / OreWorld.PIXELS_PER_METER, 900 / OreWorld.PIXELS_PER_METER);

            m_atlas = new TextureAtlas(Gdx.files.internal("packed/entities.atlas"));

            //we don't generate the block world with noise, just init it so it isn't
            //it isn't null
            initializeBlocksArray();

            //note although it may look like it.. order for render/logic ones..actually doesn't matter, their base
            // class dictates this.
            m_artemisWorld = new World(
                    new WorldConfigurationBuilder().register(new GameLoopSystemInvocationStrategy(25))
                                                   .with(new ProfilerSystem(m_client.m_skin))
                                                   .with(new TagManager())
                                                   .with(new PlayerManager())
                                                   .with(new MovementSystem(this))
                                                   .with(new NetworkClientSystem(this))
                                                   .with(new PowerCircuitSystem(this))
                                                   .with(new InputSystem(m_camera, this))
                                                   .with(new PlayerSystem(this))
                                                   .with(new DebugTextRenderSystem(m_camera, this))
                                                   .with(new PowerOverlayRenderSystem(this))
                                                   .with(new SpriteRenderSystem(this))
                                                   .with(new TileRenderSystem(m_camera, this))
                                                   .with(new TileTransitionSystem(m_camera, this))
                                                   .build());
            //b.dependsOn(WorldConfigurationBuilder.Priority.LOWEST + 1000,ProfilerSystem.class);

            //inject the mappers into the world, before we start doing things
            m_artemisWorld.inject(this, true);
            int crosshair = m_artemisWorld.create();
            m_artemisWorld.getSystem(TagManager.class).register(s_crosshair, crosshair);
            m_artemisWorld.process();

            SpriteComponent spriteComponent = spriteMapper.create(crosshair);
            spriteComponent.sprite.setSize(BLOCK_SIZE, BLOCK_SIZE);
            spriteComponent.sprite.setRegion(m_atlas.findRegion("crosshair-blockpicking"));
            spriteComponent.noClip = true;

            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
        } else {
            m_artemisWorld = new World(new WorldConfigurationBuilder().with(new TagManager())
                                                                      .with(new PlayerManager())
                                                                      .with(new MovementSystem(this))
                                                                      .with(new PowerCircuitSystem(this))
                                                                      .with(new GrassBlockSystem(this))
                                                                      .with(new PlayerSystem(this))
                                                                      .with(new NetworkServerSystem(this, m_server))
                                                                      .register(
                                                                              new GameLoopSystemInvocationStrategy(25))
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
        return !ClassReflection.isAssignableFrom(EntityProcessingSystem.class, NetworkClientSystem.class);
    }

    //TODO cleanup, can be broken down into various handlers, early returns and handling multiple cases make it
    // convoluted
    protected void clientHotbarInventoryItemSelected() {
        assert !isServer();

        int mainPlayer = m_artemisWorld.getSystem(TagManager.class).getEntity(s_mainPlayer).getId();
        PlayerComponent playerComponent = playerMapper.get(mainPlayer);
        int equippedEntity = playerComponent.getEquippedPrimaryItem();

        //if it is here, remove it...we respawn the placement overlay further down either way.
        Entity placementOverlay = m_artemisWorld.getSystem(TagManager.class).getEntity(s_itemPlacementOverlay);
        if (placementOverlay != null) {
            m_artemisWorld.delete(placementOverlay.getId());
        }

        if (equippedEntity == ENTITY_INVALID) {
            return;
        }

        SpriteComponent crosshairSprite =
                spriteMapper.get(m_artemisWorld.getSystem(TagManager.class).getEntity(s_crosshair));
        crosshairSprite.visible = false;

        assert crosshairSprite.noClip;

        if (blockMapper.has(equippedEntity)) {
            // if the switched to item is a block, we should show a crosshair overlay
            crosshairSprite.visible = true;

            //don't show the placement overlay for blocks, just items and other placeable things
            return;
        }

        ToolComponent entityToolComponent = toolMapper.getSafe(equippedEntity);
        if (entityToolComponent != null) {
            if (entityToolComponent.type == ToolComponent.ToolType.Drill) {
                //drill, one of the few cases we want to show the block crosshair...
                crosshairSprite.visible = true;

                //drill has no placement overlay
                //fixme: return;
            }
        }

        //this item is placeable, show an overlay of it so we can see where we're going to place it (by cloning its
        // entity)
        int newPlacementOverlay = cloneEntity(equippedEntity);
        ItemComponent itemComponent = itemMapper.get(newPlacementOverlay);
        //transition to the in world state, since the cloned source item was in the inventory state, so to would this
        itemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = spriteMapper.get(newPlacementOverlay);
        spriteComponent.noClip = true;

        //crosshair shoudln't be visible if the power overlay is
        if (m_artemisWorld.getSystem(PowerOverlayRenderSystem.class).overlayVisible) {
            spriteComponent.visible = false;
        }

        m_artemisWorld.getSystem(TagManager.class).register(s_itemPlacementOverlay, newPlacementOverlay);
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
        playerComponent.connectionId = connectionId;
        //fixme fixme, should be consolidated w/ sprite's noclip...or should it?? make mention, is sprite present on
        // the server?? at least the component, maybe not inner sprite
        playerComponent.noClip = m_noClipEnabled;

        playerComponent.playerName = playerName;
        playerComponent.loadedViewport.setRect(
                new Rectangle(0, 0, LoadedViewport.MAX_VIEWPORT_WIDTH, LoadedViewport.MAX_VIEWPORT_HEIGHT));
        playerComponent.loadedViewport.centerOn(new Vector2(playerSprite.sprite.getX() / OreWorld.BLOCK_SIZE,
                                                            playerSprite.sprite.getY() / OreWorld.BLOCK_SIZE));

        playerSprite.sprite.setSize(OreWorld.BLOCK_SIZE * 2, OreWorld.BLOCK_SIZE * 3);
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
                Block block = blockAt(x, y);

                //fixme check biomes and their ranges
                //fill the surface/exposed dirt blocks with grass blocks
                if (block.type == Block.BlockType.DirtBlockType) {
                    Block topBlock = blockAtSafely(x, y - 1);

                    if (topBlock.type == Block.BlockType.NullBlockType) {
                        block.setFlag(Block.BlockFlags.GrassBlock);
                        y = WORLD_SIZE_Y;
                    }
                }
            }
        }

        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {
                Block block = blockAt(x, y);

                if (block.type == Block.BlockType.DirtBlockType && block.hasFlag(Block.BlockFlags.GrassBlock)) {
                    Block topBlock = blockAtSafely(x, y - 1);
                    Block bottomBlock = blockAtSafely(x, y + 1);
                    Block bottomLeftBlock = blockAtSafely(x - 1, y + 1);
                    Block bottomRightBlock = blockAtSafely(x + 1, y + 1);

                    //                    boolean leftEmpty =

                    if (topBlock.type == Block.BlockType.NullBlockType) {
                        block.setFlag(Block.BlockFlags.GrassBlock);
                    }
                }
            }
        }
    }

    public void initializeBlocksArray() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                int index = x * WORLD_SIZE_Y + y;
                blocks[index] = new Block();
                blocks[index].type = Block.BlockType.NullBlockType;
            }
        }
    }

    private void generateOres() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                int index = x * WORLD_SIZE_Y + y;

                //java wants me to go through each and every block and initialize them..
                Block block = new Block();
                blocks[index] = block;
                block.type = Block.BlockType.NullBlockType;
                block.wallType = Block.WallType.NullWallType;

                //create some sky
                if (y <= seaLevel()) {
                    continue;
                }

                boolean underground = true;

                switch (MathUtils.random(0, 3)) {
                    case 0:
                        block.type = Block.BlockType.NullBlockType;
                        break;

                    case 1:
                        block.type = Block.BlockType.DirtBlockType;
                        break;
                    case 2:
                        //fixme, simulate only dirt for now. blocks[index].type = Block.BlockType.StoneBlockType;
                        block.type = Block.BlockType.DirtBlockType;
                        break;
                }

                if (underground) {
                    block.wallType = Block.WallType.DirtUndergroundWallType;
                }

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

    public boolean isServer() {
        return m_server != null;
    }

    public boolean isClient() {
        return m_client != null;
    }

    public Block blockAtPosition(Vector2 pos) {
        int x = MathUtils.clamp((int) (pos.x / BLOCK_SIZE), 0, WORLD_SIZE_X - 1);
        int y = MathUtils.clamp((int) (pos.y / BLOCK_SIZE), 0, WORLD_SIZE_Y - 1);
        return blockAt(x, y);
    }

    /**
     * safely return a block at x, y, clamped at world bounds
     *
     * @param x
     * @param y
     *
     * @return
     */
    public Block blockAtSafely(int x, int y) {
        return blocks[blockXSafe(x) * WORLD_SIZE_Y + blockYSafe(y)];
    }

    public int blockXSafe(int x) {
        return MathUtils.clamp(x, 0, WORLD_SIZE_X - 1);
    }

    public int blockYSafe(int y) {
        return MathUtils.clamp(y, 0, WORLD_SIZE_Y - 1);
    }

    public Block blockAt(int x, int y) {
        assert x >= 0 && y >= 0 && x <= WORLD_SIZE_X && y <= WORLD_SIZE_Y :
                String.format("block index out of range. x: %d, y: %d", x, y);

        return blocks[x * WORLD_SIZE_Y + y];
    }

    public boolean isBlockSolid(int x, int y) {
        boolean solid = true;

        byte type = blockAt(x, y).type;

        if (type == Block.BlockType.NullBlockType) {
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

    public void process() {
        if (isClient()) {
            //        playerSprite.sprite.setOriginCenter();

            //        m_camera.position.set(playerSprite.sprite.getX() + playerSprite.sprite.getWidth() * 0.5f,
            // playerSprite
            // .sprite.getY() + playerSprite.sprite.getHeight() * 0.5f, 0);

            updateCrosshair();
            updateItemPlacementOverlay();

            //fixme
            if (m_artemisWorld.getSystem(TagManager.class).isRegistered(s_itemPlacementOverlay)) {
                SpriteComponent component = spriteMapper.getSafe(
                        m_artemisWorld.getSystem(TagManager.class).getEntity(s_itemPlacementOverlay).getId());
                assert component != null : "how the hell does it have no spritecomp?!!";

                assert component.noClip : "placement overlay found to not be in noclip mode!!!";
            }

        }

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
        Block block = blockAtSafely(x, y);

        //attempt to place one if the area is empty
        if (block.type == Block.BlockType.NullBlockType) {
            block.type = placedBlockType;

            Block bottomBlock = blockAtSafely(x, y + 1);
            if (bottomBlock.hasFlag(Block.BlockFlags.GrassBlock)) {
                //remove grass flag here.
                bottomBlock.unsetFlag(Block.BlockFlags.GrassBlock);
            }

            return true;
        }
        //TODO: check collision with other entities...

        return false;
    }

    public void render(double elapsed) {
        //        m_camera.zoom *= 0.9;
        //m_lightRenderer->renderToFBO();

        //FIXME: incorporate entities into the pre-lit gamescene FBO, then render lighting as last pass
        //m_lightRenderer->renderToBackbuffer();

        //FIXME: take lighting into account, needs access to fbos though.
        //   m_fluidRenderer->render();
        //    m_particleRenderer->render();
        //FIXME unused    m_quadTreeRenderer->render();
    }

    private void updateCrosshair() {
        //PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);
        //playerComponent

        SpriteComponent spriteComponent =
                spriteMapper.get(m_artemisWorld.getSystem(TagManager.class).getEntity(s_crosshair).getId());

        Vector2 mouse = mousePositionWorldCoords();
        Vector2 crosshairPosition = new Vector2(BLOCK_SIZE * MathUtils.floor(mouse.x / BLOCK_SIZE),
                                                BLOCK_SIZE * MathUtils.floor(mouse.y / BLOCK_SIZE));

        Vector2 crosshairOriginOffset =
                new Vector2(spriteComponent.sprite.getWidth() * 0.5f, spriteComponent.sprite.getHeight() * 0.5f);

        Vector2 crosshairFinalPosition = crosshairPosition.add(crosshairOriginOffset);

        spriteComponent.sprite.setPosition(crosshairFinalPosition.x, crosshairFinalPosition.y);
    }

    public Vector2 mousePositionWorldCoords() {
        //libgdx can and probably will return negative mouse coords..
        Vector3 mouse = new Vector3(Math.max(Gdx.input.getX(), 0), Math.max(Gdx.input.getY(), 0), 0f);
        Vector3 finalMouse = m_camera.unproject(mouse);

        return new Vector2(finalMouse.x, finalMouse.y);
    }

    //fixmeasap
    private void updateItemPlacementOverlay() {
        /*
        if (m_itemPlacementOverlayEntity == ENTITY_INVALID) {
            return;
        }

        Vector2 mouse = mousePositionWorldCoords();
        alignPositionToBlocks(mouse);

        SpriteComponent spriteComponent = spriteMapper.get(m_itemPlacementOverlayEntity);
        spriteComponent.sprite.setPosition(mouse.x, mouse.y);
        spriteComponent.placementValid = isPlacementValid(m_itemPlacementOverlayEntity);
        */
    }

    void alignPositionToBlocks(Vector2 pos) {
        pos.set(BLOCK_SIZE * MathUtils.floor(pos.x / BLOCK_SIZE), BLOCK_SIZE * MathUtils.floor(pos.y / BLOCK_SIZE));
    }

    public int seaLevel() {
        return WORLD_SEA_LEVEL;
    }

    /**
     * @param block
     *         block entity id
     * @param blockType
     */
    public void createBlockItem(int block, byte blockType) {
        velocityMapper.create(block);

        BlockComponent blockComponent = blockMapper.create(block);
        blockComponent.blockType = blockType;

        SpriteComponent blockSprite = spriteMapper.create(block);
        blockSprite.textureName = blockTypes.get(blockComponent.blockType).textureName;

        //warning fixme size is fucked
        blockSprite.sprite.setSize(32 / OreWorld.PIXELS_PER_METER, 32 / OreWorld.PIXELS_PER_METER);

        ItemComponent itemComponent = itemMapper.create(block);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;
    }

    public int createLight() {
        int light = m_artemisWorld.create();

        ItemComponent itemComponent = itemMapper.create(light);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        PowerDeviceComponent powerDeviceComponent = powerDeviceMapper.create(light);

        SpriteComponent sprite = spriteMapper.create(light);
        sprite.textureName = "light-blue";

        sprite.sprite.setSize(BLOCK_SIZE * 2, BLOCK_SIZE * 2);

        PowerConsumerComponent powerConsumerComponent = powerConsumerMapper.create(light);
        powerConsumerComponent.powerDemandRate = 100;

        return light;
    }

    public int createPowerGenerator() {
        int power = m_artemisWorld.create();

        ItemComponent itemComponent = itemMapper.create(power);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        PowerDeviceComponent powerDeviceComponent = powerDeviceMapper.create(power);

        SpriteComponent sprite = spriteMapper.create(power);
        sprite.textureName = "air-generator-64x64";

        //warning fixme size is fucked
        sprite.sprite.setSize(BLOCK_SIZE * 4, BLOCK_SIZE * 4);

        PowerGeneratorComponent powerComponent = powerGeneratorMapper.create(power);
        powerComponent.powerSupplyRate = 100;

        return power;
    }

    public int createAirGenerator() {
        int air = m_artemisWorld.create();
        ItemComponent itemComponent = itemMapper.create(air);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;

        PowerDeviceComponent power = powerDeviceMapper.create(air);

        SpriteComponent airSprite = spriteMapper.create(air);
        airSprite.textureName = "air-generator-64x64";

        //warning fixme size is fucked
        airSprite.sprite.setSize(BLOCK_SIZE * 4, BLOCK_SIZE * 4);

        AirGeneratorComponent airComponent = airGeneratorMapper.create(air);
        airComponent.airOutputRate = 100;

        return air;
    }

    /**
     * @param entity
     *         entity id
     *
     * @return true if the item can be placed where it currently resides, without any obstructions
     */
    boolean isPlacementValid(int entity) {
        SpriteComponent spriteComponent = spriteMapper.get(entity);
        Vector2 pos = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        Vector2 size = new Vector2(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

        float epsilon = 0.001f;
        int startX = (int) ((pos.x - (size.x * 0.5f)) / BLOCK_SIZE + epsilon);
        int startY = (int) ((pos.y - (size.y * 0.5f)) / BLOCK_SIZE + epsilon);

        int endX = (int) ((pos.x + (size.x * 0.5f)) / BLOCK_SIZE + 0);
        int endY = (int) ((pos.y + (size.y * 0.5f - epsilon)) / BLOCK_SIZE + 1);

        if (!(startX >= 0 && startY >= 0 && endX <= WORLD_SIZE_X && endY <= WORLD_SIZE_Y)) {
            //fixme
            //not sure why, but this ends up giving me some way way invalid values. likely due to mouse being outside
            //of valid range, *somehow*. sometimes does it on startup etc
            return false;
        }

        //check collision against blocks first
        for (int x = startX; x < endX; ++x) {
            for (int y = startY; y < endY; ++y) {
                if (blockAt(x, y).type != Block.BlockType.NullBlockType) {
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
        log("sparse block update", "loaded, count: " + update.blocks.size);

        for (Network.SingleSparseBlock sparseBlock : update.blocks) {
            Block originalBlock = blockAt(sparseBlock.x, sparseBlock.y);
            originalBlock.type = sparseBlock.block.type;
            originalBlock.wallType = sparseBlock.block.wallType;
            originalBlock.flags = sparseBlock.block.flags;
        }
    }

    public void loadBlockRegion(Network.BlockRegion region) {
        int sourceIndex = 0;
        for (int y = region.y; y <= region.y2; ++y) {
            for (int x = region.x; x <= region.x2; ++x) {
                Block origBlock = blockAt(x, y);
                Network.SingleBlock srcBlock = region.blocks.get(sourceIndex);
                origBlock.type = srcBlock.type;
                origBlock.wallType = srcBlock.wallType;
                origBlock.flags = srcBlock.flags;

                //fixme wall type as well

                ++sourceIndex;
            }
        }

        log("block region", String.format("loaded %s tiles", sourceIndex));

        //fixme should re transition tiles in this area
    }

    /**
     * Clone everything about the entity and adds it to the engine/world
     *
     * @param entity
     *         to clone
     *
     * @return the cloned entity
     */
    public int cloneEntity(int entity) {
        int clonedEntity = m_artemisWorld.create();

        //sorted alphabetically for your pleasure
        if (airMapper.has(entity)) {
            airMapper.create(clonedEntity);
        }

        if (airGeneratorMapper.has(entity)) {
            airGeneratorMapper.create(clonedEntity);
        }

        if (blockMapper.has(entity)) {
            blockMapper.create(clonedEntity);
        }

        if (controlMapper.has(entity)) {
            controlMapper.create(clonedEntity);
        }

        if (healthMapper.has(entity)) {
            healthMapper.create(clonedEntity);
        }

        if (itemMapper.has(entity)) {
            itemMapper.create(clonedEntity);
        }

        if (jumpMapper.has(entity)) {
            jumpMapper.create(clonedEntity);
        }

        //player, unneeded
        assert playerMapper.getSafe(entity) == null;

        if (spriteMapper.has(entity)) {
            spriteMapper.create(clonedEntity);
        }

        if (toolMapper.has(entity)) {
            toolMapper.create(clonedEntity);
        }

        if (lightMapper.has(entity)) {
            lightMapper.create(clonedEntity);
        }

        if (velocityMapper.has(entity)) {
            velocityMapper.create(clonedEntity);
        }

        if (powerDeviceMapper.has(entity)) {
            powerDeviceMapper.create(clonedEntity);
        }

        if (powerConsumerMapper.has(entity)) {
            powerConsumerMapper.create(clonedEntity);
        }

        if (powerGeneratorMapper.has(entity)) {
            powerGeneratorMapper.create(clonedEntity);
        }

        return clonedEntity;
    }

    public void addPlayer(int playerEntity) {
        m_players.add(playerEntity);
    }

    /**
     * gets the player entity that corresponds to this player id.
     *
     * @param playerId
     *         the playerid of the player
     *
     * @return the player entity
     */
    public int playerForID(int playerId) {
        assert !isClient();

        IntBag entities =
                m_artemisWorld.getAspectSubscriptionManager().get(Aspect.all(PlayerComponent.class)).getEntities();
        PlayerComponent playerComponent;
        for (int i = 0; i < entities.size(); ++i) {
            playerComponent = playerMapper.get(entities.get(i));
            if (playerComponent.connectionId == playerId) {
                return entities.get(i);
            }
        }

        throw new IllegalStateException("player id attempted to be obtained from item, but this player does not exist");
    }

    //fixme better way to do key and mouse events. i'd like to just have systems be able to sign up,
    //and they can process that in there. or maybe this should be in the client..after all, a server has no key events
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) {
            return false;
        }

        PowerOverlayRenderSystem powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem.class);
        if (powerOverlaySystem.overlayVisible) {
            powerOverlaySystem.leftMouseClicked();
            return true;
        } else {
            return true;
        }

        //return false;
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

    public static class BlockStruct {
        public String textureName; //e.g. "dirt", "stone", etc.
        Collision collision;
        public BlockCategory category;

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

        BlockStruct(String textureName, Collision collides, BlockCategory category) {
            this.textureName = textureName;
            this.collision = collides;
            this.category = category;
        }
    }
}
