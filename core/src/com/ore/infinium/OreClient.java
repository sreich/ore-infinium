package com.ore.infinium;

import com.artemis.ComponentMapper;
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
import com.ore.infinium.components.*;
import com.ore.infinium.systems.DebugTextRenderSystem;
import com.ore.infinium.systems.NetworkClientSystem;
import com.ore.infinium.systems.PowerOverlayRenderSystem;

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

    // zoom every n ms, while zoom key is held down
    private static final int zoomInterval = 30;
    private static OreTimer m_zoomTimer = new OreTimer();

    private InputMultiplexer m_multiplexer;

    private Stage m_stage;
    private Skin m_skin;

    public Chat m_chat;
    private Sidebar m_sidebar;

    private DragAndDrop m_dragAndDrop;

    private Dialog dialog;
    private ChatBox m_chatBox;
    private HotbarInventoryView m_hotbarView;
    private InventoryView m_inventoryView;

    public Inventory m_hotbarInventory;
    private Inventory m_inventory;

    private ScreenViewport m_viewport;

    private int m_mainPlayerEntity = OreWorld.ENTITY_INVALID;

    private OreServer m_server;
    private Thread m_serverThread;

    private boolean m_renderGui = true;

    private BitmapFont bitmapFont_8pt;

    /**
     * whether or not we're connected to the server (either local or mp).
     * This will only be true when the player has spawned. this means,
     * the server has spawned our player and the initial
     * player data has been sent back, indicating to the client that it has
     * been spawned, and under what player id.
     */
    public boolean connected;

    FreeTypeFontGenerator m_fontGenerator;

    @Override
    public void create() {
        // for debugging kryonet
        //    Log.set(Log.LEVEL_DEBUG);
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

        m_stage = new Stage(viewport = new StretchViewport(1600, 900));
        m_multiplexer = new InputMultiplexer(m_stage, this);

        m_viewport = new ScreenViewport();
        m_viewport.setScreenBounds(0, 0, 1600, 900);

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

        m_chatBox = new ChatBox(this, m_stage, m_skin);
        m_chat = new Chat();
        m_chat.addListener(m_chatBox);

        m_sidebar = new Sidebar(m_stage, m_skin, this);

        hostAndJoin();
    }

    public void handleLeftMousePrimaryAttack() {
        Vector2 mouse = m_world.mousePositionWorldCoords();

        PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);
        int itemEntity = playerComponent.getEquippedPrimaryItem();
        if (itemEntity == OreWorld.ENTITY_INVALID) {
            return;
        }

        ToolComponent toolComponent = toolMapper.getSafe(itemEntity);
        if (toolComponent != null) {
            if (toolComponent.type != ToolComponent.ToolType.Drill) {
                return;
            }

            int x = (int) (mouse.x / OreWorld.BLOCK_SIZE);
            int y = (int) (mouse.y / OreWorld.BLOCK_SIZE);

            Block block = m_world.blockAt(x, y);

            if (block.type != Block.BlockType.NullBlockType) {
                block.destroy();
                m_world.m_artemisWorld.getSystem(NetworkClientSystem.class).sendBlockPick(x, y);
            }

            //action performed
            return;
        }

        BlockComponent blockComponent = blockMapper.getSafe(itemEntity);
        if (blockComponent != null) {

            int x = (int) (mouse.x / OreWorld.BLOCK_SIZE);
            int y = (int) (mouse.y / OreWorld.BLOCK_SIZE);

            boolean blockPlaced = m_world.attemptBlockPlacement(x, y, blockComponent.blockType);
            if (blockPlaced) {
                m_world.m_artemisWorld.getSystem(NetworkClientSystem.class).sendBlockPlace(x, y);
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

    private void attemptItemPlace(float x, float y, int itemEntity) {

        //place the item
        int placedItemEntity = m_world.cloneEntity(itemEntity);

        ItemComponent placedItemComponent = itemMapper.get(placedItemEntity);

        placedItemComponent.state = ItemComponent.State.InWorldState;

        Vector2 alignedPosition = new Vector2(x, y);
        SpriteComponent spriteComponent = spriteMapper.get(placedItemEntity);
        m_world.alignPositionToBlocks(alignedPosition);

        spriteComponent.sprite.setPosition(alignedPosition.x, alignedPosition.y);

        if (m_world.isPlacementValid(placedItemEntity)) {
            //todo, do more validation..
            m_world.m_artemisWorld.getSystem(NetworkClientSystem.class)
                                  .sendItemPlace(alignedPosition.x, alignedPosition.y);
        } else {
            //fixme i know, it isn't ideal..i technically add the item anyways and delete it if it cannot be placed
            //because the function actually takes only the entity, to check if its size, position etc conflict with
            // anything

            //engine.removeEntity(placedItemEntity);
        }
    }

    public void toggleChatVisible() {
        if (m_chatBox.chatVisibilityState == ChatBox.ChatVisibility.Normal) {
            m_chatBox.closeChatDialog();
        } else {
            m_chatBox.openChatDialog();
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
        m_world = new OreWorld(this, null);
        m_world.m_artemisWorld.inject(this);

        m_world.m_artemisWorld.getSystem(NetworkClientSystem.class).addListener(new NetworkConnectListener(this));
        m_world.m_artemisWorld.getSystem(NetworkClientSystem.class).connect("127.0.0.1", Network.port);
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
        if (m_renderGui) {
            m_stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
            m_stage.draw();
        }

        if (m_world != null) {
            m_world.process();
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
            m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderDebugClient =
                    !m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderDebugClient;
        } else if (keycode == Input.Keys.F9) {
            m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderDebugServer =
                    !m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderDebugServer;
        } else if (keycode == Input.Keys.F10) {
            m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderTiles =
                    !m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_renderTiles;
        } else if (keycode == Input.Keys.F11) {
            m_renderGui = !m_renderGui;
        } else if (keycode == Input.Keys.F12) {
            m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_guiDebug =
                    !m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_guiDebug;
            m_stage.setDebugAll(m_world.m_artemisWorld.getSystem(DebugTextRenderSystem.class).m_guiDebug);
        } else if (keycode == Input.Keys.I) {
            if (m_inventoryView != null) {
                m_inventoryView.setVisible(!m_inventoryView.inventoryVisible);
            }
        }

        //fixme, ERROR
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

        if (m_mainPlayerEntity == OreWorld.ENTITY_INVALID) {
            return false;
        }

        ControllableComponent controllableComponent = controlMapper.get(m_mainPlayerEntity);

        if (keycode == Input.Keys.Q) {

            PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);

            if (playerComponent.getEquippedPrimaryItem() != OreWorld.ENTITY_INVALID) {
                Network.HotbarDropItemRequestFromClient dropItemRequestFromClient =
                        new Network.HotbarDropItemRequestFromClient();
                dropItemRequestFromClient.index = playerComponent.hotbarInventory.m_selectedSlot;
                // decrement count, we assume it'll get spawned shortly. delete in-inventory entity if necessary
                // server assumes we already do so
                int itemEntity = playerComponent.getEquippedPrimaryItem();
                ItemComponent itemComponent = itemMapper.get(itemEntity);
                if (itemComponent.stackSize > 1) {
                    //decrement count, server has already done so. we assume here that it went through properly.
                    itemComponent.stackSize -= 1;
                } else {
                    int item = playerComponent.hotbarInventory.takeItem(dropItemRequestFromClient.index);
                    m_world.m_artemisWorld.delete(item);
                }

                //fixme
                //m_clientKryo.sendTCP(dropItemRequestFromClient);

            }
        } else if (keycode == Input.Keys.E) {
            //power overlay
            m_world.m_artemisWorld.getSystem(PowerOverlayRenderSystem.class).overlayVisible =
                    !m_world.m_artemisWorld.getSystem(PowerOverlayRenderSystem.class).overlayVisible;

            //fixmeasap
            //            if (m_world.m_itemPlacementOverlayEntity != OreWorld.ENTITY_INVALID) {
            //                spriteMapper.get(m_world.m_itemPlacementOverlayEntity).visible =
            //                        !m_world.m_artemisWorld.getSystem(PowerOverlaySystem.class).overlayVisible;
            //            }
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

        if (keycode == Input.Keys.UP) {

        }

        if (keycode == Input.Keys.DOWN) {

        }

        if (keycode == Input.Keys.SPACE) {
            JumpComponent jumpComponent = jumpMapper.get(m_mainPlayerEntity);
            jumpComponent.shouldJump = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (m_mainPlayerEntity == OreWorld.ENTITY_INVALID) {
            return false;
        }

        ControllableComponent controllableComponent = controlMapper.get(m_mainPlayerEntity);

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            controllableComponent.desiredDirection.x = 0;
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            controllableComponent.desiredDirection.x = 0;
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
        if (m_mainPlayerEntity == OreWorld.ENTITY_INVALID) {
            return false;
        }

        int index = m_hotbarInventory.m_selectedSlot;
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            m_hotbarInventory.selectSlot((byte) Math.min(index + 1, Inventory.maxHotbarSlots - 1));
        } else {
            //left
            m_hotbarInventory.selectSlot((byte) Math.max(index - 1, 0));
        }

        return true;
    }

    public int createPlayer(String playerName, int connectionId) {
        int player = m_world.createPlayer(playerName, connectionId);
        ControllableComponent controllableComponent = controlMapper.create(player);

        //only do this for the main player! each other player that gets spawned will not need this information, ever.
        if (m_mainPlayerEntity == OreWorld.ENTITY_INVALID) {
            PlayerComponent playerComponent = playerMapper.get(player);

            m_hotbarInventory = new Inventory(player);
            m_hotbarInventory.inventoryType = Inventory.InventoryType.Hotbar;
            playerComponent.hotbarInventory = m_hotbarInventory;

            m_hotbarInventory.addListener(new HotbarSlotListener());

            m_inventory = new Inventory(player);
            m_inventory.inventoryType = Inventory.InventoryType.Inventory;
            playerComponent.inventory = m_inventory;

            m_hotbarView =
                    new HotbarInventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, m_world);
            m_inventoryView =
                    new InventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, m_world);

            playerComponent.hotbarInventory.selectSlot((byte) 0);
        }

        //          SpriteComponent spriteComponent = spriteMapper.get(player);
        //        spriteComponent.sprite.setTexture();

        return player;
    }

    private class HotbarSlotListener implements Inventory.SlotListener {
        @Override
        public void countChanged(byte index, Inventory inventory) {

        }

        @Override
        public void set(byte index, Inventory inventory) {

        }

        @Override
        public void removed(byte index, Inventory inventory) {

        }

        @Override
        public void selected(byte index, Inventory inventory) {
            if (m_mainPlayerEntity == OreWorld.ENTITY_INVALID) {
                return;
            }

            m_world.m_artemisWorld.getSystem(NetworkClientSystem.class).sendHotbarEquipped(index);
            PlayerComponent playerComponent = playerMapper.get(m_mainPlayerEntity);

            m_world.clientHotbarInventoryItemSelected();
        }
    }

    private static class NetworkConnectListener implements NetworkClientSystem.NetworkClientListener {
        private final OreClient m_client;

        @Override
        public void connected() {
            m_client.connected = true;

        }

        @Override
        public void disconnected() {
            //todo show gui, say we've disconnected

            m_client.connected = false;
        }

        public NetworkConnectListener(OreClient oreClient) {
            m_client = oreClient;
        }

    }
}
