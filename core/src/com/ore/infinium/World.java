package com.ore.infinium;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                        *
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
public class World implements Disposable {
    public static final float PIXELS_PER_METER = 50.0f;
    public static final float GRAVITY_ACCEL = 9.8f / PIXELS_PER_METER / 3.0f;
    public static final float GRAVITY_ACCEL_CLAMP = 9.8f / PIXELS_PER_METER / 3.0f;

    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;

    public static final int WORLD_COLUMNCOUNT = 1000; //2400
    public static final int WORLD_ROWCOUNT = 1000; //8400
    public static final int WORLD_SEA_LEVEL = 50;

    /**
     * looks up the texture prefix name for each block type.
     * e.g. DirtBlockType -> "dirt", etc.
     */
    public static final HashMap<Byte, BlockStruct> blockTypes = new HashMap<>();

    static {
        blockTypes.put(Block.BlockType.NullBlockType, new BlockStruct("", false));
        blockTypes.put(Block.BlockType.DirtBlockType, new BlockStruct("dirt", true));
        blockTypes.put(Block.BlockType.StoneBlockType, new BlockStruct("stone", true));
    }

    /**
     * @first bitmask of all sides, that maps to valid transition types
     * e.g. left | right, indicates that it needs to mesh on the left and right sides ONLY
     * @second
     */
    public static final HashMap<EnumSet<TileTransitions>, Integer> dirtTransitionTypes = new HashMap<>();
    public static final HashMap<EnumSet<TileTransitions>, Integer> grassTransitionTypes = new HashMap<>();
    public static final HashMap<EnumSet<TileTransitions>, Integer> stoneTransitionTypes = new HashMap<>();

    private enum TileTransitions {
        left,
        right,
        top,
        bottom,
        //
        LeftDirt,
        RightDirt,
        TopDirt,
        BottomDirt
    }

    static {
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.top, TileTransitions.bottom), 0);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.bottom), 1);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.top, TileTransitions.bottom), 2);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.right), 3);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right), 4);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left), 5);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.top), 6);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.bottom), 7);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.bottom), 8);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.bottom), 9);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.top, TileTransitions.bottom), 10);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.top, TileTransitions.bottom), 11);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.top), 12);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.top), 13);
        dirtTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.top), 14);
        dirtTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 15);

        ///////////////////////////////////////////////////////////////////////////////////

        grassTransitionTypes.put(EnumSet.of(TileTransitions.bottom), 0);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.top, TileTransitions.bottom), 1);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right), 2);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right), 3);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.top, TileTransitions.bottom), 4);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left), 5);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.bottom), 6);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.bottom), 7);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.bottom), 8);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.top), 9);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.top), 10);
        grassTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 11);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.top, TileTransitions.bottom), 12);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.top), 13);

        //rightbottomtop unhandled ??? NEEDED??
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right, TileTransitions.top, TileTransitions.bottom), 9);
        //lefttopbottom
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.top, TileTransitions.bottom), 10);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.right, TileTransitions.top), 4);
        //hack ^^

        //below here is junk
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left, TileTransitions.top), 14);
//        grassTransitionTypes.put(TileTransitions.none, 11);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.top), 4); //hack
        grassTransitionTypes.put(EnumSet.of(TileTransitions.right), 16);
        grassTransitionTypes.put(EnumSet.of(TileTransitions.left), 17);

        ////////////////////


        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 1);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 2);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 3);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 4);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 5);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 6);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 7);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 8);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 9);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 10);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 11);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 12);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 13);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 14);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 15);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 16);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 17);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 18);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 19);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 20);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 21);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 22);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 23);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 24);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 25);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 26);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 27);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 28);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 29);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 30);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 31);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 32);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 33);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 34);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 35);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 36);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 37);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 38);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 39);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 40);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 41);
        stoneTransitionTypes.put(EnumSet.noneOf(TileTransitions.class), 42);

    }

    private static final int zoomInterval = 50; //ms
    private static OreTimer m_zoomTimer = new OreTimer();

    public Block[] blocks;

    public PooledEngine engine;
    public Array<Entity> m_players = new Array<>();
    public Entity m_mainPlayer;
    public OreServer m_server;
    public AssetManager assetManager;
    public OreClient m_client;
    public OrthographicCamera m_camera;

    //fixme remove in favor of the render system
    public TextureAtlas m_atlas;
    protected TileRenderer m_tileRenderer;
    PowerOverlayRenderSystem m_powerOverlaySystem;
    public PowerCircuitSystem m_powerCircuitSystem;

    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ControllableComponent> controlMapper = ComponentMapper.getFor(ControllableComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<JumpComponent> jumpMapper = ComponentMapper.getFor(JumpComponent.class);
    private ComponentMapper<BlockComponent> blockMapper = ComponentMapper.getFor(BlockComponent.class);
    private ComponentMapper<AirGeneratorComponent> airGeneratorMapper = ComponentMapper.getFor(AirGeneratorComponent.class);
    private ComponentMapper<ToolComponent> toolMapper = ComponentMapper.getFor(ToolComponent.class);
    private ComponentMapper<AirComponent> airMapper = ComponentMapper.getFor(AirComponent.class);
    private ComponentMapper<TagComponent> tagMapper = ComponentMapper.getFor(TagComponent.class);
    private ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
    private ComponentMapper<TorchComponent> torchMapper = ComponentMapper.getFor(TorchComponent.class);
    private ComponentMapper<PowerDeviceComponent> powerMapper = ComponentMapper.getFor(PowerDeviceComponent.class);

    private boolean m_noClipEnabled;
    private Entity m_blockPickingCrosshair;
    Entity m_itemPlacementGhost;

    private com.artemis.World artemisWorld;

    public World(OreClient client, OreServer server) {
        m_client = client;
        m_server = server;

        if (isClient()) {
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
        }

        blocks = new Block[WORLD_ROWCOUNT * WORLD_COLUMNCOUNT];

//        assetManager = new AssetManager();
//        TextureAtlas m_blockAtlas = assetManager.get("data/", TextureAtlas.class);
//        assetManager.finishLoading();
        engine = new PooledEngine(2000, 2000, 2000, 2000);

        engine.addSystem(new MovementSystem(this));
        engine.addSystem(m_powerCircuitSystem = new PowerCircuitSystem(this));
        engine.addSystem(new PlayerSystem(this));

        m_camera = new OrthographicCamera(1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);//30, 30 * (h / w));
        m_camera.setToOrtho(true, 1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);

//        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);

        assert isClient() ^ isServer();
        if (isClient()) {
            m_atlas = new TextureAtlas(Gdx.files.internal("packed/entities.atlas"));
            initializeWorld();

            m_blockPickingCrosshair = engine.createEntity();
            TagComponent tagComponent = engine.createComponent(TagComponent.class);
            tagComponent.tag = "crosshair";
            m_blockPickingCrosshair.add(tagComponent);

            SpriteComponent spriteComponent = engine.createComponent(SpriteComponent.class);
            m_blockPickingCrosshair.add(spriteComponent);
            spriteComponent.sprite.setSize(BLOCK_SIZE, BLOCK_SIZE);
            spriteComponent.sprite.setRegion(m_atlas.findRegion("crosshair-blockpicking"));

            engine.addSystem(m_tileRenderer = new TileRenderer(m_camera, this, 1f / 60f));
        }

        if (isServer()) {
            generateWorld();
        }
    }

    protected void clientHotbarInventoryItemSelected() {
        assert !isServer();

        PlayerComponent playerComponent = playerMapper.get(m_mainPlayer);
        Entity entity = playerComponent.equippedPrimaryItem();

        if (m_itemPlacementGhost != null) {
            engine.removeEntity(m_itemPlacementGhost);
        }

        if (entity == null) {
            return;
        }

        //don't show the placement for block
        if (blockMapper.get(entity) != null) {
            return;
        }

        //this item is placeable, show a ghost of it so we can see where we're going to place it
        m_itemPlacementGhost = cloneEntity(entity);
        ItemComponent itemComponent = itemMapper.get(m_itemPlacementGhost);
        itemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = spriteMapper.get(m_itemPlacementGhost);

        if (m_powerOverlaySystem.overlayVisible) {
            spriteComponent.visible = false;
        }

        TagComponent tag = engine.createComponent(TagComponent.class);
        tag.tag = "itemPlacementGhost";
        m_itemPlacementGhost.add(tag);
        engine.addEntity(m_itemPlacementGhost);

    }

    public void initServer() {
    }

    public void initClient(Entity mainPlayer) {
        m_mainPlayer = mainPlayer;
//        velocityMapper.get(m_mainPlayer);

        engine.addSystem(new SpriteRenderSystem(this));
        engine.addSystem(m_powerOverlaySystem = new PowerOverlayRenderSystem(this));

        SpriteComponent playerSprite = spriteMapper.get(m_mainPlayer);
        playerSprite.sprite.setRegion(m_atlas.findRegion("player-32x64"));
        playerSprite.sprite.flip(false, true);
    }

    /**
     * adding entity to the world is callers responsibility
     *
     * @param playerName
     * @param connectionId
     * @return
     */
    public Entity createPlayer(String playerName, int connectionId) {
        Entity player = engine.createEntity();
        SpriteComponent playerSprite = engine.createComponent(SpriteComponent.class);
        player.add(playerSprite);

        player.add(engine.createComponent(VelocityComponent.class));
        PlayerComponent playerComponent = engine.createComponent(PlayerComponent.class);
        playerComponent.connectionId = connectionId;
        playerComponent.noClip = m_noClipEnabled;

        playerComponent.playerName = playerName;
        playerComponent.loadedViewport.setRect(new Rectangle(0, 0, LoadedViewport.MAX_VIEWPORT_WIDTH, LoadedViewport.MAX_VIEWPORT_HEIGHT));
        playerComponent.loadedViewport.centerOn(new Vector2(playerSprite.sprite.getX(), playerSprite.sprite.getY()));
        player.add(playerComponent);

        playerSprite.sprite.setSize(World.BLOCK_SIZE * 2, World.BLOCK_SIZE * 3);
        player.add(engine.createComponent(ControllableComponent.class));

        playerSprite.textureName = "player1Standing1";
        playerSprite.category = SpriteComponent.EntityCategory.Character;
        player.add(engine.createComponent(JumpComponent.class));

        HealthComponent healthComponent = engine.createComponent(HealthComponent.class);
        healthComponent.health = healthComponent.maxHealth;
        player.add(healthComponent);

        AirComponent airComponent = engine.createComponent(AirComponent.class);
        airComponent.air = airComponent.maxAir;
        player.add(airComponent);

        return player;
    }

    private void generateWorld() {
        generateOres();
        meshTiles();
    }

    private void meshTiles() {
        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {
                int index = x * WORLD_ROWCOUNT + y;

                if (blocks[index].blockType == Block.BlockType.DirtBlockType) {
                    if (blocks[index].hasFlag(Block.BlockFlags.SunlightVisibleBlock)) {
                        blocks[index].unsetFlag(Block.BlockFlags.SunlightVisibleBlock);
                    }
                }

                if (blocks[index].blockType == Block.BlockType.NullBlockType) {
                    continue;
                }

                if (blocks[index].blockType == Block.BlockType.DirtBlockType) {
                    //fixme may be able to be made generic. MAYBE.
                    transitionDirtTile(x, y);
                } else if (blocks[index].blockType == Block.BlockType.StoneBlockType) {
                    transitionStoneTile(x, y);
                }
            }
        }
    }

    private void transitionStoneTile(int x, int y) {
        int index = x * WORLD_ROWCOUNT + y;
        //essentially, if the *other* tiles in question are the same blocks, we should
        //merge/transition with them.

        Set<TileTransitions> result = EnumSet.noneOf(TileTransitions.class);

        boolean leftMerge = shouldTileTransitionWith(x, y, x - 1, y);
        boolean rightMerge = shouldTileTransitionWith(x, y, x + 1, y);
        boolean topMerge = shouldTileTransitionWith(x, y, x, y - 1);
        boolean bottomMerge = shouldTileTransitionWith(x, y, x, y + 1);

        if (leftMerge) {
            result.add(TileTransitions.left);
        }

        if (rightMerge) {
            result.add(TileTransitions.right);
        }

        if (topMerge) {
            result.add(TileTransitions.top);
        }

        if (bottomMerge) {
            result.add(TileTransitions.bottom);
        }

        Integer lookup = stoneTransitionTypes.get(result);
        assert lookup != null : "transition lookup failure!";
        blocks[index].meshType = (byte) lookup.intValue();
    }

    private void transitionDirtTile(int x, int y) {
        int index = x * WORLD_ROWCOUNT + y;
        //essentially, if the *other* tiles in question are the same blocks, we should
        //merge/transition with them.

        Set<TileTransitions> result = EnumSet.noneOf(TileTransitions.class);

        boolean leftMerge = shouldTileTransitionWith(x, y, x - 1, y);
        boolean rightMerge = shouldTileTransitionWith(x, y, x + 1, y);
        boolean topMerge = shouldTileTransitionWith(x, y, x, y - 1);
        boolean bottomMerge = shouldTileTransitionWith(x, y, x, y + 1);

        if (leftMerge) {
            result.add(TileTransitions.left);
        }

        if (rightMerge) {
            result.add(TileTransitions.right);
        }

        if (topMerge) {
            result.add(TileTransitions.top);
        }

        if (bottomMerge) {
            result.add(TileTransitions.bottom);
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
     * @return
     */
    private boolean shouldTileTransitionWith(int sourceTileX, int sourceTileY, int nearbyTileX, int nearbyTileY) {
        boolean isMatched = false;
        int srcIndex = MathUtils.clamp(sourceTileX * WORLD_ROWCOUNT + sourceTileY, 0, WORLD_ROWCOUNT * WORLD_COLUMNCOUNT - 1);
        int nearbyIndex = MathUtils.clamp(nearbyTileX * WORLD_ROWCOUNT + nearbyTileY, 0, WORLD_ROWCOUNT * WORLD_COLUMNCOUNT - 1);

        if (blocks[srcIndex].blockType == blocks[nearbyIndex].blockType) {
            //todo in the future look up if it blends or not based on various thingies. not jsut "is tile same"
            //some may be exceptions??
            isMatched = true;
        }

        return isMatched;
    }

    private void initializeWorld() {
        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {

                int index = x * WORLD_ROWCOUNT + y;
                blocks[index] = new Block();
                blocks[index].blockType = Block.BlockType.NullBlockType;
            }
        }
    }

    private void generateOres() {
        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {

                int index = x * WORLD_ROWCOUNT + y;

                //java wants me to go through each and every block and initialize them..
                blocks[index] = new Block();
                blocks[index].blockType = Block.BlockType.NullBlockType;

                //create some sky
                if (y <= seaLevel()) {
                    continue;
                }

                switch (MathUtils.random(0, 3)) {
                    case 0:
                        blocks[index].blockType = Block.BlockType.NullBlockType;
                        break;

                    case 1:
                        blocks[index].blockType = Block.BlockType.DirtBlockType;
                        break;
                    case 2:
                        //hack, simulate only dirt for now. blocks[index].blockType = Block.BlockType.StoneBlockType;
                        blocks[index].blockType = Block.BlockType.DirtBlockType;
                        break;
                }

//                blocks[dragSourceIndex].wallType = Block::Wall
            }
        }
//        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
//            for (int y = seaLevel(); y < WORLD_ROWCOUNT; ++y) {
//                Block block = blockAt(x, y);
//                block.blockType = Block.BlockType.DirtBlockType;
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
        int x = MathUtils.clamp((int) (pos.x / BLOCK_SIZE), 0, WORLD_COLUMNCOUNT - 1);
        int y = MathUtils.clamp((int) (pos.y / BLOCK_SIZE), 0, WORLD_ROWCOUNT - 1);
        return blockAt(x, y);
    }

    /**
     * safely return a block at x, y, clamped at world bounds
     *
     * @param x
     * @param y
     * @return
     */
    public Block blockAtSafely(int x, int y) {
        return blocks[MathUtils.clamp(x, 0, WORLD_COLUMNCOUNT - 1) * WORLD_ROWCOUNT + MathUtils.clamp(y, 0, WORLD_ROWCOUNT - 1)];
    }

    public Block blockAt(int x, int y) {
        assert x >= 0 && y >= 0 && x <= WORLD_COLUMNCOUNT && y <= WORLD_ROWCOUNT : "block index out of range";

        return blocks[x * WORLD_ROWCOUNT + y];
    }

    public boolean isBlockSolid(int x, int y) {
        boolean solid = true;

        byte type = blockAt(x, y).blockType;

        if (type == Block.BlockType.NullBlockType) {
            solid = false;
        }

        return solid;
    }

    public boolean canPlaceBlock(int x, int y) {
        boolean canPlace = blockAt(x, y).blockType == Block.BlockType.NullBlockType;
        //TODO: check collision with other entities...

        return canPlace;
    }

    public void dispose() {
    }

    public void zoom(float factor) {

        m_camera.zoom *= factor;
    }

    public void update(double elapsed) {
        if (isClient()) {
            if (m_mainPlayer == null) {
                return;
            }
//        playerSprite.sprite.setOriginCenter();

//        m_camera.position.set(playerSprite.sprite.getX() + playerSprite.sprite.getWidth() * 0.5f, playerSprite.sprite.getY() + playerSprite.sprite.getHeight() * 0.5f, 0);

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
            updateItemPlacementGhost();

            if (m_client.leftMouseDown && !m_powerOverlaySystem.overlayVisible) {
                handleLeftMousePrimaryAttack();
            }
        }

        if (isServer()) {

        }

        //todo explicitly call update on systems, me thinks...otherwise the render and update steps are coupled
        engine.update((float) elapsed);

    }

    private void handleLeftMousePrimaryAttack() {
        Vector2 mouse = mousePositionWorldCoords();

        PlayerComponent playerComponent = playerMapper.get(m_mainPlayer);
        Entity item = playerComponent.equippedPrimaryItem();
        if (item == null) {
            return;
        }

        ToolComponent toolComponent = toolMapper.get(item);
        if (toolComponent != null) {
            if (toolComponent.type != ToolComponent.ToolType.Drill) {
                return;
            }

            int x = (int) (mouse.x / BLOCK_SIZE);
            int y = (int) (mouse.y / BLOCK_SIZE);

            Block block = blockAt(x, y);

            if (block.blockType != Block.BlockType.NullBlockType) {
                block.destroy();
                m_client.sendBlockPick(x, y);
            }

            //action performed
            return;
        }

        BlockComponent blockComponent = blockMapper.get(item);
        if (blockComponent != null) {

            int x = (int) (mouse.x / BLOCK_SIZE);
            int y = (int) (mouse.y / BLOCK_SIZE);

            Block block = blockAt(x, y);

            //attempt to place one if the area is empty
            if (block.blockType == Block.BlockType.NullBlockType) {
                block.blockType = blockComponent.blockType;
                m_client.sendBlockPlace(x, y);
            }

            //action performed
            return;
        }

        ItemComponent itemComponent = itemMapper.get(item);
        if (itemComponent != null) {
            if (playerComponent.placeableItemTimer.milliseconds() > PlayerComponent.placeableItemDelay) {
                playerComponent.placeableItemTimer.reset();

                attemptItemPlace(mouse.x, mouse.y, playerComponent.equippedPrimaryItem());
            }
        }
    }

    private void attemptItemPlace(float x, float y, Entity item) {

        //place the item
        Entity placedItem = cloneEntity(item);

        ItemComponent placedItemComponent = itemMapper.get(placedItem);

        placedItemComponent.state = ItemComponent.State.InWorldState;

        Vector2 alignedPosition = new Vector2(x, y);
        SpriteComponent spriteComponent = spriteMapper.get(placedItem);
        alignPositionToBlocks(alignedPosition);

        spriteComponent.sprite.setPosition(alignedPosition.x, alignedPosition.y);

        engine.addEntity(placedItem);

        if (isPlacementValid(placedItem)) {
            //hack, do more validation..
            m_client.sendItemPlace(alignedPosition.x, alignedPosition.y);
        } else {
            //fixme i know, it isn't ideal..i technically add the item anyways and delete it if it cannot be placed
            //because the function actually takes only the entity, to check if its size, position etc conflict with anything
            engine.removeEntity(placedItem);
        }
    }

    public void render(double elapsed) {
        if (m_mainPlayer == null) {
            return;
        }

        meshTiles();
        //computeSunlight();
//        m_camera.zoom *= 0.9;
        //m_lightRenderer->renderToFBO();

        if (m_client.m_renderTiles) {
            //m_tileRenderer.render(elapsed);
        } else {

        }

        //FIXME: incorporate entities into the pre-lit gamescene FBO, then render lighting as last pass
        //m_lightRenderer->renderToBackbuffer();

        //FIXME: take lighting into account, needs access to fbos though.
        //   m_fluidRenderer->render();
//    m_particleRenderer->render();
//FIXME unused    m_quadTreeRenderer->render();

        updateCrosshair();
        updateItemPlacementGhost();
    }

    private void computeSunlight() {
        int previousY = 0;

        //if true, probably continue downward until we are surounded on both sides by blocks
        boolean previousYHadSun = false;
        boolean ignoreFurther = false;

        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            ignoreFurther = false;

            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {
                int index = x * WORLD_ROWCOUNT + y;

                if (ignoreFurther) {
                    y = WORLD_ROWCOUNT;
                    continue;
                }

                if (previousYHadSun) {

                    Block leftBlock = blockAt(MathUtils.clamp(x - 1, 0, WORLD_COLUMNCOUNT), y);
                    Block rightBlock = blockAt(MathUtils.clamp(x + 1, 0, WORLD_COLUMNCOUNT), y);
                    Block topBlock = blockAt(x, MathUtils.clamp(y - 1, 0, WORLD_ROWCOUNT));
                    Block bottomBlock = blockAt(x, MathUtils.clamp(y + 1, 0, WORLD_ROWCOUNT));

                    if (blocks[index].blockType == Block.BlockType.DirtBlockType) {
                        previousYHadSun = true;

                        boolean leftMerge = leftBlock.blockType == Block.BlockType.DirtBlockType;

                        boolean rightMerge = rightBlock.blockType == Block.BlockType.DirtBlockType;

                        //hack is top redundant?
                        boolean topMerge = topBlock.blockType == Block.BlockType.DirtBlockType;

                        boolean bottomMerge = bottomBlock.blockType == Block.BlockType.DirtBlockType;

                        blocks[index].setFlag(Block.BlockFlags.SunlightVisibleBlock);

                        Set<TileTransitions> result = EnumSet.noneOf(TileTransitions.class);
                        if (leftMerge) {
                            result = EnumSet.of(TileTransitions.left);
                        }

                        if (rightMerge) {
                            result = EnumSet.of(TileTransitions.right);
                        }

                        if (topMerge) {
                            result = EnumSet.of(TileTransitions.top);
                        }

                        if (bottomMerge) {
                            result = EnumSet.of(TileTransitions.bottom);
                        }

                        byte finalMesh = (byte) grassTransitionTypes.get(result).intValue();

                        if (result.isEmpty() || finalMesh == 15) {

                            //byte finalMesasdash = (byte) grassTransitionTypes.get(result, -1);
                        }

                        blocks[index].meshType = finalMesh;
                        if (finalMesh == -1) {
                            assert false : "invalid mesh type retrieval, for some reason";
                        }
                    } else {
                        //skip to next column, no more grass transitions possible in this area
                        previousYHadSun = false;
                        ignoreFurther = true;
                        //++x;
                        //y = 0;
                    }
                } else {
//                    if (blocks[index].blockType == Block.BlockType.NullBlockType) {
//                        continue;
//                    }
//hack fixme, this is the issue..when it's surrounded by none/it's the first one in the list, it never hits the above
                    //condition, i think
                    if (blocks[index].blockType == Block.BlockType.DirtBlockType) {
                        blocks[index].setFlag(Block.BlockFlags.SunlightVisibleBlock);
                        previousYHadSun = true;
                    }
                }
            }
        }

    }

    /*
    private boolean shouldGrassMesh(int sourceTileX, int sourceTileY, int nearbyTileX, int nearbyTileY) {
        boolean isMatched = false;
        int srcIndex = MathUtils.clamp(sourceTileX * WORLD_ROWCOUNT + sourceTileY, 0, WORLD_ROWCOUNT * WORLD_COLUMNCOUNT - 1);
        int nearbyIndex = MathUtils.clamp(nearbyTileX * WORLD_ROWCOUNT + nearbyTileY, 0, WORLD_ROWCOUNT * WORLD_COLUMNCOUNT - 1);

        if (blocks[srcIndex].blockType == blocks[nearbyIndex].blockType) {
            //todo in the future look up if it blends or not based on various thingies. not jsut "is tile same"
            //some may be exceptions??
            isMatched = true;
        }

        return isMatched;
    }
    */

    private void updateCrosshair() {
        //PlayerComponent playerComponent = playerMapper.get(m_mainPlayer);
        //playerComponent

        SpriteComponent spriteComponent = spriteMapper.get(m_blockPickingCrosshair);

        Vector2 mouse = mousePositionWorldCoords();
        Vector2 crosshairPosition = new Vector2(BLOCK_SIZE * MathUtils.floor(mouse.x / BLOCK_SIZE), BLOCK_SIZE * MathUtils.floor(mouse.y / BLOCK_SIZE));

        Vector2 crosshairOriginOffset = new Vector2(spriteComponent.sprite.getWidth() * 0.5f, spriteComponent.sprite.getHeight() * 0.5f);

        Vector2 crosshairFinalPosition = crosshairPosition.add(crosshairOriginOffset);

        spriteComponent.sprite.setPosition(crosshairFinalPosition.x, crosshairFinalPosition.y);
    }

    Vector2 mousePositionWorldCoords() {
        //libgdx can and probably will return negative mouse coords..
        Vector3 mouse = new Vector3(Math.max(Gdx.input.getX(), 0), Math.max(Gdx.input.getY(), 0), 0f);
        Vector3 finalMouse = m_camera.unproject(mouse);

        return new Vector2(finalMouse.x, finalMouse.y);
    }

    private void updateItemPlacementGhost() {
        if (m_itemPlacementGhost == null || m_itemPlacementGhost.getId() == 0) {
            return;
        }

        Vector2 mouse = mousePositionWorldCoords();
        alignPositionToBlocks(mouse);

        SpriteComponent spriteComponent = spriteMapper.get(m_itemPlacementGhost);
        spriteComponent.sprite.setPosition(mouse.x, mouse.y);
        spriteComponent.placementValid = isPlacementValid(m_itemPlacementGhost);
    }

    private void alignPositionToBlocks(Vector2 pos) {
        pos.set(BLOCK_SIZE * MathUtils.floor(pos.x / BLOCK_SIZE), BLOCK_SIZE * MathUtils.floor(pos.y / BLOCK_SIZE));
    }

    public int seaLevel() {
        return WORLD_SEA_LEVEL;
    }

    public void createBlockItem(Entity block, byte blockType) {
        block.add(engine.createComponent(VelocityComponent.class));

        BlockComponent blockComponent = engine.createComponent(BlockComponent.class);
        blockComponent.blockType = blockType;
        block.add(blockComponent);

        SpriteComponent blockSprite = engine.createComponent(SpriteComponent.class);
        blockSprite.textureName = blockTypes.get(blockComponent.blockType).textureName;

//warning fixme size is fucked
        blockSprite.sprite.setSize(32 / World.PIXELS_PER_METER, 32 / World.PIXELS_PER_METER);
        block.add(blockSprite);

        ItemComponent itemComponent = engine.createComponent(ItemComponent.class);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;
        block.add(itemComponent);
    }

    public Entity createAirGenerator() {
        Entity air = engine.createEntity();
        ItemComponent itemComponent = engine.createComponent(ItemComponent.class);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;
        air.add(itemComponent);

        PowerDeviceComponent power = engine.createComponent(PowerDeviceComponent.class);
        air.add(power);

        SpriteComponent airSprite = engine.createComponent(SpriteComponent.class);
        airSprite.textureName = "air-generator-64x64";

//warning fixme size is fucked
        airSprite.sprite.setSize(BLOCK_SIZE * 4, BLOCK_SIZE * 4);
        air.add(airSprite);

        AirGeneratorComponent airComponent = engine.createComponent(AirGeneratorComponent.class);
        airComponent.airOutputRate = 100;
        air.add(airComponent);

        return air;
    }

    private boolean isPlacementValid(Entity entity) {
        SpriteComponent spriteComponent = spriteMapper.get(entity);
        Vector2 pos = new Vector2(spriteComponent.sprite.getX(), spriteComponent.sprite.getY());
        Vector2 size = new Vector2(spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

        float epsilon = 0.001f;
        int startX = (int) ((pos.x - (size.x * 0.5f)) / BLOCK_SIZE + epsilon);
        int startY = (int) ((pos.y - (size.y * 0.5f)) / BLOCK_SIZE + epsilon);

        int endX = (int) ((pos.x + (size.x * 0.5f)) / BLOCK_SIZE + 0);
        int endY = (int) ((pos.y + (size.y * 0.5f - epsilon)) / BLOCK_SIZE + 1);

        if (!(startX >= 0 && startY >= 0 && endX <= WORLD_COLUMNCOUNT && endY <= WORLD_ROWCOUNT)) {
            //fixme
            //not sure why, but this ends up giving me some way way invalid values. likely due to mouse being outside
            //of valid range, *somehow*. sometimes does it on startup etc
            return false;
        }

        for (int column = startX; column < endX; ++column) {
            for (int row = startY; row < endY; ++row) {
                if (blockAt(column, row).blockType != Block.BlockType.NullBlockType) {
                    return false;
                }
            }
        }

        //float x = Math.min(pos.x - (BLOCK_SIZE * 20), 0.0f);
        //float y = Math.min(pos.y - (BLOCK_SIZE * 20), 0.0f);
        //float x2 = Math.min(pos.x + (BLOCK_SIZE * 20), WORLD_COLUMNCOUNT * BLOCK_SIZE);
        //float y2 = Math.min(pos.y + (BLOCK_SIZE * 20), WORLD_ROWCOUNT * BLOCK_SIZE);

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(SpriteComponent.class).get());
        for (int i = 0; i < entities.size(); ++i) {
            //it's us, don't count a collision with ourselves
            if (entities.get(i) == entity) {
                continue;
            }

            //ignore players, aka don't count them as colliding when placing static objects.
//        if (e.has_component<PlayerComponent>()) {
//            continue;
//        }

            ItemComponent itemComponent = itemMapper.get(entities.get(i));
            if (itemComponent != null) {
                if (itemComponent.state == ItemComponent.State.DroppedInWorld) {
                    continue;
                }
            }

            TagComponent tagComponent = tagMapper.get(entities.get(i));
            if (tagComponent != null && tagComponent.tag.equals("itemPlacementGhost")) {
                //ignore all collisions with this
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

        boolean collides = !(left2 > right1
                || right2 < left1
                || top2 > bottom1
                || bottom2 < top1);

        return collides;
    }

    public void loadBlockRegion(Network.BlockRegion region) {
        int sourceIndex = 0;
        for (int row = region.y; row < region.y2; ++row) {
            for (int col = region.x; col < region.x2; ++col) {
                int index = col * WORLD_ROWCOUNT + row;

                Block origBlock = blocks[index];
                Network.SingleBlock srcBlock = region.blocks.get(sourceIndex);
                origBlock.blockType = srcBlock.blockType;

                //fixme wall type as well

                ++sourceIndex;
            }
        }

        Gdx.app.log("block region", "loading");

        //fixme obviously don't do the whole world..
        meshTiles();
    }

    /**
     * Clone everything about the entity. Does *not* add it to the engine
     *
     * @param entity to clone
     * @return the cloned entity
     */
    public Entity cloneEntity(Entity entity) {
        Entity clonedEntity = engine.createEntity();

        //sorted alphabetically for your pleasure
        AirComponent airComponent = airMapper.get(entity);
        if (airComponent != null) {
            AirComponent clonedComponent = new AirComponent(airComponent);
            clonedEntity.add(clonedComponent);
        }

        AirGeneratorComponent airGeneratorComponent = airGeneratorMapper.get(entity);
        if (airGeneratorComponent != null) {
            AirGeneratorComponent clonedComponent = new AirGeneratorComponent(airGeneratorComponent);
            clonedEntity.add(clonedComponent);
        }

        BlockComponent blockComponent = blockMapper.get(entity);
        if (blockComponent != null) {
            BlockComponent clonedComponent = new BlockComponent(blockComponent);
            clonedEntity.add(clonedComponent);
        }

        ControllableComponent controllableComponent = controlMapper.get(entity);
        if (controllableComponent != null) {
            ControllableComponent clonedComponent = new ControllableComponent(controllableComponent);
            clonedEntity.add(clonedComponent);
        }

        HealthComponent healthComponent = healthMapper.get(entity);
        if (healthComponent != null) {
            HealthComponent clonedComponent = new HealthComponent(healthComponent);
            clonedEntity.add(clonedComponent);
        }

        ItemComponent itemComponent = itemMapper.get(entity);
        if (itemComponent != null) {
            ItemComponent clonedComponent = new ItemComponent(itemComponent);
            clonedEntity.add(clonedComponent);
        }

        JumpComponent jumpComponent = jumpMapper.get(entity);
        if (jumpComponent != null) {
            JumpComponent clonedComponent = new JumpComponent(jumpComponent);
            clonedEntity.add(clonedComponent);
        }

        //player, unneeded
        assert playerMapper.get(entity) == null;

        SpriteComponent spriteComponent = spriteMapper.get(entity);
        if (spriteComponent != null) {
            SpriteComponent clonedComponent = new SpriteComponent(spriteComponent);
            clonedEntity.add(clonedComponent);
        }

        TagComponent tagComponent = tagMapper.get(entity);
        if (tagComponent != null) {
            TagComponent clonedComponent = new TagComponent(tagComponent);
            clonedEntity.add(clonedComponent);
        }

        ToolComponent toolComponent = toolMapper.get(entity);
        if (toolComponent != null) {
            ToolComponent clonedComponent = new ToolComponent(toolComponent);
            clonedEntity.add(clonedComponent);
        }

        TorchComponent torchComponent = torchMapper.get(entity);
        if (torchComponent != null) {
            TorchComponent clonedComponent = new TorchComponent(torchComponent);
            clonedEntity.add(clonedComponent);
        }

        VelocityComponent velocityComponent = velocityMapper.get(entity);
        if (velocityComponent != null) {
            VelocityComponent clonedComponent = new VelocityComponent(velocityComponent);
            clonedEntity.add(clonedComponent);
        }

        PowerDeviceComponent powerDeviceComponent = powerMapper.get(entity);
        if (powerDeviceComponent != null) {
            PowerDeviceComponent clonedComponent = new PowerDeviceComponent(powerDeviceComponent);
            clonedEntity.add(clonedComponent);
        }

        return clonedEntity;
    }

    public void addPlayer(Entity player) {
        m_players.add(player);
    }

    public Entity playerForID(int playerIdWhoDropped) {
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
        boolean collides;

        BlockStruct(String _textureName, boolean _collides) {
            textureName = _textureName;
            collides = _collides;
        }
    }
}
