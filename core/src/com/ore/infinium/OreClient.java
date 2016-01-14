package com.ore.infinium;

import com.artemis.ComponentMapper;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.esotericsoftware.minlog.Log;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.ClientBlockDiggingSystem;
import com.ore.infinium.systems.DebugTextRenderSystem;
import com.ore.infinium.systems.NetworkClientSystem;
import com.ore.infinium.systems.PowerOverlayRenderSystem;

import java.io.IOException;

public class OreClient implements ApplicationListener, InputProcessor {
    public final static int ORE_VERSION_MAJOR = 0;
    public final static int ORE_VERSION_MINOR = 1;
    public final static int ORE_VERSION_REVISION = 1;

    public boolean leftMouseDown;
    public StretchViewport viewport;
    protected OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;
    private ComponentMapper<BlockComponent> blockMapper;
    private ComponentMapper<ToolComponent> toolMapper;

    private NetworkClientSystem m_networkClientSystem;
    private TagManager m_tagManager;
    private DebugTextRenderSystem m_debugTextRenderSystem;
    private PowerOverlayRenderSystem m_powerOverlayRenderSystem;
    private ClientBlockDiggingSystem m_clientBlockDiggingSystem;

    // zoom every n ms, while zoom key is held down
    private static final int zoomInterval = 30;
    private static OreTimer m_zoomTimer = new OreTimer();

    private InputMultiplexer m_multiplexer;

    Stage m_stage;
    Skin m_skin;

    public Chat m_chat;
    private Sidebar m_sidebar;

    private DragAndDrop m_dragAndDrop;

    private Dialog dialog;
    private ChatDialog m_chatDialog;
    private HotbarInventoryView m_hotbarView;
    private InventoryView m_inventoryView;

    public Inventory m_hotbarInventory;
    private Inventory m_inventory;

    private ScreenViewport m_viewport;

    public OreServer m_server;
    private Thread m_serverThread;

    public boolean m_renderGui = true;

    public BitmapFont bitmapFont_8pt;

    FreeTypeFontGenerator m_fontGenerator;

    @Override
    public void create() {
        // for debugging kryonet

        if (OreSettings.networkLog) {
            Log.set(Log.LEVEL_DEBUG);
        }

        //        Gdx.app.setLogLevel(Application.LOG_NONE);
        //        Gdx.app.setLogLevel(Application.LOG_NONE);
        //        Log.set(Log.LEVEL_INF

        //        ProgressBar progressBar = new ProgressBar(0, 100, 10, false, m_skin);
        //        progressBar.setValue(50);
        //        progressBar.getStyle().knobBefore = progressBar.getStyle().knob;
        //        progressBar.getStyle().knob.setMinHeight(50);
        //        container.add(progressBar);

        Thread.currentThread().setName("client render thread (GL)");

        m_dragAndDrop = new DragAndDrop();

        m_stage = new Stage(viewport = new StretchViewport(OreSettings.width, OreSettings.height));
        m_multiplexer = new InputMultiplexer(m_stage, this);

        Gdx.input.setInputProcessor(m_multiplexer);

        //fixme: this really needs to be stripped out of the client, put in a proper
        //system or something
        m_fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 13;
        bitmapFont_8pt = m_fontGenerator.generateFont(parameter);

        parameter.size = 9;

        m_fontGenerator.dispose();

        m_skin = new Skin();
        m_skin.addRegions(new TextureAtlas(Gdx.files.internal("packed/ui.atlas")));
        m_skin.add("myfont", bitmapFont_8pt, BitmapFont.class);
        m_skin.load(Gdx.files.internal("ui/ui.json"));

        m_chatDialog = new ChatDialog(this, m_stage, m_skin);
        m_chat = new Chat();
        m_chat.addListener(m_chatDialog);

        m_sidebar = new Sidebar(m_stage, m_skin, this);

        hostAndJoin();
    }

    public void handleLeftMousePrimaryAttack() {
        Vector2 mouse = m_world.mousePositionWorldCoords();

        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        PlayerComponent playerComponent = playerMapper.get(player);
        int itemEntity = playerComponent.getEquippedPrimaryItem();
        if (itemEntity == OreWorld.ENTITY_INVALID) {
            return;
        }

        BlockComponent blockComponent = blockMapper.getSafe(itemEntity);
        if (blockComponent != null) {

            int x = (int) (mouse.x);
            int y = (int) (mouse.y);

            boolean blockPlaced = m_world.attemptBlockPlacement(x, y, blockComponent.blockType);
            if (blockPlaced) {
                m_networkClientSystem.sendBlockPlace(x, y);
            }

            return;
        }

        ItemComponent itemComponent = itemMapper.getSafe(itemEntity);
        if (itemComponent != null) {
            //ignore tools and such, can't place those
            if (toolMapper.has(itemEntity)) {
                return;
            }

            if (playerComponent.placeableItemTimer.milliseconds() > PlayerComponent.placeableItemDelay) {
                playerComponent.placeableItemTimer.reset();

                attemptItemPlace(playerComponent.getEquippedPrimaryItem());
            }
        }
    }

    /**
     * Placement position is determined by the current position of the overlay
     *
     * @param itemEntity
     */
    private void attemptItemPlace(int itemEntity) {

        //place the item
        int placedItemEntity = m_world.cloneEntity(itemEntity);

        ItemComponent placedItemComponent = itemMapper.get(placedItemEntity);

        placedItemComponent.state = ItemComponent.State.InWorldState;

        SpriteComponent spriteComponent = spriteMapper.get(placedItemEntity);

        int placementOverlay = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay).getId();
        SpriteComponent placementOverlaySprite = spriteMapper.get(placementOverlay);

        float placeX = placementOverlaySprite.sprite.getX();
        float placeY = placementOverlaySprite.sprite.getY();
        spriteComponent.sprite.setPosition(placeX, placeY);

        if (m_world.isPlacementValid(placedItemEntity)) {
            //todo, do more validation..
            m_networkClientSystem.sendItemPlace(placeX, placeY);
        } else {
            //fixme i know, it isn't ideal..i technically add the item anyways and delete it if it cannot be placed
            //because the function actually takes only the entity, to check if its size, position etc conflict with
            // anything

            //engine.removeEntity(placedItemEntity);
        }
    }

    public void toggleChatVisible() {
        if (m_chatDialog.chatVisibilityState == ChatDialog.ChatVisibility.Normal) {
            m_chatDialog.closeChatDialog();
        } else {
            m_chatDialog.openChatDialog();
        }
    }

    public void toggleInventoryVisible() {
        m_inventoryView.setVisible(!m_inventoryView.inventoryVisible);
    }

    /**
     * immediately hops into hosting and joining its own local server
     */
    private void hostAndJoin() {
        m_server = new OreServer();
        m_serverThread = new Thread(m_server, "main server thread");
        m_serverThread.start();

        try {
            //wait for the local server thread to report that it is live and running, before we attempt
            // a connection to it
            m_server.connectHostLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //call system, if returns false, fail and show:
        m_world = new OreWorld(this, m_server, OreWorld.WorldInstanceType.ClientHostingServer);
        m_world.init();
        m_world.m_artemisWorld.inject(this);

        m_networkClientSystem.addListener(new NetworkConnectListener(this));

        try {
            m_networkClientSystem.connect("127.0.0.1", Network.PORT);
        } catch (IOException e) {
            e.printStackTrace();
            //fuck. gonna have to show the fail to connect dialog.
            //could be a socket error..or anything, i guess
            System.exit(1);
        }
        //showFailToConnectDialog();
    }

    @Override
    public void dispose() {
        if (m_world != null) {
            m_world.shutdown();
        }
    }

    @Override
    public void render() {
        if (m_world != null) {
            m_world.process();
        }

        if (m_renderGui) {
            m_stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
            m_stage.draw();
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

    private void showFailToConnectDialog() {
        dialog = new Dialog("", m_skin, "dialog") {
            protected void result(Object object) {
                System.out.println("Chosen: " + object);
            }

        };
        TextButton dbutton = new TextButton("Yes", m_skin, "default");
        dialog.button(dbutton, true);

        dbutton = new TextButton("No", m_skin, "default");
        dialog.button(dbutton, false);
        dialog.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false);
        dialog.invalidateHierarchy();
        dialog.invalidate();
        dialog.layout();
        //m_stage.addActor(dialog);
        dialog.show(m_stage);

    }

    private void shutdown() {
        //only have to shutdown the server if it's a client-hosted server
        if (m_server != null) {
            m_server.shutdownLatch.countDown();
            try {
                //merge the server thread over, it should already have
                //gotten the shutdown signal
                m_serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Gdx.app.exit();
    }

    public void zoom(float factor) {
        if (m_world != null) {
            m_world.m_camera.zoom *= factor;
        }
    }

    @Override
    public void resize(int width, int height) {
        m_stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            shutdown();
        } else if (keycode == Input.Keys.F7) {
        } else if (keycode == Input.Keys.F8) {
            //fixme; this kind of stuff could be maybe put into a base interface which systems interested in input
            // could derive from. so we could just call this, and await the return...all of the debug things could be
            // handled
            //directly in there. but the question is, what to do for everything else.
            m_debugTextRenderSystem.m_renderDebugClient = !m_debugTextRenderSystem.m_renderDebugClient;
        } else if (keycode == Input.Keys.F9) {
            m_debugTextRenderSystem.m_renderDebugServer = !m_debugTextRenderSystem.m_renderDebugServer;
        } else if (keycode == Input.Keys.F10) {
            m_debugTextRenderSystem.m_renderTiles = !m_debugTextRenderSystem.m_renderTiles;
        } else if (keycode == Input.Keys.F11) {
            m_renderGui = !m_renderGui;
        } else if (keycode == Input.Keys.F12) {
            m_debugTextRenderSystem.m_guiDebug = !m_debugTextRenderSystem.m_guiDebug;
            m_stage.setDebugAll(m_debugTextRenderSystem.m_guiDebug);
        } else if (keycode == Input.Keys.I) {
            if (m_inventoryView != null) {
                m_inventoryView.setVisible(!m_inventoryView.inventoryVisible);
            }
        }

        //everything below here requires a world. it's terrible, i know...fixme
        if (m_world == null) {
            return false;
        }

        if (!m_networkClientSystem.connected) {
            return false;
        }

        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();
        ControllableComponent controllableComponent = controlMapper.get(player);

        if (keycode == Input.Keys.Q) {
            attemptItemDrop();
        } else if (keycode == Input.Keys.E) {
            //power overlay
            m_powerOverlayRenderSystem.toggleOverlay();
        } else if (keycode == Input.Keys.NUM_1) {
            m_hotbarInventory.selectSlot((byte) 0);
        } else if (keycode == Input.Keys.NUM_2) {
            m_hotbarInventory.selectSlot((byte) 1);
        } else if (keycode == Input.Keys.NUM_3) {
            m_hotbarInventory.selectSlot((byte) 2);
        } else if (keycode == Input.Keys.NUM_4) {
            m_hotbarInventory.selectSlot((byte) 3);
        } else if (keycode == Input.Keys.NUM_5) {
            m_hotbarInventory.selectSlot((byte) 4);
        } else if (keycode == Input.Keys.NUM_6) {
            m_hotbarInventory.selectSlot((byte) 5);
        } else if (keycode == Input.Keys.NUM_7) {
            m_hotbarInventory.selectSlot((byte) 6);
        } else if (keycode == Input.Keys.NUM_8) {
            m_hotbarInventory.selectSlot((byte) 7);
        }

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            controllableComponent.desiredDirection.x = -1;
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            controllableComponent.desiredDirection.x = 1;
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            controllableComponent.desiredDirection.y = -1;
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            controllableComponent.desiredDirection.y = 1;
        }

        if (keycode == Input.Keys.SPACE) {
            JumpComponent jumpComponent = jumpMapper.get(player);
            jumpComponent.shouldJump = true;
        }

        return true;
    }

    private void attemptItemDrop() {
        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();
        PlayerComponent playerComponent = playerMapper.get(player);

        if (playerComponent.getEquippedPrimaryItem() != OreWorld.ENTITY_INVALID) {
            Network.HotbarDropItemFromClient dropItemRequestFromClient = new Network.HotbarDropItemFromClient();
            int currentEquippedIndex = playerComponent.hotbarInventory.getSelectedSlot();
            dropItemRequestFromClient.index = (byte) currentEquippedIndex;

            // decrement count, we assume it'll get spawned shortly when the server tells us to.
            // delete in-inventory entity if necessary server assumes we already do so
            int itemEntity = playerComponent.getEquippedPrimaryItem();
            ItemComponent itemComponent = itemMapper.get(itemEntity);
            if (itemComponent.stackSize > 1) {
                //decrement count, server has already done so. we assume here that it went through properly.
                itemComponent.stackSize -= 1;
                m_hotbarInventory.setCount(currentEquippedIndex, itemComponent.stackSize);
            } else {
                //delete it, server knows/assumes we already did, since there are no more left
                int item = playerComponent.hotbarInventory.takeItem(dropItemRequestFromClient.index);
                m_world.m_artemisWorld.delete(item);
            }

            m_networkClientSystem.m_clientKryo.sendTCP(dropItemRequestFromClient);
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        if (m_world == null) {
            return false;
        }

        if (!m_networkClientSystem.connected) {
            return false;
        }

        int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

        ControllableComponent controllableComponent = controlMapper.get(player);

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            controllableComponent.desiredDirection.x = 0;
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            controllableComponent.desiredDirection.x = 0;
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            controllableComponent.desiredDirection.y = 0;
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            controllableComponent.desiredDirection.y = 0;
        }

        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        leftMouseDown = true;

        if (m_world != null) {
            return m_world.touchDown(screenX, screenY, pointer, button);
            //fixme

        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        leftMouseDown = false;
        if (m_world != null) {
            return m_world.touchUp(screenX, screenY, pointer, button);
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (m_world == null) {
            return false;
        }

        if (!m_networkClientSystem.connected) {
            return false;
        }

        if (m_powerOverlayRenderSystem.overlayVisible) {
            //don't allow item/inventory selection during this
            return false;
        }

        int index = m_hotbarInventory.getSelectedSlot();
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            m_hotbarInventory.selectSlot((byte) Math.min(index + 1, Inventory.maxHotbarSlots - 1));
        } else {
            //left
            m_hotbarInventory.selectSlot((byte) Math.max(index - 1, 0));
        }

        return true;
    }

    /**
     * @param playerName
     * @param connectionId
     * @param mainPlayer
     *         true if we should spawn our clients player (first player we get)
     *
     * @return
     */
    public int createPlayer(String playerName, int connectionId, boolean mainPlayer) {
        int player = m_world.createPlayer(playerName, connectionId);
        ControllableComponent controllableComponent = controlMapper.create(player);

        //only do this for the main player! each other player that gets spawned will not need this information, ever.
        PlayerComponent playerComponent = playerMapper.get(player);

        m_hotbarInventory = new Inventory(player, Inventory.InventoryType.Hotbar);
        m_world.m_artemisWorld.inject(m_hotbarInventory, true);
        playerComponent.hotbarInventory = m_hotbarInventory;

        m_hotbarInventory.addListener(new HotbarSlotListener());

        m_inventory = new Inventory(player, Inventory.InventoryType.Inventory);
        m_world.m_artemisWorld.inject(m_inventory, true);
        playerComponent.inventory = m_inventory;

        m_hotbarView = new HotbarInventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, m_world);
        m_inventoryView = new InventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, m_world);

        if (mainPlayer) {
            m_tagManager.register(OreWorld.s_mainPlayer, player);
        }

        //select the first slot, so the inventory view highlights something.
        playerComponent.hotbarInventory.selectSlot((byte) 0);

        //          SpriteComponent spriteComponent = spriteMapper.get(player);
        //        spriteComponent.sprite.setTexture();

        return player;
    }

    private class HotbarSlotListener implements Inventory.SlotListener {
        @Override
        public void selected(int index, @org.jetbrains.annotations.NotNull Inventory inventory) {
            assert m_world != null;

            int player = m_tagManager.getEntity(OreWorld.s_mainPlayer).getId();

            m_networkClientSystem.sendHotbarEquipped((byte) index);

            PlayerComponent playerComponent = playerMapper.get(player);
        }

        @Override
        public void countChanged(int index, @org.jetbrains.annotations.NotNull Inventory inventory) {

        }

        @Override
        public void set(int index, @org.jetbrains.annotations.NotNull Inventory inventory) {

        }

        @Override
        public void removed(int index, @org.jetbrains.annotations.NotNull Inventory inventory) {

        }
    }

    private static class NetworkConnectListener implements NetworkClientSystem.NetworkClientListener {
        private final OreClient m_client;

        @Override
        public void connected() {
            //todo surely there's some first-time connection stuff we must do?
        }

        @Override
        public void disconnected() {
            //todo show gui, say we've disconnected
        }

        public NetworkConnectListener(OreClient oreClient) {
            m_client = oreClient;
        }

    }

}
