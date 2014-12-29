package com.ore.infinium;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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

import java.io.IOException;

public class OreClient implements ApplicationListener, InputProcessor {
	private World m_world;
	private Stage m_stage;
	private Table m_table;
	private Skin m_skin;
	private ScreenViewport m_viewport;
	private FPSLogger m_fpsLogger;

	private Client m_clientKryo;

	private OreServer m_server;
	private Thread m_serverThread;
	private double m_accumulator;
	private double m_currentTime;
	private double m_step = 1.0 / 60.0;

	@Override
	public void create() {
		//Log.set(Log.LEVEL_DEBUG);
		m_stage = new Stage(new StretchViewport(1600, 900));
		m_fpsLogger = new FPSLogger();

		m_viewport = new ScreenViewport();
		m_viewport.setScreenBounds(0, 0, 1600, 900);

		Gdx.input.setInputProcessor(m_stage);

		// A skin can be loaded via JSON or defined programmatically, either is fine. Using a skin is optional but strongly
		// recommended solely for the convenience of getting a texture, region, etc as a drawable, tinted drawable, etc.
		m_skin = new Skin();

		// Generate a 1x1 white texture and store it in the skin named "white".
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();
		m_skin.add("white", new Texture(pixmap));

		// Store the default libgdx font under the name "default".
		m_skin.add("default", new BitmapFont());

		// Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
		TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
		textButtonStyle.up = m_skin.newDrawable("white", Color.DARK_GRAY);
		textButtonStyle.down = m_skin.newDrawable("white", Color.DARK_GRAY);
		textButtonStyle.checked = m_skin.newDrawable("white", Color.BLUE);
		textButtonStyle.over = m_skin.newDrawable("white", Color.LIGHT_GRAY);
		textButtonStyle.font = m_skin.getFont("default");
		m_skin.add("default", textButtonStyle);

		// Create a table that fills the screen. Everything else will go inside this table.
		Table table = new Table();
		table.setFillParent(true);
		m_stage.addActor(table);

		// Create a button with the "default" TextButtonStyle. A 3rd parameter can be used to specify a name other than "default".
		final TextButton button = new TextButton("Click me!", m_skin);
		table.add(button);

		// Add a listener to the button. ChangeListener is fired when the button's checked state changes, eg when clicked,
		// Button#setChecked() is called, via a key press, etc. If the event.cancel() is called, the checked state will be reverted.
		// ClickListener could have been used, but would only fire when clicked. Also, canceling a ClickListener event won't
		// revert the checked state.
		button.addListener(new ChangeListener() {
			public void changed (ChangeListener.ChangeEvent event, Actor actor) {
				System.out.println("Clicked! Is checked: " + button.isChecked());
				button.setText("Good job!");
			}
		});

		// Add an image actor. Have to set the size, else it would be the size of the drawable (which is the 1x1 texture).
		table.add(new Image(m_skin.newDrawable("white", Color.RED))).size(64);

		m_table = new Table();
		m_stage.addActor(m_table);
		m_table.setFillParent(true);

		m_table.debug();
		m_table.defaults().space(6);
//		m_table.add(label);
		m_table.add(button);


		Gdx.input.setInputProcessor(this);

		hostAndJoin();

		m_world = new World();
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

		m_clientKryo = new Client();
		m_clientKryo.start();

		Network.register(m_clientKryo);

		m_clientKryo.addListener(new ClientListener());

   new Thread("Connect") {
                        public void run () {
                                try {
                                        m_clientKryo.connect(5000, "127.0.0.1", Network.port);
                                        // Server communication after connection can go here, or in Listener#connected().
                                } catch (IOException ex) {
                                        ex.printStackTrace();
                                        System.exit(1);
                                }
                        }
                }.start();
	}

	@Override
	public void dispose() {
		m_world.dispose();
	}

	@Override
	public void render() {
		m_fpsLogger.log();

		double newTime = TimeUtils.millis() / 1000.0;
		double frameTime = Math.min(newTime - m_currentTime, 0.25);
		double deltaTime = frameTime;

		m_accumulator += frameTime;

		m_currentTime = newTime;

		while (m_accumulator >= m_step) {
			m_accumulator -= m_step;
			//entityManager.update();
		}

		double alpha = m_accumulator / m_step;

//		try {
//			m_clientKryo.update(0);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		//Gdx.app.log("frametime", Double.toString(frameTime));
		//Gdx.app.log("alpha", Double.toString(alpha));

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

//		m_camera

//		m_viewport.apply();

		m_world.render(frameTime);
		m_stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
		m_stage.draw();
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

		if (keycode == Input.Keys.MINUS) {
			//zoom out
			m_world.zoom(1.1f);
		}

		if (keycode == Input.Keys.EQUALS) {
			m_world.zoom(0.9f);
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

	class ClientListener extends Listener {
		public void connected(Connection connection) {
			Network.RegisterName registerName = new Network.RegisterName();
			registerName.name = "testname";
			m_clientKryo.sendTCP(registerName);
		}

		public void received(Connection connection, Object object) {
			// if (object instanceof ChatMessage) {
			//         ChatMessage chatMessage = (ChatMessage)object;
			//         chatFrame.addMessage(chatMessage.text);
			//         return;
			// }
		}

		public void disconnected(Connection connection) {

		}
	}
}