package com.ore.infinium.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglInput;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.beust.jcommander.JCommander;
import com.ore.infinium.ErrorDialog;
import com.ore.infinium.OreClient;
import com.ore.infinium.OreSettings;

public class DesktopLauncher {

    public static void main(String[] arg) {
        new DesktopLauncher().runGame(arg);
    }

    private void runGame(String[] arg) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            //            ExceptionDialog dialog = new ExceptionDialog("Ore Infinium Exception Handler", s, throwable);
            ErrorDialog dialog2 = new ErrorDialog(throwable, Thread.currentThread());
            dialog2.setVisible(true);
        });

        OreSettings oreSettings = OreSettings.getInstance();

        //inject jcommander into OreSettings, to properly parse args
        //into respective annotated variables
        JCommander jCommander = new JCommander();
        jCommander.addObject(oreSettings);
        jCommander.setCaseSensitiveOptions(false);
        jCommander.setProgramName("Ore Infinium");
        jCommander.parse(arg);

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Ore Infinium";

        config.width = oreSettings.width;
        config.height = oreSettings.height;
        config.resizable = oreSettings.resizable;
        config.vSyncEnabled = oreSettings.vsyncEnabled;
        config.foregroundFPS = oreSettings.framerate;
        config.backgroundFPS = oreSettings.framerate;

        if (oreSettings.help) {
            System.out.println("Ore Infinium - an open source block building survival game.\n" +
                               "To enable assertions, you may want to pass to the Java VM, -ea");
            //print how to use
            jCommander.usage();

            return;
        }

        if (oreSettings.pack) {
            TexturePacker.Settings settings = new TexturePacker.Settings();
            settings.maxWidth = 512;
            settings.maxHeight = 512;
            settings.fast = true; //fixme just to speed up debugging, overrides local settings(probably??)

            TexturePacker.process(settings, "blocks", "../assets/packed", "blocks");
            TexturePacker.process(settings, "tiles", "../assets/packed", "tiles");
            TexturePacker.process(settings, "ui", "../assets/packed", "ui");
            TexturePacker.process(settings, "entities", "../assets/packed", "entities");
        }

        LwjglInput.keyRepeatTime = 0.08f;
        LwjglInput.keyRepeatInitialTime = 0.15f;

        new LwjglApplication(new OreClient(), config);
    }
}
