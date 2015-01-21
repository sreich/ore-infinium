package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.ore.infinium.components.ControllableComponent;
import com.ore.infinium.components.PlayerComponent;
import com.ore.infinium.components.SpriteComponent;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OreClient implements ApplicationListener, InputProcessor {
    public final static int ORE_VERSION_MAJOR = 0;
    public final static int ORE_VERSION_MINOR = 1;
    public final static int ORE_VERSION_REVISION = 1;
    static OreTimer timer = new OreTimer();
    static String frameTimeString = "0";
    static String debugString1 = "F12 - gui debug";

    static DecimalFormat decimalFormat = new DecimalFormat("#.");

    public ConcurrentLinkedQueue<Object> m_netQueue = new ConcurrentLinkedQueue<>();
    FreeTypeFontGenerator m_fontGenerator;
    private World m_world;
    private Stage m_stage;
    private Table m_table;
    private Skin m_skin;
    private InputMultiplexer m_multiplexer;
    private ChatBox m_chat;
    private HotbarInventoryView m_hotbarView;
    private ScreenViewport m_viewport;
    private Entity m_mainPlayer;
    private Client m_clientKryo;
    private OreServer m_server;
    private Thread m_serverThread;
    private double m_accumulator;
    private double m_currentTime;
    private double m_step = 1.0 / 60.0;
    private SpriteBatch m_batch;
    private BitmapFont m_font;
    private Dialog dialog;
    private boolean m_guiDebug;

    private BitmapFont bitmapFont_8pt;

    @Override
    public void create() {
        //    Log.set(Log.LEVEL_DEBUG);
//        Log.set(Log.LEVEL_INFO);

        decimalFormat.setMaximumFractionDigits(9);

        m_batch = new SpriteBatch();
        m_font = new BitmapFont();
        m_font.setColor(26f / 255f, 152f / 255f, 1, 1);

        m_stage = new Stage(new StretchViewport(1600, 900));
        m_multiplexer = new InputMultiplexer(this, m_stage);

        GLProfiler.enable();

        m_viewport = new ScreenViewport();
        m_viewport.setScreenBounds(0, 0, 1600, 900);

        Gdx.input.setInputProcessor(m_multiplexer);

        m_fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 13;
        bitmapFont_8pt = m_fontGenerator.generateFont(parameter);

        m_skin = new Skin();
        m_skin.addRegions(new TextureAtlas(Gdx.files.internal("packed/ui.atlas")));
        m_skin.add("myfont", bitmapFont_8pt, BitmapFont.class);
        m_skin.load(Gdx.files.internal("ui/ui.json"));


        m_chat = new ChatBox(m_stage, m_skin);
        m_hotbarView = new HotbarInventoryView(m_stage, m_skin);

        TextButton button = new TextButton("click me", m_skin);
        TextButton button2 = new TextButton("click me2", m_skin);

        // Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
        // Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
        // ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
        // revert the checked state.
        button.addListener(new ChangeListener() {
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                System.out.println("Clicked! Is checked: " + button.isChecked());
                button.setText("Good job!");
            }
        });

        // Add an image actor. Have to set the size, else it would be the size of the drawable (which is the 1x1 texture).
//        table.add(new Image(m_skin.newDrawable("white", Color.RED))).size(64);

//
//        m_table = new Table();
//        m_stage.addActor(m_table);
//        m_table.setFillParent(true);
//
//        final ScrollPane scroll = new ScrollPane(m_table, m_skin);
//
//        m_table.debug();
//        m_table.add(button);
//        m_table.row();
//        m_table.add(button2);
//
//        m_table.row();
//        Label label1 = new Label("crap crap crap crap crap crap", m_skin, "default");
//        label1.setWrap(true);
//        m_table.add(label1);
//
//        m_table.row();
//        Label label2 = new Label("crap crap crap crap crap crapssssssssssssssssssssssssssssssssssssssssssss", m_skin, "default");
//        label2.setWrap(true);
//        label2.setWidth(100);
//        m_table.add(label2).width(100f);
//


//        Gdx.input.setInputProcessor(this);

        hostAndJoin();
    }

    private void hostAndJoin() {
        m_server = new OreServer();
        m_serverThread = new Thread(m_server);
        m_serverThread.start();

        try {
            m_server.latch.await();
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

        while (m_accumulator >= m_step) {
            m_accumulator -= m_step;
            //entityManager.update();
            processNetworkQueue();

            if (m_world != null) {
                m_world.update(m_step);
            }
        }

        double alpha = m_accumulator / m_step;

        //Gdx.app.log("frametime", Double.toString(frameTime));
        //Gdx.app.log("alpha", Double.toString(alpha));

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (m_world != null) {
            m_world.render(m_step);
        }

        m_stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        m_stage.draw();
//        try {
//            Thread.sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (timer.milliseconds() > 2000) {
            frameTimeString = "Frametime: " + decimalFormat.format(frameTime);
            timer.reset();
        }

        m_batch.begin();

        int textY = Gdx.graphics.getHeight() - 150;
        m_font.draw(m_batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 0, textY);
        textY -= 15;
        m_font.draw(m_batch, frameTimeString, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, debugString1, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, "Texture switches: " + GLProfiler.textureBindings, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, "Shader switches: " + GLProfiler.shaderSwitches, 0, textY);
        textY -= 15;
        m_font.draw(m_batch, "Draw calls: " + GLProfiler.drawCalls, 0, textY);
        m_batch.end();
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
            Gdx.app.exit();
        }

        if (keycode == Input.Keys.F12) {
            m_guiDebug = !m_guiDebug;
            m_stage.setDebugAll(m_guiDebug);
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
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
        return false;
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

    private Entity createPlayer(String playerName, int connectionId) {
        Entity player = m_world.createPlayer(playerName, connectionId);
        ControllableComponent controllableComponent = m_world.engine.createComponent(ControllableComponent.class);
        player.add(controllableComponent);

        m_world.engine.addEntity(player);

        return player;
    }

    private void processNetworkQueue() {
        for (Object object = m_netQueue.poll(); object != null; object = m_netQueue.poll()) {
            if (object instanceof Network.PlayerSpawnedFromServer) {

                Network.PlayerSpawnedFromServer spawn = (Network.PlayerSpawnedFromServer) object;
                assert m_world == null;

                m_world = new World(this, null);

                m_mainPlayer = createPlayer(spawn.playerName, m_clientKryo.getID());
                SpriteComponent spriteComp = Mappers.sprite.get(m_mainPlayer);

                spriteComp.sprite.setPosition(spawn.pos.pos.x, spawn.pos.pos.y);
                m_world.addPlayer(m_mainPlayer);
                m_world.initClient(m_mainPlayer);
            } else if (object instanceof Network.KickReason) {
                Network.KickReason reason = (Network.KickReason) object;
            } else if (object instanceof Network.BlockRegion) {
                Network.BlockRegion region = (Network.BlockRegion) object;
                m_world.loadBlockRegion(region);
            } else if (object instanceof Network.LoadedViewportMovedFromServer) {
                Network.LoadedViewportMovedFromServer v = (Network.LoadedViewportMovedFromServer) object;
                PlayerComponent c = Mappers.player.get(m_mainPlayer);
                c.loadedViewport.rect = v.rect;
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
}