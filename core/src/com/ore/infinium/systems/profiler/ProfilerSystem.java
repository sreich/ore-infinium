package com.ore.infinium.systems.profiler;

import com.artemis.BaseSystem;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.ore.infinium.systems.RenderSystemMarker;

/**
 * Example profiling system.
 *
 * @author piotr-j
 * @author Daan van Yperen
 */
@Wire
public class ProfilerSystem extends BaseSystem implements RenderSystemMarker {

    public static final int TOGGLE_PROFILER_KEY = Input.Keys.P;

    OrthographicCamera camera;
    ShapeRenderer renderer;
    Stage stage;
    Skin m_skin;

    SystemProfilerGUI gui;
    private boolean f3ButtonDown;

    public ProfilerSystem(Skin skin) {
        m_skin = skin;
    }

    @Override
    protected void initialize() {

        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setToOrtho(false);
        camera.update();
        renderer = new ShapeRenderer();
        stage = new Stage();
        stage.getBatch().setProjectionMatrix(camera.combined);
        //m_skin = new Skin(Gdx.files.classpath("net/mostlyoriginal/plugin/profiler/skin/uiskin.json"));

        // setup some static config like colors etc
        SystemProfilerGUI.GRAPH_H_LINE.set(Color.ORANGE);
        gui = new SystemProfilerGUI(m_skin, "default");
        gui.setResizeBorder(8);
        gui.show(stage);
        gui.setWidth(Gdx.graphics.getWidth());
    }

    @Override
    protected void processSystem() {
        if (!isEnabled() || !isConfigured()) {
            return;
        }

        checkActivationButton();

        if (SystemProfiler.isRunning()) {
            processInput();
            render();
        }
    }

    private boolean isConfigured() {
        return gui.getParent() != null;
    }

    private void render() {
        stage.act(world.delta);
        stage.draw();
        renderer.setProjectionMatrix(camera.combined);
        renderer.begin(ShapeRenderer.ShapeType.Line);
        gui.updateAndRender(world.delta, renderer);
        renderer.end();
    }

    private void checkActivationButton() {
        if (Gdx.input.isKeyPressed(TOGGLE_PROFILER_KEY)) {
            if (!f3ButtonDown) {
                if (!SystemProfiler.isRunning()) {
                    gui.setHeight(Gdx.graphics.getHeight() / 2);
                    SystemProfiler.resume();
                } else if (gui.getHeight() != Gdx.graphics.getHeight()) {
                    gui.setHeight(Gdx.graphics.getHeight());
                } else {
                    SystemProfiler.pause();
                }
            }
            f3ButtonDown = true;
        } else {
            f3ButtonDown = false;
        }
    }

    private boolean leftMouseDown;

    /**
     * Emulate stage input to maintain pre-existing input processor.
     */
    private void processInput() {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            if (!leftMouseDown) {
                leftMouseDown = true;
                stage.touchDown(Gdx.input.getX(), Gdx.input.getY(), 0, Input.Buttons.LEFT);
            } else {
                stage.touchDragged(Gdx.input.getX(), Gdx.input.getY(), 0);
            }
        } else if (leftMouseDown) {
            leftMouseDown = false;
            stage.touchUp(Gdx.input.getX(), Gdx.input.getY(), 0, Input.Buttons.LEFT);
        }
    }

    @Override
    protected void dispose() {
        SystemProfiler.dispose();
    }

}
