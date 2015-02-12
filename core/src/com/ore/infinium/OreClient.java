package com.ore.infinium;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.ore.infinium.components.*;
import com.ore.infinium.systems.TileRenderer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OreClient implements ApplicationListener, InputProcessor {
    public final static int ORE_VERSION_MAJOR = 0;
    public final static int ORE_VERSION_MINOR = 1;
    public final static int ORE_VERSION_REVISION = 1;

    static final String debugString1 = "F12 - gui debug";
    static final String debugString2 = "F11 - gui render toggle";
    static final String debugString3 = "F10 - tile render toggle";
    static final String debugString4 = "F9 - client/server sync debug render toggle";

    static OreTimer frameTimer = new OreTimer();
    static String frameTimeString = "";
    static String fpsString = "";
    static String textureSwitchesString = "";
    static String shaderSwitchesString = "";
    static String drawCallsString = "";
    static DecimalFormat decimalFormat = new DecimalFormat("#.");

    private SpriteBatch m_debugServerBatch;

    public boolean m_renderTiles = true;
    private boolean m_guiDebug;
    private boolean m_renderGui = true;
    private boolean m_renderDebugServer = false;

    private ConcurrentLinkedQueue<Object> m_netQueue = new ConcurrentLinkedQueue<>();
    FreeTypeFontGenerator m_fontGenerator;
    private Stage m_stage;
    private Skin m_skin;

    private InputMultiplexer m_multiplexer;

    private ChatBox m_chatBox;
    private Chat m_chat;
    private Dialog dialog;
    private Sidebar m_sidebar;

    private HotbarInventoryView m_hotbarView;
    private InventoryView m_inventoryView;
    private Inventory m_hotbarInventory;
    private Inventory m_inventory;

    private ScreenViewport m_viewport;
    protected World m_world;
    private Entity m_mainPlayer;
    private Client m_clientKryo;
    private OreServer m_server;
    private Thread m_serverThread;

    private double m_accumulator;
    private double m_currentTime;
    private double m_step = 1.0 / 60.0;

    private SpriteBatch m_batch;
    private BitmapFont m_font;
    private BitmapFont bitmapFont_8pt;

    public boolean leftMouseDown;

    private DragAndDrop m_dragAndDrop;

    private Texture junktexture;

    // the internal (client) entity for the network(server's) entity ID
    private LongMap<Entity> m_entityForNetworkId = new LongMap<>(500);
    // map to reverse lookup, the long (server) entity ID for the given Entity
    private ObjectMap<Entity, Long> m_networkIdForEntityId = new ObjectMap<>(500);

    @Override
    public void create() {
        //    Log.set(Log.LEVEL_DEBUG);
//        Log.set(Log.LEVEL_INF
//        ProgressBar progressBar = new ProgressBar(0, 100, 10, false, m_skin);
//        progressBar.setValue(50);
//        progressBar.getStyle().knobBefore = progressBar.getStyle().knob;
//        progressBar.getStyle().knob.setMinHeight(50);
//        container.add(progressBar);

        m_dragAndDrop = new DragAndDrop();
        decimalFormat.setMaximumFractionDigits(9);

        m_batch = new SpriteBatch();

        m_stage = new Stage(new StretchViewport(1600, 900));
        m_multiplexer = new InputMultiplexer(m_stage, this);

        GLProfiler.enable();

        m_viewport = new ScreenViewport();
        m_viewport.setScreenBounds(0, 0, 1600, 900);

        Gdx.input.setInputProcessor(m_multiplexer);

        m_fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 13;
        bitmapFont_8pt = m_fontGenerator.generateFont(parameter);

        parameter.size = 9;
        m_font = m_fontGenerator.generateFont(parameter);
//        m_font.setColor(26f / 255f, 152f / 255f, 1, 1);
        m_font.setColor(234f / 255f, 28f / 255f, 164f / 255f, 1);

        m_fontGenerator.dispose();

        m_skin = new Skin();
        m_skin.addRegions(new TextureAtlas(Gdx.files.internal("packed/ui.atlas")));
        m_skin.add("myfont", bitmapFont_8pt, BitmapFont.class);
        m_skin.load(Gdx.files.internal("ui/ui.json"));

        m_chatBox = new ChatBox(this, m_stage, m_skin);
        m_chat = new Chat();
        m_chat.addListener(m_chatBox);

        m_sidebar = new Sidebar(m_stage, m_skin, this);

        junktexture = new Texture(Gdx.files.internal("entities/debug.png"));
        m_debugServerBatch = new SpriteBatch();

        hostAndJoin();
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

    private void hostAndJoin() {
        m_server = new OreServer();
        m_serverThread = new Thread(m_server);
        m_serverThread.start();

        try {
            m_server.connectHostLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        m_clientKryo = new Client(8192, 65536);
        m_clientKryo.start();

        Network.register(m_clientKryo);

        m_clientKryo.addListener(new ClientListener(this));
        m_clientKryo.setKeepAliveTCP(999999);

        new Thread("Connect") {
            public void run() {
                try {
                    m_clientKryo.connect(99999999 /*HACK, debug*/, "127.0.0.1", Network.port);
                    // Server communication after connection can go here, or in Listener#connected().

                    Network.InitialClientData initialClientData = new Network.InitialClientData();

                    initialClientData.playerName = "testname";

                    //TODO generate some random thing
                    initialClientData.playerUUID = UUID.randomUUID().toString();
                    initialClientData.versionMajor = ORE_VERSION_MAJOR;
                    initialClientData.versionMinor = ORE_VERSION_MINOR;
                    initialClientData.versionRevision = ORE_VERSION_REVISION;

                    m_clientKryo.sendTCP(initialClientData);
                } catch (IOException ex) {

                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        }.start();
        //showFailToConnectDialog();
    }

    @Override
    public void dispose() {
        if (m_world != null) {
            m_world.dispose();
        }
    }

    @Override
    public void render() {

        double newTime = TimeUtils.millis() / 1000.0;
        double frameTime = Math.min(newTime - m_currentTime, 1.0 / 15.0);

        m_accumulator += frameTime;

        m_currentTime = newTime;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        while (m_accumulator >= m_step) {
            m_accumulator -= m_step;
            //entityManager.update();
            processNetworkQueue();

        }

        if (m_world != null) {
            //on client, actually renders, due to systems :S
            //fixme we want to split rendering and updating..its coupled right now
            m_world.update(m_step);
        }

        double alpha = m_accumulator / m_step;

        //Gdx.app.log("frametime", Double.toString(frameTime));
        //Gdx.app.log("alpha", Double.toString(alpha));


        if (m_world != null) {
            m_world.render(m_step);
        }

        if (m_renderGui) {
            m_stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
            m_stage.draw();
        }

        if (frameTimer.milliseconds() > 1000) {
            frameTimeString = "Frametime: " + decimalFormat.format(frameTime);
            fpsString = "FPS: " + Gdx.graphics.getFramesPerSecond();
            textureSwitchesString = "Texture switches: " + GLProfiler.textureBindings;
            shaderSwitchesString = "Shader switches: " + GLProfiler.shaderSwitches;
            drawCallsString = "Draw calls: " + GLProfiler.drawCalls;

            frameTimer.reset();
        }

        m_batch.begin();

        int textY = Gdx.graphics.getHeight() - 130;
        m_font.draw(m_batch, fpsString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, frameTimeString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, debugString1, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, debugString2, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, debugString3, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, debugString4, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, "tiles rendered: " + TileRenderer.tileCount, 0, textY);
        textY -= 15;

        if (m_world != null) {
            m_font.draw(m_batch, "client entities: " + m_world.engine.getEntities().size(), 0, textY);

            if (m_server != null) {
                textY -= 15;
                m_font.draw(m_batch, "server entities: " + m_server.m_world.engine.getEntities().size(), 0, textY);

            }
        }

        m_batch.end();

        if (m_world != null && m_renderDebugServer && false) {
            m_debugServerBatch.setProjectionMatrix(m_world.m_camera.combined);
            m_debugServerBatch.begin();
            m_debugServerBatch.setColor(1, 0, 0, 0.5f);
            ImmutableArray<Entity> entities = m_server.m_world.engine.getEntitiesFor(Family.all(SpriteComponent.class).get());
            for (Entity e : entities) {
                SpriteComponent spriteComponent = Mappers.sprite.get(e);

                m_debugServerBatch.draw(junktexture, spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                        spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                        spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());
            }

            m_debugServerBatch.end();
        }

        GLProfiler.reset();

        //try {
        //    int sleep = (int)Math.max(newTime + m_step - TimeUtils.millis()/1000.0, 0.0);
        //    Gdx.app.log("", "sleep amnt: " + sleep);
        //    Thread.sleep(sleep);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}
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
        if (m_server != null) {
            m_server.shutdownLatch.countDown();
            try {
                m_serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Gdx.app.exit();
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
        } else if (keycode == Input.Keys.F9) {
            m_renderDebugServer = !m_renderDebugServer;
        } else if (keycode == Input.Keys.F10) {
            m_renderTiles = !m_renderTiles;
        } else if (keycode == Input.Keys.F11) {
            m_renderGui = !m_renderGui;
        } else if (keycode == Input.Keys.F12) {
            m_guiDebug = !m_guiDebug;
            m_stage.setDebugAll(m_guiDebug);
        } else if (keycode == Input.Keys.I) {
            if (m_inventoryView != null) {
                m_inventoryView.setVisible(!m_inventoryView.inventoryVisible);
            }
        }

        if (m_mainPlayer == null) {
            return false;
        }

        ControllableComponent controllableComponent = Mappers.control.get(m_mainPlayer);

        if (keycode == Input.Keys.Q) {

            PlayerComponent playerComponent = Mappers.player.get(m_mainPlayer);

            if (playerComponent.equippedPrimaryItem() != null) {
                Network.HotbarDropItemRequestFromClient dropItemRequestFromClient = new Network.HotbarDropItemRequestFromClient();
                dropItemRequestFromClient.index = playerComponent.hotbarInventory.m_selectedSlot;
                // decrement count, we assume it'll get spawned shortly. delete in-inventory entity if necessary
                // server assumes we already do so
                Entity item = playerComponent.equippedPrimaryItem();
                ItemComponent itemComponent = Mappers.item.get(item);
                if (itemComponent.stackSize > 1) {
                    //decrement count, server has already done so. we assume here that it went through properly.
                    itemComponent.stackSize -= 1;
                } else {
                    m_world.engine.removeEntity(playerComponent.hotbarInventory.takeItem(dropItemRequestFromClient.index));
                }

                m_clientKryo.sendTCP(dropItemRequestFromClient);

            }
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
            JumpComponent jumpComponent = Mappers.jump.get(m_mainPlayer);
            jumpComponent.shouldJump = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (m_mainPlayer == null) {
            return false;
        }

        ControllableComponent controllableComponent = Mappers.control.get(m_mainPlayer);

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
        Gdx.app.log("", "mousedown");
        leftMouseDown = true;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        leftMouseDown = false;
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
        if (m_mainPlayer == null) {
            return false;
        }

        int index = m_hotbarInventory.m_selectedSlot;
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            m_hotbarInventory.selectSlot((byte) Math.min(index + 1, m_hotbarInventory.maxHotbarSlots - 1));
        } else {
            //left
            m_hotbarInventory.selectSlot((byte) Math.max(index - 1, 0));
        }

        return true;
    }

    public void sendInventoryMove(Inventory.InventoryType sourceInventoryType, byte sourceIndex, Inventory.InventoryType destInventoryType, byte destIndex) {
        Network.PlayerMoveInventoryItemFromClient inventoryItemFromClient = new Network.PlayerMoveInventoryItemFromClient();
        inventoryItemFromClient.sourceType = sourceInventoryType;
        inventoryItemFromClient.sourceIndex = sourceIndex;
        inventoryItemFromClient.destType = destInventoryType;
        inventoryItemFromClient.destIndex = destIndex;

        m_clientKryo.sendTCP(inventoryItemFromClient);
    }

    /**
     * Send the command indicating (main) player moved to position
     */
    public void sendPlayerMoved() {
        SpriteComponent sprite = Mappers.sprite.get(m_mainPlayer);

        Network.PlayerMoveFromClient move = new Network.PlayerMoveFromClient();
        move.position = new Vector2(sprite.sprite.getX(), sprite.sprite.getY());

        m_clientKryo.sendTCP(move);
    }

    public void sendChatMessage(String message) {
        Network.ChatMessageFromClient chatMessageFromClient = new Network.ChatMessageFromClient();
        chatMessageFromClient.message = message;

        m_clientKryo.sendTCP(chatMessageFromClient);
    }

    private Entity createPlayer(String playerName, int connectionId) {
        Entity player = m_world.createPlayer(playerName, connectionId);
        ControllableComponent controllableComponent = m_world.engine.createComponent(ControllableComponent.class);
        player.add(controllableComponent);

        //only do this for the main player! each other player that gets spawned will not need this information, ever.
        if (m_mainPlayer == null) {
            PlayerComponent playerComponent = Mappers.player.get(player);

            m_hotbarInventory = new Inventory(player);
            m_hotbarInventory.inventoryType = Inventory.InventoryType.Hotbar;
            playerComponent.hotbarInventory = m_hotbarInventory;

            m_hotbarInventory.addListener(new HotbarSlotListener());

            m_inventory = new Inventory(player);
            m_inventory.inventoryType = Inventory.InventoryType.Inventory;
            playerComponent.inventory = m_inventory;

            m_hotbarView = new HotbarInventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, this);
            m_inventoryView = new InventoryView(m_stage, m_skin, m_hotbarInventory, m_inventory, m_dragAndDrop, this);

            playerComponent.hotbarInventory.selectSlot((byte) 0);
        }

//          SpriteComponent spriteComponent = Mappers.sprite.get(player);
//        spriteComponent.sprite.setTexture();

        m_world.engine.addEntity(player);

        return player;
    }

    private void sendHotbarEquipped(byte index) {
        Network.PlayerEquipHotbarIndexFromClient playerEquipHotbarIndexFromClient = new Network.PlayerEquipHotbarIndexFromClient();
        playerEquipHotbarIndexFromClient.index = index;

        m_clientKryo.sendTCP(playerEquipHotbarIndexFromClient);
    }

    private void processNetworkQueue() {
        for (Object object = m_netQueue.poll(); object != null; object = m_netQueue.poll()) {
            if (object instanceof Network.PlayerSpawnedFromServer) {

                Network.PlayerSpawnedFromServer spawn = (Network.PlayerSpawnedFromServer) object;

                if (m_mainPlayer == null) {

                    m_world = new World(this, null);

                    m_mainPlayer = createPlayer(spawn.playerName, m_clientKryo.getID());
                    SpriteComponent spriteComp = Mappers.sprite.get(m_mainPlayer);

                    spriteComp.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y);
                    m_world.addPlayer(m_mainPlayer);
                    m_world.initClient(m_mainPlayer);

                    m_world.engine.addEntityListener(new ClientEntityListener());
                } else {
                    //FIXME cover other players joining case
                    assert false;
                }
            } else if (object instanceof Network.KickReason) {
                Network.KickReason reason = (Network.KickReason) object;
            } else if (object instanceof Network.BlockRegion) {
                Network.BlockRegion region = (Network.BlockRegion) object;
                m_world.loadBlockRegion(region);
            } else if (object instanceof Network.LoadedViewportMovedFromServer) {
                Network.LoadedViewportMovedFromServer v = (Network.LoadedViewportMovedFromServer) object;
                PlayerComponent c = Mappers.player.get(m_mainPlayer);
                c.loadedViewport.rect = v.rect;
            } else if (object instanceof Network.PlayerSpawnHotbarInventoryItemFromServer) {
                Network.PlayerSpawnHotbarInventoryItemFromServer spawn = (Network.PlayerSpawnHotbarInventoryItemFromServer) object;

                //HACK spawn.id, sprite!!
                Entity e = m_world.engine.createEntity();
                for (Component c : spawn.components) {
                    e.add(c);
                }

                SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
                spriteComponent.textureName = spawn.textureName;
                spriteComponent.sprite.setSize(spawn.size.size.x, spawn.size.size.y);

                TextureRegion textureRegion;
                if (Mappers.block.get(e) == null) {
                    textureRegion = m_world.m_atlas.findRegion(spriteComponent.textureName);
                } else {
                    textureRegion = m_world.m_tileRenderer.m_atlas.findRegion(spriteComponent.textureName);
                }

                spriteComponent.sprite.setRegion(textureRegion);
                e.add(spriteComponent);

                m_world.engine.addEntity(e);

                ToolComponent toolComponent = Mappers.tool.get(e);

                ItemComponent itemComponent = Mappers.item.get(e);
                m_hotbarInventory.setSlot(itemComponent.inventoryIndex, e);

                //TODO i wonder if i can implement my own serializer (trivially!) and make it use the entity/component pool. look into kryo itself, you can override creation (easily i hope), per class

            } else if (object instanceof Network.EntitySpawnFromServer) {
                //fixme this and hotbar code needs consolidation
                Network.EntitySpawnFromServer spawn = (Network.EntitySpawnFromServer) object;

                Entity e = m_world.engine.createEntity();
                for (Component c : spawn.components) {
                    e.add(c);
                }

                //hack id..see above.
                SpriteComponent spriteComponent = m_world.engine.createComponent(SpriteComponent.class);
                spriteComponent.textureName = spawn.textureName;
                spriteComponent.sprite.setSize(spawn.size.size.x, spawn.size.size.y);
                spriteComponent.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y);

                TextureRegion textureRegion;
                if (Mappers.block.get(e) == null) {
                    textureRegion = m_world.m_atlas.findRegion(spriteComponent.textureName);
                } else {
                    textureRegion = m_world.m_tileRenderer.m_atlas.findRegion(spriteComponent.textureName);
                }

                spriteComponent.sprite.setRegion(textureRegion);
                e.add(spriteComponent);

                m_networkIdForEntityId.put(e, spawn.id);
                m_entityForNetworkId.put(spawn.id, e);

                m_world.engine.addEntity(e);
            } else if (object instanceof Network.ChatMessageFromServer) {
                Network.ChatMessageFromServer data = (Network.ChatMessageFromServer) object;
                m_chat.addChatLine(data.timestamp, data.playerName, data.message, data.sender);
            } else if (object instanceof Network.EntityMovedFromServer) {
                Network.EntityMovedFromServer data = (Network.EntityMovedFromServer) object;
                Entity entity = m_entityForNetworkId.get(data.id);
                assert entity != null;

                SpriteComponent spriteComponent = Mappers.sprite.get(entity);
                spriteComponent.sprite.setPosition(data.position.x, data.position.y);
            } else {
                if (!(object instanceof FrameworkMessage.KeepAlive)) {
                    Gdx.app.log("client network", "unhandled network receiving class");
                    assert false;
                }
            }

            // if (object instanceof ChatMessage) {
            //         ChatMessage chatMessage = (ChatMessage)object;
            //         chatFrame.addMessage(chatMessage.text);
            //         return;
            // }
        }
    }

    class ClientListener extends Listener {
        private OreClient m_client;

        ClientListener(OreClient client) {
            m_client = client;

        }

        public void connected(Connection connection) {
            connection.setTimeout(999999999);
        }

        //FIXME: do sanity checking (null etc) on both client, server
        public void received(Connection connection, Object object) {
            m_netQueue.add(object);
        }

        public void disconnected(Connection connection) {
        }
    }

    public void sendBlockPick(int x, int y) {
        Network.BlockPickFromClient blockPickFromClient = new Network.BlockPickFromClient();
        blockPickFromClient.x = x;
        blockPickFromClient.y = y;
        m_clientKryo.sendTCP(blockPickFromClient);
    }

    public void sendBlockPlace(int x, int y) {
        Network.BlockPlaceFromClient blockPlaceFromClient = new Network.BlockPlaceFromClient();
        blockPlaceFromClient.x = x;
        blockPlaceFromClient.y = y;
        m_clientKryo.sendTCP(blockPlaceFromClient);
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
            if (m_mainPlayer == null) {
                return;
            }

            sendHotbarEquipped(index);
            PlayerComponent playerComponent = Mappers.player.get(m_mainPlayer);

            Entity itemCopy = playerComponent.equippedPrimaryItem();
            playerComponent.equippedItemAnimator = itemCopy;

            m_world.clientInventoryItemSelected();
        }
    }

    private class ClientEntityListener implements EntityListener {
        @Override
        public void entityAdded(Entity entity) {

        }

        @Override
        public void entityRemoved(Entity entity) {
            Long networkId = m_networkIdForEntityId.remove(entity);
            if (networkId != null) {
                //a local only thing, like crosshair etc
                m_entityForNetworkId.remove(networkId);
            }
        }
    }

}