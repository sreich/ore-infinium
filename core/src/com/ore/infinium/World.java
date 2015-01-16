package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.MovementSystem;
import com.ore.infinium.systems.PlayerSystem;

import java.util.HashMap;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                        *
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
    public static final float GRAVITY_ACCEL_CLAMP = 9.8f / PIXELS_PER_METER / 20.0f;

    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;

    public static final int WORLD_COLUMNCOUNT = 2400;
    public static final int WORLD_ROWCOUNT = 8400;
    public static final int WORLD_SEA_LEVEL = 50;

    public static final HashMap<Block.BlockType, BlockStruct> blockTypes = new HashMap<>();

    static {
        blockTypes.put(Block.BlockType.NullBlockType, new BlockStruct("", false));
        blockTypes.put(Block.BlockType.DirtBlockType, new BlockStruct("dirt", true));
        blockTypes.put(Block.BlockType.StoneBlockType, new BlockStruct("stone", true));
    }

    private static final int zoomInterval = 50; //ms
    private static OreTimer m_zoomTimer = new OreTimer();

    public Block[] blocks;
    public PooledEngine engine;
    public AssetManager assetManager;
    public Array<Entity> m_players = new Array<>();
    public Entity m_mainPlayer;
    public OreServer m_server;
    private boolean m_noClipEnabled;
    private SpriteBatch m_batch;
    private Texture m_texture;
    private Sprite m_sprite2;
    private TileRenderer m_tileRenderer;
    private SpriteRenderer m_spriteRenderer;
    private OrthographicCamera m_camera;
    private OreClient m_client;

    public World(OreClient client, OreServer server) {
        m_client = client;
        m_server = server;

        if (isClient()) {
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            m_batch = new SpriteBatch();
        }

        blocks = new Block[WORLD_ROWCOUNT * WORLD_COLUMNCOUNT];


//        assetManager = new AssetManager();
//        TextureAtlas atlas = assetManager.get("data/", TextureAtlas.class);
//        assetManager.finishLoading();
        engine = new PooledEngine(2000, 2000, 2000, 2000);

        engine.addSystem(new MovementSystem(this));
        engine.addSystem(new PlayerSystem(this));

        m_sprite2 = new Sprite();
        m_sprite2.setPosition(90, 90);

        m_camera = new OrthographicCamera(1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);//30, 30 * (h / w));
        m_camera.setToOrtho(true, 1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);

//        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);

        assert isClient() ^ isServer();
        if (isClient()) {
            m_texture = new Texture(Gdx.files.internal("crap.png"));
            m_sprite2.setTexture(m_texture);
            initializeWorld();
        }

        if (isServer()) {
            generateWorld();
        }
    }

    public void initServer() {
    }

    public void initClient(Entity mainPlayer) {
        m_mainPlayer = mainPlayer;
//        Mappers.velocity.get(m_mainPlayer);

        m_tileRenderer = new TileRenderer(m_camera, this);
        m_spriteRenderer = new SpriteRenderer();
        Mappers.sprite.get(m_mainPlayer).sprite.setTexture(m_texture);
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

        playerSprite.texture = "player1Standing1";
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
    }

    private void initializeWorld() {
        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {

                int index = x * WORLD_ROWCOUNT + y;
                blocks[index] = new Block();
                blocks[index].blockType = Block.BlockType.StoneBlockType;
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
                        blocks[index].blockType = Block.BlockType.StoneBlockType;
                        break;
                }

//                blocks[index].wallType = Block::Wall
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
        return blockAt((int) (pos.x / BLOCK_SIZE), (int) (pos.y / BLOCK_SIZE));
    }

    public Block blockAt(int x, int y) {
        assert x >= 0 && y >= 0 && x <= WORLD_COLUMNCOUNT && y <= WORLD_ROWCOUNT;

        return blocks[x * WORLD_ROWCOUNT + y];
    }

    public boolean isBlockSolid(int x, int y) {
        boolean solid = true;

        Block.BlockType type = blockAt(x, y).blockType;

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
//        m_batch.dispose();
//        m_texture.dispose();
    }

    public void zoom(float factor) {

        m_camera.zoom *= factor;
    }

    public void update(double elapsed) {
        if (isClient()) {
            ControllableComponent controllableComponent = Mappers.control.get(m_mainPlayer);

            controllableComponent.desiredDirection.setZero();

            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                controllableComponent.desiredDirection.x = -1;
            }

            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                controllableComponent.desiredDirection.x = 1;
            }

            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {

            }

            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {

            }

            if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
                JumpComponent jumpComponent = Mappers.jump.get(m_mainPlayer);
                jumpComponent.shouldJump = true;
            }

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
        }

        if (isServer()) {

        }

        engine.update((float) elapsed);

        if (isClient()) {
            m_client.sendPlayerMoved();
        }
    }

    public void render(double elapsed) {
        if (m_mainPlayer == null) {
            return;
        }
//        m_camera.zoom *= 0.9;
        //m_lightRenderer->renderToFBO();

        m_tileRenderer.render(elapsed);

        //FIXME: incorporate entities into the pre-lit gamescene FBO, then render lighting as last pass
        //m_lightRenderer->renderToBackbuffer();

        m_spriteRenderer.renderEntities(elapsed);
        m_spriteRenderer.renderCharacters(elapsed);
        m_spriteRenderer.renderDroppedEntities(elapsed);
        m_spriteRenderer.renderDroppedBlocks(elapsed);


        //FIXME: take lighting into account, needs access to fbos though.
        //   m_fluidRenderer->render();
//    m_particleRenderer->render();
//FIXME unused    m_quadTreeRenderer->render();

        updateCrosshair();
        updateItemPlacementGhost();


        SpriteComponent playerSprite = Mappers.sprite.get(m_mainPlayer);
        playerSprite.sprite.setOriginCenter();

//        m_camera.position.set(playerSprite.sprite.getX() + playerSprite.sprite.getWidth() * 0.5f, playerSprite.sprite.getY() + playerSprite.sprite.getHeight() * 0.5f, 0);
        m_camera.position.set(playerSprite.sprite.getX(), playerSprite.sprite.getY(), 0);
        m_camera.update();

        m_batch.setProjectionMatrix(m_camera.combined);

        m_tileRenderer.render(elapsed);

        m_batch.begin();
        m_batch.draw(playerSprite.sprite, playerSprite.sprite.getX() - playerSprite.sprite.getWidth() * 0.5f,
                playerSprite.sprite.getY() - playerSprite.sprite.getHeight() * 0.5f,
                playerSprite.sprite.getWidth(), playerSprite.sprite.getHeight());

        m_sprite2.draw(m_batch);
        m_batch.end();
    }

    private void updateCrosshair() {
    }

    private void updateItemPlacementGhost() {

    }

    public int seaLevel() {
        return WORLD_SEA_LEVEL;
    }

    public void createBlockItem(Entity block) {
        block.add(engine.createComponent(VelocityComponent.class));

        BlockComponent blockComponent = engine.createComponent(BlockComponent.class);
        blockComponent.blockType = Block.BlockType.StoneBlockType;
        block.add(blockComponent);

        SpriteComponent blockSprite = engine.createComponent(SpriteComponent.class);
        blockSprite.texture = "pickaxeWooden1"; // HACK ?

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

        SpriteComponent airSprite = engine.createComponent(SpriteComponent.class);
        airSprite.texture = "airGenerator1";

//warning fixme size is fucked
        airSprite.sprite.setSize(BLOCK_SIZE * 4, BLOCK_SIZE * 4);
        air.add(airSprite);

        AirGeneratorComponent airComponent = engine.createComponent(AirGeneratorComponent.class);
        airComponent.airOutputRate = 100;
        air.add(airComponent);

        return air;
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
    }

    public void addPlayer(Entity player) {
        m_players.add(player);
    }

    public static class BlockStruct {
        String textureName; //e.g. "dirt", "stone", etc.
        boolean collides;

        BlockStruct(String _textureName, boolean _collides) {
            textureName = _textureName;
            collides = _collides;
        }
    }
}
