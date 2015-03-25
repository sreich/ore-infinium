package com.ore.infinium.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglInput;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.ExceptionDialog;
import com.ore.infinium.OreClient;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String s = String.format("You broke it bad :(\nRecovery not possible.\nException on thread name: %s",
                    thread.getName());

            throwable.printStackTrace();

            ExceptionDialog dialog = new ExceptionDialog("Ore Infinium Exception Handler", s, throwable);
            dialog.setVisible(true);
        });

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 1600;
        config.height = 900;
        config.resizable = false;
        config.title = "Ore Infinium";
        config.vSyncEnabled = false;

        Array<String> array = new Array<>(arg);

        if (array.contains("--pack", false)) {
            TexturePacker.Settings settings = new TexturePacker.Settings();
            settings.maxWidth = 512;
            settings.maxHeight = 512;
            settings.fast = true; //hack just to speed up debugging, overrides local settings(probably??)

            TexturePacker.process(settings, "blocks", "../assets/packed", "blocks");
            TexturePacker.process(settings, "tiles", "../assets/packed", "tiles");
            TexturePacker.process(settings, "ui", "../assets/packed", "ui");
            TexturePacker.process(settings, "entities", "../assets/packed", "entities");
        }

        LwjglInput.keyRepeatTime = 0.08f;
        LwjglInput.keyRepeatInitialTime = 0.15f;

        //config.foregroundFPS = 0;
        //config.backgroundFPS = 0;
        new LwjglApplication(new OreClient(), config);

    }
}
