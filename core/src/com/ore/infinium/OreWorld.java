package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.WorldConfigurationBuilder;
import com.artemis.annotations.Wire;
import com.artemis.managers.PlayerManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.*;
import net.mostlyoriginal.plugin.ProfilerPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

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
@Wire
public class OreWorld implements Disposable {
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

    /**
     * @first bitmask of all sides, that maps to valid transition types e.g. left | right, indicates that it needs to
     * mesh on the left and right sides ONLY
     * @second
     */
    public static final HashMap<EnumSet<Transitions>, Integer> dirtTransitionTypes = new HashMap<>();
    public static final HashMap<EnumSet<Transitions>, Integer> grassTransitions = new HashMap<>();
    public static final HashMap<EnumSet<Transitions>, Integer> stoneTransitionTypes = new HashMap<>();

    /**
     * each flag here is handled (possibly, somewhat) differently depending on what kinda
     * block it is. The various types have their own logic, these are just sometimes-shared
     * identifiers.
     * <p>
     * Grass mostly uses the leftEmpty, etc. meaning that it will show grass on the left side of this block
     * Grass additionally uses the left, right, "should merge/transition" rules. That is, grass merges/blends with
     * dirt,
     * so if "left" is set, it means it will be a continuous stretch of dirt on the left side.
     * <p>
     * If eg "bottom" is NOT set, it means that it will look all jagged on that side. If it is set, for grass, it means
     * to blend
     * the dirt on that side.
     */
    private enum Transitions {
        left,
        right,
        top,
        bottom,
        topLeftEmpty,
        topRightEmpty,
        bottomLeftEmpty,
        bottomRightEmpty,

        // show grass on the left side of this current block
        leftEmpty,
        rightEmpty,
        topEmpty,
        bottomEmpty,
        topLeftGrass,
        topRightGrass,

        //
        leftOre,
        rightOre,
        topOre,
        bottomOre,

        //
        leftDirt,
        rightDirt,
        topDirt,
        bottomDirt
    }

    static {
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right, Transitions.top, Transitions.bottom),
                                0);
        dirtTransitionTypes.put(EnumSet.of(Transitions.bottom), 1);
        dirtTransitionTypes.put(EnumSet.of(Transitions.top, Transitions.bottom), 2);
        dirtTransitionTypes.put(EnumSet.of(Transitions.right), 3);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 4);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left), 5);
        dirtTransitionTypes.put(EnumSet.of(Transitions.top), 6);
        dirtTransitionTypes.put(EnumSet.of(Transitions.right, Transitions.bottom), 7);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right, Transitions.bottom), 8);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.bottom), 9);
        dirtTransitionTypes.put(EnumSet.of(Transitions.right, Transitions.top, Transitions.bottom), 10);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.top, Transitions.bottom), 11);
        dirtTransitionTypes.put(EnumSet.of(Transitions.right, Transitions.top), 12);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right, Transitions.top), 13);
        dirtTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.top), 14);
        dirtTransitionTypes.put(EnumSet.noneOf(Transitions.class), 15);

        ///////////////////////////////////////////////////////////////////////////////////

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topLeftEmpty, Transitions.topRightEmpty, Transitions.bottomLeftEmpty,
                           Transitions.bottomRightEmpty), 0);

        grassTransitions.put(
                EnumSet.of(Transitions.rightDirt, Transitions.bottomDirt, Transitions.leftEmpty, Transitions.topEmpty),
                1);

        grassTransitions.put(EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topEmpty), 2);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.bottomDirt, Transitions.topEmpty, Transitions.rightEmpty),
                3);

        grassTransitions.put(
                EnumSet.of(Transitions.rightDirt, Transitions.bottomDirt, Transitions.topDirt, Transitions.leftEmpty),
                4);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.topDirt, Transitions.bottomDirt, Transitions.rightEmpty),
                5);

        grassTransitions.put(
                EnumSet.of(Transitions.topDirt, Transitions.rightDirt, Transitions.leftEmpty, Transitions.bottomEmpty),
                6);

        grassTransitions.put(
                EnumSet.of(Transitions.topDirt, Transitions.leftDirt, Transitions.rightDirt, Transitions.bottomEmpty),
                7);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.topDirt, Transitions.rightEmpty, Transitions.bottomEmpty),
                8);

        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.rightEmpty, Transitions.topEmpty, Transitions.bottomDirt),
                9);
        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.rightEmpty, Transitions.topEmpty, Transitions.bottomDirt,
                           Transitions.topLeftEmpty, Transitions.topRightEmpty), 9);

        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.rightEmpty, Transitions.topDirt, Transitions.bottomDirt),
                10);

        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.topEmpty, Transitions.bottomEmpty, Transitions.rightDirt),
                11);
        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.topEmpty, Transitions.bottomEmpty, Transitions.rightDirt,
                           Transitions.topLeftEmpty, Transitions.topRightEmpty, Transitions.bottomLeftEmpty,
                           Transitions.bottomRightEmpty), 11);

        grassTransitions.put(
                EnumSet.of(Transitions.topEmpty, Transitions.bottomEmpty, Transitions.leftDirt, Transitions.rightDirt),
                12);

        grassTransitions.put(
                EnumSet.of(Transitions.topEmpty, Transitions.bottomEmpty, Transitions.rightEmpty, Transitions.leftDirt),
                13);

        grassTransitions.put(
                EnumSet.of(Transitions.leftEmpty, Transitions.rightEmpty, Transitions.bottomEmpty, Transitions.topDirt),
                14);

        grassTransitions.put(EnumSet.of(Transitions.topEmpty, Transitions.bottomEmpty, Transitions.leftEmpty,
                                        Transitions.rightEmpty), 15);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topLeftEmpty), 16); //hack 16, probably need one without bottom,etc

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topRightEmpty), 17); //HACK

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.bottomLeftEmpty), 18);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.bottomRightEmpty), 19);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topLeftEmpty, Transitions.topRightEmpty), 20);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topLeftEmpty, Transitions.bottomLeftEmpty), 21);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.topRightEmpty, Transitions.bottomRightEmpty), 22);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt,
                           Transitions.bottomLeftEmpty, Transitions.bottomRightEmpty), 23);

        grassTransitions.put(
                EnumSet.of(Transitions.rightDirt, Transitions.topDirt, Transitions.bottomDirt, Transitions.leftEmpty),
                24);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.topDirt, Transitions.bottomDirt, Transitions.rightEmpty),
                25);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.topDirt, Transitions.bottomEmpty),
                26);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.bottomDirt, Transitions.topEmpty),
                27);

        grassTransitions.put(
                EnumSet.of(Transitions.leftDirt, Transitions.topDirt, Transitions.topLeftEmpty, Transitions.rightEmpty,
                           Transitions.bottomEmpty), 28);

        grassTransitions.put(
                EnumSet.of(Transitions.rightDirt, Transitions.topDirt, Transitions.topRightEmpty, Transitions.leftEmpty,
                           Transitions.bottomEmpty), 29);

        grassTransitions.put(EnumSet.of(Transitions.rightDirt, Transitions.bottomDirt, Transitions.bottomRightEmpty,
                                        Transitions.leftEmpty), 31);
        grassTransitions.put(EnumSet.of(Transitions.rightDirt, Transitions.bottomDirt, Transitions.bottomRightEmpty,
                                        Transitions.leftEmpty, Transitions.bottomLeftEmpty, Transitions.topLeftEmpty),
                             31);
        grassTransitions.put(
                EnumSet.of(Transitions.rightDirt, Transitions.bottomDirt, Transitions.leftEmpty, Transitions.topEmpty,
                           Transitions.topLeftEmpty, Transitions.topRightEmpty, Transitions.bottomLeftEmpty,
                           Transitions.bottomRightEmpty), 31);

        //hack?

        grassTransitions.put(EnumSet.of(Transitions.leftEmpty, Transitions.topEmpty, Transitions.rightDirt), 1);
        ////////////////////

        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right, Transitions.top, Transitions.bottom),
                                 0);
        stoneTransitionTypes.put(EnumSet.of(Transitions.right, Transitions.bottom), 1);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 2);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.bottom), 3);
        stoneTransitionTypes.put(EnumSet.of(Transitions.right, Transitions.bottom, Transitions.top), 4);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.top, Transitions.bottom), 5);
        stoneTransitionTypes.put(EnumSet.of(Transitions.top, Transitions.right), 6);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right, Transitions.top), 7);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 8);
        stoneTransitionTypes.put(EnumSet.of(Transitions.bottom), 9);
        stoneTransitionTypes.put(EnumSet.of(Transitions.bottom, Transitions.top), 10);
        stoneTransitionTypes.put(EnumSet.of(Transitions.right), 11);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 12);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left), 13);
        stoneTransitionTypes.put(EnumSet.of(Transitions.top), 14);
        stoneTransitionTypes.put(
                EnumSet.of(Transitions.leftDirt, Transitions.rightDirt, Transitions.bottomDirt, Transitions.topDirt),
                15);
        stoneTransitionTypes.put(EnumSet.of(Transitions.bottom, Transitions.right), 16);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.bottom, Transitions.right), 17);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.bottom), 18);
        stoneTransitionTypes.put(EnumSet.of(Transitions.top, Transitions.bottom, Transitions.right), 19);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.top, Transitions.bottom), 20);
        stoneTransitionTypes.put(EnumSet.of(Transitions.top, Transitions.right), 21);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 22);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.top), 23);
        stoneTransitionTypes.put(EnumSet.of(Transitions.bottom), 24);
        stoneTransitionTypes.put(EnumSet.of(Transitions.bottom, Transitions.top), 25);
        stoneTransitionTypes.put(EnumSet.of(Transitions.right), 26);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left, Transitions.right), 27);
        stoneTransitionTypes.put(EnumSet.of(Transitions.left), 28);
        stoneTransitionTypes.put(EnumSet.of(Transitions.top), 29);
        stoneTransitionTypes.put(EnumSet.noneOf(Transitions.class), 30);
    }

    // zoom every n ms, while zoom key is held down
    private static final int zoomInterval = 30;
    private static OreTimer m_zoomTimer = new OreTimer();

    public Block[] blocks;

    //fixme players really should be always handled by the system..and i suspect a lot of logic can be handled by
    // them alone.
    public IntArray m_players = new IntArray();
    public int m_mainPlayerEntity = ENTITY_INVALID;
    public OreServer m_server;
    public AssetManager assetManager;
    public OreClient m_client;
    public OrthographicCamera m_camera;

    //fixme remove in favor of the render system
    public TextureAtlas m_atlas;
    protected TileRenderer m_tileRenderer;
    PowerOverlayRenderSystem m_powerOverlaySystem;
    public PowerCircuitSystem m_powerCircuitSystem;

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
    private int m_blockPickingCrosshairEntity = ENTITY_INVALID;
    int m_itemPlacementOverlayEntity = ENTITY_INVALID;

    com.artemis.World m_artemisWorld;

    public OreWorld(OreClient client, OreServer server) {

        m_client = client;
        m_server = server;

        if (isClient()) {
            m_artemisWorld = new com.artemis.World(new WorldConfigurationBuilder().dependsOn(ProfilerPlugin.class)
                                                                                  .with(new TagManager(),
                                                                                        new PlayerManager())
                                                                                  .with(new MovementSystem(this))
                                                                                  .with(new PowerCircuitSystem(this))
                                                                                  .with(new PlayerSystem(this))
                                                                                  .build());

            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
        } else {

            //hack
            m_artemisWorld =
                    new com.artemis.World(new WorldConfigurationBuilder().dependsOn(ProfilerPlugin.class).build());
        }

        blocks = new Block[WORLD_SIZE_Y * WORLD_SIZE_X];

        //        assetManager = new AssetManager();
        //        TextureAtlas m_blockAtlas = assetManager.get("data/", TextureAtlas.class);
        //        assetManager.finishLoading();

        m_camera = new OrthographicCamera(1600 / OreWorld.PIXELS_PER_METER,
                                          900 / OreWorld.PIXELS_PER_METER);//30, 30 * (h / w));
        m_camera.setToOrtho(true, 1600 / OreWorld.PIXELS_PER_METER, 900 / OreWorld.PIXELS_PER_METER);

        //        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);

        assert isClient() ^ isServer();
        if (isClient()) {
            m_atlas = new TextureAtlas(Gdx.files.internal("packed/entities.atlas"));
            initializeWorld();

            m_blockPickingCrosshairEntity = m_artemisWorld.create();
            m_artemisWorld.getSystem(TagManager.class).register("crosshair", m_blockPickingCrosshairEntity);

            SpriteComponent spriteComponent = spriteMapper.create(m_blockPickingCrosshairEntity);
            spriteComponent.sprite.setSize(BLOCK_SIZE, BLOCK_SIZE);
            spriteComponent.sprite.setRegion(m_atlas.findRegion("crosshair-blockpicking"));
            spriteComponent.noClip = true;

            engine.addSystem(m_tileRenderer = new TileRenderer(m_camera, this, 1f / 60f));
        }

        if (isServer()) {
            generateWorld();
        }
    }

    //TODO cleanup, can be broken down into various handlers, early returns and handling multiple cases make it
    // convoluted
    protected void clientHotbarInventoryItemSelected() {
        assert !isServer();

        PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);
        int equippedEntity = playerComponent.getEquippedPrimaryItem();

        //if it is here, remove it...we respawn the placement overlay further down either way.
        if (m_itemPlacementOverlayEntity != ENTITY_INVALID) {
            m_artemisWorld.delete(m_itemPlacementOverlayEntity);
            m_itemPlacementOverlayEntity = ENTITY_INVALID;
        }

        if (equippedEntity == ENTITY_INVALID) {
            return;
        }

        SpriteComponent crosshairSprite = spriteMapper.get(m_blockPickingCrosshairEntity);
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
                //HACK: return;
            }
        }

        //this item is placeable, show an overlay of it so we can see where we're going to place it (by cloning its
        // entity)
        m_itemPlacementOverlayEntity = cloneEntity(equippedEntity);
        ItemComponent itemComponent = itemMapper.get(m_itemPlacementOverlayEntity);
        //transition to the in world state, since the cloned source item was in the inventory state, so to would this
        itemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = spriteMapper.get(m_itemPlacementOverlayEntity);
        spriteComponent.noClip = true;

        //crosshair shoudln't be visible if the power overlay is
        if (m_powerOverlaySystem.overlayVisible) {
            spriteComponent.visible = false;
        }

        m_artemisWorld.getSystem(TagManager.class).register("itemPlacementOverlay", m_itemPlacementOverlayEntity);

    }

    public void initServer() {
    }

    public void initClient(int mainPlayer) {
        m_mainPlayerEntity = mainPlayer;
        //        velocityMapper.get(m_mainPlayerEntity);

        engine.addSystem(new SpriteRenderSystem(this));
        engine.addSystem(m_powerOverlaySystem = new PowerOverlayRenderSystem(this));

        SpriteComponent playerSprite = spriteMapper.get(m_mainPlayerEntity);
        playerSprite.sprite.setRegion(m_atlas.findRegion("player-32x64"));
        playerSprite.sprite.flip(false, true);
    }

    /**
     * adding entity to the world is callers responsibility
     *
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
        //hack fixme, should be consolidated w/ sprite's noclip...or should it?? make mention, is sprite present on
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
        transitionTiles();

        counter.stop();
        String s = String.format("total world gen took (incl transitioning, etc): %s seconds", counter.current);
        Gdx.app.log("", s);

    }

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

    private void transitionTiles() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {
                int index = x * WORLD_SIZE_Y + y;

                if (blocks[index].type == Block.BlockType.NullBlockType) {
                    continue;
                }

                if (blocks[index].type == Block.BlockType.DirtBlockType) {
                    //fixme may be able to be made generic. MAYBE.
                    transitionDirtTile(x, y);
                } else if (blocks[index].type == Block.BlockType.StoneBlockType) {
                    transitionStoneTile(x, y);
                }
            }
        }
    }

    private void transitionStoneTile(int x, int y) {
        int index = x * WORLD_SIZE_Y + y;
        //essentially, if the *other* tiles in question are the same blocks, we should
        //merge/transition with them.

        Set<Transitions> result = EnumSet.noneOf(Transitions.class);

        boolean leftMerge = shouldTileTransitionWith(x, y, x - 1, y);
        boolean rightMerge = shouldTileTransitionWith(x, y, x + 1, y);
        boolean topMerge = shouldTileTransitionWith(x, y, x, y - 1);
        boolean bottomMerge = shouldTileTransitionWith(x, y, x, y + 1);

        if (leftMerge) {
            result.add(Transitions.left);
        }

        if (rightMerge) {
            result.add(Transitions.right);
        }

        if (topMerge) {
            result.add(Transitions.top);
        }

        if (bottomMerge) {
            result.add(Transitions.bottom);
        }

        Integer lookup = stoneTransitionTypes.get(result);
        assert lookup != null : "transition lookup failure!";
        blocks[index].meshType = (byte) lookup.intValue();
    }

    private void transitionDirtTile(int x, int y) {
        int index = x * WORLD_SIZE_Y + y;
        //essentially, if the *other* tiles in question are the same blocks, we should
        //merge/transition with them.

        Set<Transitions> result = EnumSet.noneOf(Transitions.class);

        boolean leftMerge = shouldTileTransitionWith(x, y, x - 1, y);
        boolean rightMerge = shouldTileTransitionWith(x, y, x + 1, y);
        boolean topMerge = shouldTileTransitionWith(x, y, x, y - 1);
        boolean bottomMerge = shouldTileTransitionWith(x, y, x, y + 1);

        if (leftMerge) {
            result.add(Transitions.left);
        }

        if (rightMerge) {
            result.add(Transitions.right);
        }

        if (topMerge) {
            result.add(Transitions.top);
        }

        if (bottomMerge) {
            result.add(Transitions.bottom);
        }

        Integer lookup = dirtTransitionTypes.get(result);
        assert lookup != null : "transition lookup failure!";
        blocks[index].meshType = (byte) lookup.intValue();
    }

    /**
     * if given tile should transition with the neighbor tile. Usually indicated by if they are the same type or not.
     * (if they are, it's a yes. If they're different, no)
     *
     * @param sourceTileX
     * @param sourceTileY
     * @param nearbyTileX
     * @param nearbyTileY
     *
     * @return
     */
    private boolean shouldTileTransitionWith(int sourceTileX, int sourceTileY, int nearbyTileX, int nearbyTileY) {
        boolean isMatched = false;
        int srcIndex = MathUtils.clamp(sourceTileX * WORLD_SIZE_Y + sourceTileY, 0, WORLD_SIZE_Y * WORLD_SIZE_X - 1);
        int nearbyIndex = MathUtils.clamp(nearbyTileX * WORLD_SIZE_Y + nearbyTileY, 0, WORLD_SIZE_Y * WORLD_SIZE_X - 1);

        if (blocks[srcIndex].type == blocks[nearbyIndex].type) {
            //todo in the future look up if it blends or not based on various thingies. not jsut "is tile same"
            //some may be exceptions??
            isMatched = true;
        }

        return isMatched;
    }

    private void initializeWorld() {
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
                        //hack, simulate only dirt for now. blocks[index].type = Block.BlockType.StoneBlockType;
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
        assert x >= 0 && y >= 0 && x <= WORLD_SIZE_X && y <= WORLD_SIZE_Y : "block index out of range";

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

    public void dispose() {
    }

    public void zoom(float factor) {

        m_camera.zoom *= factor;
    }

    public void update(double elapsed) {
        if (isClient()) {
            if (m_mainPlayerEntity == ENTITY_INVALID) {
                return;
            }

            //        playerSprite.sprite.setOriginCenter();

            //        m_camera.position.set(playerSprite.sprite.getX() + playerSprite.sprite.getWidth() * 0.5f,
            // playerSprite
            // .sprite.getY() + playerSprite.sprite.getHeight() * 0.5f, 0);

            final float zoomAmount = 0.004f;
            if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
                if (m_zoomTimer.milliseconds() >= zoomInterval) {
                    //zoom out
                    zoom(1.0f + zoomAmount);
                    m_zoomTimer.reset();
                }
            }

            if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
                if (m_zoomTimer.milliseconds() >= zoomInterval) {
                    zoom(1.0f - zoomAmount);
                    m_zoomTimer.reset();
                }
            }

            updateCrosshair();
            updateItemPlacementOverlay();

            if (m_client.leftMouseDown && !m_powerOverlaySystem.overlayVisible) {
                handleLeftMousePrimaryAttack();
            }

            //hack
            if (m_itemPlacementOverlayEntity != ENTITY_INVALID) {
                SpriteComponent component = spriteMapper.get(m_itemPlacementOverlayEntity);
                assert component != null : "how the hell does it have no spritecomp?!!";

                assert component.noClip : "placement overlay found to not be in noclip mode!!!";
            }
        }

        if (isServer()) {

            //            if (randomGrassTimer.milliseconds() > 500) {
            //HACK
            randomGrowGrass();
            randomGrassTimer.reset();
            //           }
        }

        m_artemisWorld.setDelta((float) elapsed);
        m_artemisWorld.process();
    }

    private void handleLeftMousePrimaryAttack() {
        Vector2 mouse = mousePositionWorldCoords();

        PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);
        int itemEntity = playerComponent.getEquippedPrimaryItem();
        if (itemEntity == ENTITY_INVALID) {
            return;
        }

        ToolComponent toolComponent = toolMapper.getSafe(itemEntity);
        if (toolComponent != null) {
            if (toolComponent.type != ToolComponent.ToolType.Drill) {
                return;
            }

            int x = (int) (mouse.x / BLOCK_SIZE);
            int y = (int) (mouse.y / BLOCK_SIZE);

            Block block = blockAt(x, y);

            if (block.type != Block.BlockType.NullBlockType) {
                block.destroy();
                m_client.sendBlockPick(x, y);
            }

            //action performed
            return;
        }

        BlockComponent blockComponent = blockMapper.getSafe(itemEntity);
        if (blockComponent != null) {

            int x = (int) (mouse.x / BLOCK_SIZE);
            int y = (int) (mouse.y / BLOCK_SIZE);

            boolean blockPlaced = attemptBlockPlacement(x, y, blockComponent.blockType);
            if (blockPlaced) {
                m_client.sendBlockPlace(x, y);
            }

            return;
        }

        ItemComponent itemComponent = itemMapper.getSafe(itemEntity);
        if (itemComponent != null) {
            if (playerComponent.placeableItemTimer.milliseconds() > PlayerComponent.placeableItemDelay) {
                playerComponent.placeableItemTimer.reset();

                attemptItemPlace(mouse.x, mouse.y, playerComponent.getEquippedPrimaryItem());
            }
        }
    }

    /**
     * Attempts to place a block at position with the type, can fail. If it succeeds it will *not*
     * notify anything (network wise). Takes care of destroying e.g. nearby grass
     *
     * @param x
     * @param y
     * @param placedBlockType
     *         block type to change it to
     *
     * @return true if placement succeeded.
     */
    boolean attemptBlockPlacement(int x, int y, byte placedBlockType) {
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

    private void attemptItemPlace(float x, float y, int itemEntity) {

        //place the item
        int placedItemEntity = cloneEntity(itemEntity);

        ItemComponent placedItemComponent = itemMapper.get(placedItemEntity);

        placedItemComponent.state = ItemComponent.State.InWorldState;

        Vector2 alignedPosition = new Vector2(x, y);
        SpriteComponent spriteComponent = spriteMapper.get(placedItemEntity);
        alignPositionToBlocks(alignedPosition);

        spriteComponent.sprite.setPosition(alignedPosition.x, alignedPosition.y);

        if (isPlacementValid(placedItemEntity)) {
            //hack, do more validation..
            m_client.sendItemPlace(alignedPosition.x, alignedPosition.y);
        } else {
            //fixme i know, it isn't ideal..i technically add the item anyways and delete it if it cannot be placed
            //because the function actually takes only the entity, to check if its size, position etc conflict with
            // anything

            //engine.removeEntity(placedItemEntity);
        }
    }

    private OreTimer tileRecomputeTimer = new OreTimer();
    private OreTimer randomGrassTimer = new OreTimer();

    public void render(double elapsed) {
        if (m_mainPlayerEntity == ENTITY_INVALID) {
            return;
        }

        if (tileRecomputeTimer.milliseconds() > 60) {
            transitionTiles();
            transitionGrass();

            tileRecomputeTimer.reset();
        }

        //        m_camera.zoom *= 0.9;
        //m_lightRenderer->renderToFBO();

        //FIXME: incorporate entities into the pre-lit gamescene FBO, then render lighting as last pass
        //m_lightRenderer->renderToBackbuffer();

        //FIXME: take lighting into account, needs access to fbos though.
        //   m_fluidRenderer->render();
        //    m_particleRenderer->render();
        //FIXME unused    m_quadTreeRenderer->render();
    }

    private void randomGrowGrass() {
        for (int i = 0; i < m_players.size; ++i) {
            int playerEntity = m_players.get(i);

            PlayerComponent playerComponent = playerMapper.get(playerEntity);

            LoadedViewport.PlayerViewportBlockRegion region = playerComponent.loadedViewport.blockRegionInViewport();

            //each tick, resample 100 or so blocks to see if grass can grow. this may need to be
            //reduced, but for debugging right now it's good.
            for (int j = 0; j < 1000; ++j) {
                int randomX = MathUtils.random(region.x, region.width);
                int randomY = MathUtils.random(region.y, region.height);

                Block block = blockAt(randomX, randomY);

                //pick a random block, if it has grass, try to grow outward along its edges/spread the grass
                if (block.hasFlag(Block.BlockFlags.GrassBlock)) {
                    int leftBlockX = blockXSafe(randomX - 1);
                    int leftBlockY = blockYSafe(randomY);

                    int rightBlockX = blockXSafe(randomX + 1);
                    int rightBlockY = blockYSafe(randomY);

                    int topBlockX = blockXSafe(randomX);
                    int topBlockY = blockYSafe(randomY - 1);

                    int bottomBlockX = blockXSafe(randomX);
                    int bottomBlockY = blockYSafe(randomY + 1);

                    int topLeftBlockX = blockXSafe(randomX - 1);
                    int topLeftBlockY = blockYSafe(randomY - 1);

                    int topRightBlockX = blockXSafe(randomX + 1);
                    int topRightBlockY = blockYSafe(randomY - 1);

                    int bottomRightBlockX = blockXSafe(randomX + 1);
                    int bottomRightBlockY = blockYSafe(randomY + 1);

                    int bottomLeftBlockX = blockXSafe(randomX - 1);
                    int bottomLeftBlockY = blockYSafe(randomY + 1);

                    Block leftBlock = blockAt(leftBlockX, leftBlockY);
                    Block rightBlock = blockAt(rightBlockX, rightBlockY);
                    Block topBlock = blockAt(topBlockX, topBlockY);
                    Block bottomBlock = blockAt(bottomBlockX, bottomBlockY);
                    Block topLeftBlock = blockAt(topLeftBlockX, topLeftBlockY);
                    Block topRightBlock = blockAt(topRightBlockX, topRightBlockY);
                    Block bottomLeftBlock = blockAt(bottomLeftBlockX, bottomLeftBlockY);
                    Block bottomRightBlock = blockAt(bottomRightBlockX, bottomRightBlockY);

                    //grow left
                    if (leftBlock.type == Block.BlockType.DirtBlockType &&
                        !leftBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        int leftLeftX = blockXSafe(leftBlockX - 1);
                        int leftLeftY = leftBlockY;
                        Block leftLeftBlock = blockAt(leftLeftX, leftLeftY);

                        if (leftLeftBlock.type == Block.BlockType.NullBlockType ||
                            topLeftBlock.type == Block.BlockType.NullBlockType ||
                            bottomLeftBlock.type == Block.BlockType.NullBlockType ||
                            (bottomLeftBlock.type == Block.BlockType.DirtBlockType &&
                             (bottomBlock.type == Block.BlockType.NullBlockType)) ||
                            (topLeftBlock.type == Block.BlockType.DirtBlockType &&
                             topBlock.type == Block.BlockType.NullBlockType)) {

                            leftBlock.setFlag(Block.BlockFlags.GrassBlock);
                            //                            m_server.sendPlayerSparseBlock(player, leftLeftBlock,
                            // leftLeftX, leftLeftY);
                            m_server.sendPlayerSparseBlock(playerEntity, leftBlock, leftBlockX, leftBlockY);
                        }
                    }

                    //grow right
                    if (rightBlock.type == Block.BlockType.DirtBlockType &&
                        !rightBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        int rightRightX = blockXSafe(rightBlockX + 1);
                        int rightRightY = rightBlockY;
                        Block rightRightBlock = blockAt(rightRightX, rightRightY);

                        if (rightRightBlock.type == Block.BlockType.NullBlockType ||
                            topRightBlock.type == Block.BlockType.NullBlockType ||
                            bottomRightBlock.type == Block.BlockType.NullBlockType ||
                            (bottomRightBlock.type == Block.BlockType.DirtBlockType &&
                             (bottomBlock.type == Block.BlockType.NullBlockType)) ||
                            (topRightBlock.type == Block.BlockType.DirtBlockType &&
                             topBlock.type == Block.BlockType.NullBlockType)) {

                            rightBlock.setFlag(Block.BlockFlags.GrassBlock);
                            //    m_server.sendPlayerSparseBlock(player, topRightBlock, topRightX, topRightY);
                            //                               m_server.sendPlayerSparseBlock(player,
                            // rightRightBlock, rightRightX, rightRightY);
                            m_server.sendPlayerSparseBlock(playerEntity, rightBlock, rightBlockX, rightBlockY);
                        }
                    }

                    //grow down
                    if (bottomBlock.type == Block.BlockType.DirtBlockType &&
                        !bottomBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        //only spread grass to the lower block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (bottomLeftBlock.type == Block.BlockType.NullBlockType ||
                            bottomRightBlock.type == Block.BlockType.NullBlockType ||
                            leftBlock.type == Block.BlockType.NullBlockType ||
                            rightBlock.type == Block.BlockType.NullBlockType) {

                            bottomBlock.setFlag(Block.BlockFlags.GrassBlock);

                            m_server.sendPlayerSparseBlock(playerEntity, bottomBlock, bottomBlockX, bottomBlockY);
                        }
                    }

                    //grow up
                    if (topBlock.type == Block.BlockType.DirtBlockType &&
                        !topBlock.hasFlag(Block.BlockFlags.GrassBlock)) {

                        //only spread grass to the upper block, if that block has open space left, right, or
                        //top left, etc. (from our perspective..the block with grass, it is our right block that
                        //we are checking for empty)
                        if (topLeftBlock.type == Block.BlockType.NullBlockType ||
                            topRightBlock.type == Block.BlockType.NullBlockType ||
                            leftBlock.type == Block.BlockType.NullBlockType ||
                            rightBlock.type == Block.BlockType.NullBlockType) {

                            topBlock.setFlag(Block.BlockFlags.GrassBlock);

                            m_server.sendPlayerSparseBlock(playerEntity, topBlock, topBlockX, topBlockY);
                        }
                    }

                    //grow top-right
                    if (topRightBlock.type == Block.BlockType.DirtBlockType) {
                        //hack                        int topRightTopRightX = blockXSafe(topRightBlockX + 1);
                        //hack                        int topRightTopRightY = blockYSafe(topRightBlockY + 1);

                        //                        Block topRightTopRightBlock = blockAt(topRightTopRightX,
                        // topRightTopRightY);

                    }
                }
            }
        }
    }

    private void transitionGrass() {
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                Block leftLeftBlock = blockAtSafely(x - 2, y);
                Block rightRightBlock = blockAtSafely(x + 2, y);
                Block leftBlock = blockAtSafely(x - 1, y);
                Block rightBlock = blockAtSafely(x + 1, y);
                Block topBlock = blockAtSafely(x, y - 1);
                Block bottomBlock = blockAtSafely(x, y + 1);

                Block topLeftBlock = blockAtSafely(x - 1, y - 1);
                Block topRightBlock = blockAtSafely(x + 1, y - 1);
                Block bottomLeftBlock = blockAtSafely(x - 1, y + 1);
                Block bottomRightBlock = blockAtSafely(x + 1, y + 1);

                Block block = blockAtSafely(x, y);
                if (block.type == Block.BlockType.DirtBlockType && block.hasFlag(Block.BlockFlags.GrassBlock)) {

                    //should have grass on left side of this block..or not.
                    boolean leftEmpty = leftBlock.type == Block.BlockType.NullBlockType;
                    boolean leftLeftEmpty = leftLeftBlock.type == Block.BlockType.NullBlockType;

                    boolean rightEmpty = rightBlock.type == Block.BlockType.NullBlockType;
                    boolean rightRightEmpty = rightRightBlock.type == Block.BlockType.NullBlockType;

                    boolean topEmpty = topBlock.type == Block.BlockType.NullBlockType;

                    boolean bottomEmpty = bottomBlock.type == Block.BlockType.NullBlockType;

                    //if block to the left is dirt..
                    boolean leftDirt = leftBlock.type == Block.BlockType.DirtBlockType;
                    boolean rightDirt = rightBlock.type == Block.BlockType.DirtBlockType;
                    boolean topDirt = topBlock.type == Block.BlockType.DirtBlockType;
                    boolean bottomDirt = bottomBlock.type == Block.BlockType.DirtBlockType;

                    //handled a bit differently,
                    boolean topLeftEmpty = topLeftBlock.type == Block.BlockType.NullBlockType;
                    boolean topRightEmpty = topRightBlock.type == Block.BlockType.NullBlockType;
                    boolean bottomLeftEmpty = bottomLeftBlock.type == Block.BlockType.NullBlockType;
                    boolean bottomRightEmpty = bottomRightBlock.type == Block.BlockType.NullBlockType;

                    boolean leftOre = blockTypes.get(leftBlock.type).category == BlockStruct.BlockCategory.Ore;

                    byte finalMesh = -1;

                    if (leftDirt && rightDirt && topDirt && bottomDirt && topLeftEmpty && topRightEmpty &&
                        bottomLeftEmpty && bottomRightEmpty) {
                        finalMesh = 0;
                    } else if (leftEmpty && topEmpty && rightDirt && bottomDirt && !bottomRightEmpty) {
                        finalMesh = 1;
                    } else if (leftDirt && topEmpty && rightDirt && bottomDirt &&
                               !(bottomLeftEmpty && bottomRightEmpty)) { //hack this is supsiciously different
                        finalMesh = 2;
                    } else if (leftDirt && bottomDirt && rightEmpty && topEmpty) { // hack check leftleftempty etc?
                        finalMesh = 3;
                    } else if (topDirt && rightDirt && bottomDirt && leftEmpty) {
                        finalMesh = 4;
                    } else if (leftDirt && topDirt && bottomDirt && rightEmpty) {
                        finalMesh = 5;
                    } else if (topDirt && rightDirt && leftEmpty && bottomEmpty && !topRightEmpty) {
                        finalMesh = 6;
                    } else if (topDirt && leftDirt && rightDirt && bottomEmpty) {
                        finalMesh = 7;
                    } else if (leftDirt && topDirt && rightEmpty && bottomEmpty && !topLeftEmpty) {
                        finalMesh = 8;
                    } else if (leftEmpty && topEmpty && rightEmpty && bottomDirt) {
                        finalMesh = 9;
                    } else if (leftEmpty && rightEmpty && topDirt && bottomDirt) {
                        finalMesh = 10;
                    } else if (leftEmpty && topEmpty && bottomEmpty && rightDirt) {
                        finalMesh = 11;
                    } else if (leftDirt && rightDirt && topEmpty && bottomEmpty) {
                        finalMesh = 12;
                    } else if (leftDirt && topEmpty && bottomEmpty && rightEmpty) {
                        finalMesh = 13;
                    } else if (leftEmpty && rightEmpty && bottomEmpty && topDirt) {
                        finalMesh = 14;
                    } else if (leftEmpty && rightEmpty && topEmpty && bottomEmpty) {
                        finalMesh = 15;
                    } else if (leftDirt && topDirt && rightDirt && bottomDirt && topLeftEmpty) {
                        finalMesh = 16;
                    } else if (leftDirt && topDirt && bottomDirt && rightDirt && topRightEmpty) {
                        finalMesh = 17;
                    } else if (leftDirt && bottomDirt && topDirt && bottomLeftEmpty &&
                               !topLeftEmpty) { //hack ADD TOP BOTTOM ETC
                        finalMesh = 18;
                    } else if (rightDirt && bottomDirt && topDirt && leftDirt && bottomRightEmpty) {
                        finalMesh = 19;
                    } else if (leftDirt && rightDirt && topDirt && topLeftEmpty && topRightEmpty) {
                        finalMesh = 20;
                    } else if (topDirt && bottomDirt && leftDirt && topLeftEmpty && bottomLeftEmpty) {
                        finalMesh = 21;
                    } else if (topDirt && bottomDirt && rightDirt && topRightEmpty && bottomRightEmpty) {
                        finalMesh = 22;
                    } else if (leftDirt && rightDirt && topDirt && bottomLeftEmpty && bottomRightEmpty) {
                        finalMesh = 23;
                    } else if (topDirt && rightDirt && bottomDirt && topRightEmpty && bottomRightEmpty &&
                               leftEmpty) { //hack
                        finalMesh = 24;
                    } else if (leftDirt && topDirt && bottomDirt && topLeftEmpty && bottomLeftEmpty && rightEmpty) {
                        finalMesh = 25;
                    } else if (leftDirt && rightDirt && topDirt && topLeftEmpty && topRightEmpty && bottomEmpty) {
                        finalMesh = 26;
                    } else if (leftDirt && rightDirt && bottomDirt && topEmpty && bottomLeftEmpty && bottomRightEmpty) {
                        finalMesh = 27;
                    } else if (leftDirt && topDirt && topLeftEmpty && rightEmpty && bottomEmpty) {
                        finalMesh = 28;
                    } else if (topDirt && rightDirt && topRightEmpty && leftEmpty && bottomEmpty) {
                        finalMesh = 29;
                    } else if (leftDirt && bottomDirt && bottomRightEmpty && rightEmpty && topEmpty) {
                        finalMesh = 30;
                    } else if (rightDirt && bottomDirt && bottomRightEmpty && leftEmpty && topEmpty) {
                        finalMesh = 31;
                    } else {
                        //failure
                        finalMesh = 15;
                    }

                    block.meshType = finalMesh;

                    if (finalMesh == -1) {
                        assert false : "invalid mesh type retrieval, for some reason";
                    }
                }
            }
        }
    }

    /*
    private boolean shouldGrassMesh(int sourceTileX, int sourceTileY, int nearbyTileX, int nearbyTileY) {
        boolean isMatched = false;
        int srcIndex = MathUtils.clamp(sourceTileX * WORLD_SIZE_Y + sourceTileY, 0, WORLD_SIZE_Y * WORLD_SIZE_X - 1);
        int nearbyIndex = MathUtils.clamp(nearbyTileX * WORLD_SIZE_Y + nearbyTileY, 0, WORLD_SIZE_Y * WORLD_SIZE_X - 1);

        if (blocks[srcIndex].type == blocks[nearbyIndex].type) {
            //todo in the future look up if it blends or not based on various thingies. not jsut "is tile same"
            //some may be exceptions??
            isMatched = true;
        }

        return isMatched;
    }
    */

    private void updateCrosshair() {
        //PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);
        //playerComponent

        SpriteComponent spriteComponent = spriteMapper.get(m_blockPickingCrosshairEntity);

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

    private void updateItemPlacementOverlay() {
        if (m_itemPlacementOverlayEntity == ENTITY_INVALID) {
            return;
        }

        Vector2 mouse = mousePositionWorldCoords();
        alignPositionToBlocks(mouse);

        SpriteComponent spriteComponent = spriteMapper.get(m_itemPlacementOverlayEntity);
        spriteComponent.sprite.setPosition(mouse.x, mouse.y);
        spriteComponent.placementValid = isPlacementValid(m_itemPlacementOverlayEntity);
    }

    private void alignPositionToBlocks(Vector2 pos) {
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
    private boolean isPlacementValid(int entity) {
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
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(SpriteComponent.class).get());
        for (int i = 0; i < entities.size(); ++i) {
            //it's the item we're trying to place, don't count a collision with ourselves
            if (entities.get(i) == entity) {
                continue;
            }

            //ignore players, aka don't count them as colliding when placing static objects.
            //        if (e.has_component<PlayerComponent>()) {
            //            continue;
            //        }

            ItemComponent itemComponent = itemMapper.get(entities.get(i));
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

    private boolean entityCollides(Entity first, Entity second) {
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
        assert playerMapper.get(entity) == null;

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
     * @param playerIdWhoDropped
     *         the playerid of the player
     *
     * @return the player entity
     */
    public int playerForID(int playerIdWhoDropped) {
        assert !isClient();
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(PlayerComponent.class).get());
        PlayerComponent playerComponent;
        for (int i = 0; i < entities.size(); ++i) {
            playerComponent = playerMapper.get(entities.get(i));
            if (playerComponent.connectionId == playerIdWhoDropped) {
                return entities.get(i);
            }
        }

        throw new IllegalStateException("player id attempted to be obtained from item, but this player does not exist");
    }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) {
            return false;
        }

        if (m_powerOverlaySystem.overlayVisible) {
            m_powerOverlaySystem.leftMouseClicked();
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

        if (m_powerOverlaySystem.overlayVisible) {
            m_powerOverlaySystem.leftMouseReleased();
            return true;
        } else {
        }

        return false;
    }

    public static class BlockStruct {
        public String textureName; //e.g. "dirt", "stone", etc.
        Collision collision;
        BlockCategory category;

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
