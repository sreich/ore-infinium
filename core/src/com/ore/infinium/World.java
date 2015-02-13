package com.ore.infinium;

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
    public static final float GRAVITY_ACCEL_CLAMP = 9.8f / PIXELS_PER_METER / 3.0f;

    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;

    public static final int WORLD_COLUMNCOUNT = 1000; //2400
    public static final int WORLD_ROWCOUNT = 1000; //8400
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

    public Array<Entity> m_players = new Array<>();
    public Entity m_mainPlayer;

    public OreServer m_server;
    public AssetManager assetManager;
    public OreClient m_client;
    private boolean m_noClipEnabled;
    protected TileRenderer m_tileRenderer;
    public OrthographicCamera m_camera;
    PowerOverlayRenderSystem m_powerOverlaySystem;

    private Entity m_blockPickingCrosshair;
    private Entity m_itemPlacementGhost;

    //fixme remove in favor of the render system
    public TextureAtlas m_atlas;

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
        }

        if (isServer()) {
            generateWorld();
        }
    }

    protected void clientInventoryItemSelected() {
        assert !isServer();

        PlayerComponent playerComponent = Mappers.player.get(m_mainPlayer);
        Entity entity = playerComponent.equippedPrimaryItem();
        if (entity == null) {
            return;
        }

        if (m_itemPlacementGhost != null) {
            engine.removeEntity(m_itemPlacementGhost);
        }

        //this item is placeable, show a ghost of it so we can see where we're going to place it
        m_itemPlacementGhost = cloneEntity(entity);
        ItemComponent itemComponent = Mappers.item.get(m_itemPlacementGhost);
        itemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = Mappers.sprite.get(m_itemPlacementGhost);

        TagComponent tag = engine.createComponent(TagComponent.class);
        tag.tag = "itemPlacementGhost";
        m_itemPlacementGhost.add(tag);
        engine.addEntity(m_itemPlacementGhost);
    }

    public void initServer() {
    }

    public void initClient(Entity mainPlayer) {
        m_mainPlayer = mainPlayer;
//        Mappers.velocity.get(m_mainPlayer);

        engine.addSystem(m_tileRenderer = new TileRenderer(m_camera, this, 1f / 60f));
        engine.addSystem(new SpriteRenderSystem(this));
        engine.addSystem(m_powerOverlaySystem = new PowerOverlayRenderSystem(this));

        SpriteComponent playerSprite = Mappers.sprite.get(m_mainPlayer);
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
        }

        if (isServer()) {

        }

        //todo explicitly call update on systems, me thinks...otherwise the render and update steps are coupled
        engine.update((float) elapsed);

    }

    private void handleLeftMousePrimaryAttack() {
        Vector2 mouse = mousePositionWorldCoords();

        PlayerComponent playerComponent = Mappers.player.get(m_mainPlayer);
        Entity item = playerComponent.equippedPrimaryItem();
        if (item == null) {
            return;
        }

        ToolComponent toolComponent = Mappers.tool.get(item);
        if (toolComponent != null) {
            if (toolComponent.type != ToolComponent.ToolType.Drill) {
                return;
            }

            int x = (int) (mouse.x / BLOCK_SIZE);
            int y = (int) (mouse.y / BLOCK_SIZE);

            Block block = blockAt(x, y);

            //attempt to destroy it if it's not already destroyed...
            if (block.blockType != Block.BlockType.NullBlockType) {
                block.blockType = Block.BlockType.NullBlockType;
                m_client.sendBlockPick(x, y);
            }

            //action performed
            return;
        }

        BlockComponent blockComponent = Mappers.block.get(item);
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

        ItemComponent itemComponent = Mappers.item.get(item);
        if (itemComponent != null) {
            //place the item
            Entity placedItem = cloneEntity(playerComponent.equippedPrimaryItem());

            itemComponent.state = ItemComponent.State.InWorldState;

            SpriteComponent spriteComponent = Mappers.sprite.get(placedItem);
            spriteComponent.sprite.setPosition(mouse.x, mouse.y);

            engine.addEntity(placedItem);

            //hack, do more validation..
            m_client.sendItemPlace(mouse.x, mouse.y);
        }
    }

    public void render(double elapsed) {
        if (m_mainPlayer == null) {
            return;
        }

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

    private void updateCrosshair() {
        //PlayerComponent playerComponent = Mappers.player.get(m_mainPlayer);
        //playerComponent

        SpriteComponent spriteComponent = Mappers.sprite.get(m_blockPickingCrosshair);

        Vector2 mouse = mousePositionWorldCoords();
        Vector2 crosshairPosition = new Vector2(BLOCK_SIZE * MathUtils.floor(mouse.x / BLOCK_SIZE), BLOCK_SIZE * MathUtils.floor(mouse.y / BLOCK_SIZE));

        Vector2 crosshairOriginOffset = new Vector2(spriteComponent.sprite.getWidth() * 0.5f, spriteComponent.sprite.getHeight() * 0.5f);

        Vector2 crosshairFinalPosition = crosshairPosition.add(crosshairOriginOffset);

        spriteComponent.sprite.setPosition(crosshairFinalPosition.x, crosshairFinalPosition.y);
    }

    private Vector2 mousePositionWorldCoords() {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        Vector3 finalMouse = m_camera.unproject(mouse);

        return new Vector2(finalMouse.x, finalMouse.y);
    }

    private void updateItemPlacementGhost() {
        if (m_itemPlacementGhost == null) {
            return;
        }

        Vector2 mouse = mousePositionWorldCoords();
        Vector2 crosshairPosition = new Vector2(BLOCK_SIZE * MathUtils.floor(mouse.x / BLOCK_SIZE), BLOCK_SIZE * MathUtils.floor(mouse.y / BLOCK_SIZE));

        SpriteComponent spriteComponent = Mappers.sprite.get(m_itemPlacementGhost);
        spriteComponent.sprite.setPosition(crosshairPosition.x, crosshairPosition.y);
        spriteComponent.placementValid = isPlacementValid(m_itemPlacementGhost);
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
        SpriteComponent spriteComponent = Mappers.sprite.get(entity);
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

            ItemComponent itemComponent = Mappers.item.get(entities.get(i));
            if (itemComponent != null) {
                if (itemComponent.state == ItemComponent.State.DroppedInWorld) {
                    continue;
                }
            }

            if (entityCollides(entities.get(i), entity)) {
                return false;
            }
        }

        return true;
    }

    private boolean entityCollides(Entity first, Entity second) {
        SpriteComponent spriteComponent1 = Mappers.sprite.get(first);
        SpriteComponent spriteComponent2 = Mappers.sprite.get(second);

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
        AirComponent airComponent = Mappers.air.get(entity);
        if (airComponent != null) {
            AirComponent clonedComponent = new AirComponent(airComponent);
            clonedEntity.add(clonedComponent);
        }

        AirGeneratorComponent airGeneratorComponent = Mappers.airGenerator.get(entity);
        if (airGeneratorComponent != null) {
            AirGeneratorComponent clonedComponent = new AirGeneratorComponent(airGeneratorComponent);
            clonedEntity.add(clonedComponent);
        }

        BlockComponent blockComponent = Mappers.block.get(entity);
        if (blockComponent != null) {
            BlockComponent clonedComponent = new BlockComponent(blockComponent);
            clonedEntity.add(clonedComponent);
        }

        ControllableComponent controllableComponent = Mappers.control.get(entity);
        if (controllableComponent != null) {
            ControllableComponent clonedComponent = new ControllableComponent(controllableComponent);
            clonedEntity.add(clonedComponent);
        }

        HealthComponent healthComponent = Mappers.health.get(entity);
        if (healthComponent != null) {
            HealthComponent clonedComponent = new HealthComponent(healthComponent);
            clonedEntity.add(clonedComponent);
        }

        ItemComponent itemComponent = Mappers.item.get(entity);
        if (itemComponent != null) {
            ItemComponent clonedComponent = new ItemComponent(itemComponent);
            clonedEntity.add(clonedComponent);
        }

        JumpComponent jumpComponent = Mappers.jump.get(entity);
        if (jumpComponent != null) {
            JumpComponent clonedComponent = new JumpComponent(jumpComponent);
            clonedEntity.add(clonedComponent);
        }

        //player, unneeded
        assert Mappers.player.get(entity) == null;

        SpriteComponent spriteComponent = Mappers.sprite.get(entity);
        if (spriteComponent != null) {
            SpriteComponent clonedComponent = new SpriteComponent(spriteComponent);
            clonedEntity.add(clonedComponent);
        }

        TagComponent tagComponent = Mappers.tag.get(entity);
        if (tagComponent != null) {
            TagComponent clonedComponent = new TagComponent(tagComponent);
            clonedEntity.add(clonedComponent);
        }

        ToolComponent toolComponent = Mappers.tool.get(entity);
        if (toolComponent != null) {
            ToolComponent clonedComponent = new ToolComponent(toolComponent);
            clonedEntity.add(clonedComponent);
        }

        TorchComponent torchComponent = Mappers.torch.get(entity);
        if (torchComponent != null) {
            TorchComponent clonedComponent = new TorchComponent(torchComponent);
            clonedEntity.add(clonedComponent);
        }

        VelocityComponent velocityComponent = Mappers.velocity.get(entity);
        if (velocityComponent != null) {
            VelocityComponent clonedComponent = new VelocityComponent(velocityComponent);
            clonedEntity.add(clonedComponent);
        }

        return clonedEntity;
    }

    public void addPlayer(Entity player) {
        m_players.add(player);
    }

    public static class BlockStruct {
        public String textureName; //e.g. "dirt", "stone", etc.
        boolean collides;

        BlockStruct(String _textureName, boolean _collides) {
            textureName = _textureName;
            collides = _collides;
        }
    }

    public Entity playerForID(int playerIdWhoDropped) {
        assert !isClient();
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(PlayerComponent.class).get());
        PlayerComponent playerComponent;
        for (int i = 0; i < entities.size(); ++i) {
            playerComponent = Mappers.player.get(entities.get(i));
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
            m_powerOverlaySystem.leftMouseReleased();
            return true;
        } else {
            handleLeftMousePrimaryAttack();
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
}
