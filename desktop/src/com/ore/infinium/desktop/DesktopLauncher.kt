package com.ore.infinium.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglInput
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.beust.jcommander.JCommander
import com.ore.infinium.ErrorDialog
import com.ore.infinium.OreClient
import com.ore.infinium.OreSettings

class DesktopLauncher {

    private fun runGame(arg: Array<String>) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            //            ExceptionDialog dialog = new ExceptionDialog("Ore Infinium Exception Handler", s, throwable);
            val dialog2 = ErrorDialog(throwable, Thread.currentThread())
            dialog2.isVisible = true
        }

        //inject jcommander into OreSettings, to properly parse args
        //into respective annotated variables
        val jCommander = JCommander()
        jCommander.addObject(OreSettings)
        jCommander.setCaseSensitiveOptions(false)
        jCommander.setProgramName("Ore Infinium")
        jCommander.parse(*arg)

        val config = LwjglApplicationConfiguration()
        config.useGL30 = true
        config.title = "Ore Infinium"

        config.width = OreSettings.width
        config.height = OreSettings.height
        config.resizable = OreSettings.resizable
        config.vSyncEnabled = OreSettings.vsyncEnabled
        config.foregroundFPS = OreSettings.framerate
        config.backgroundFPS = OreSettings.framerate

        if (OreSettings.help) {
            println("Ore Infinium - an open source block building survival game.\n" + "To enable assertions, you may want to pass to the Java VM, -ea")
            //print how to use
            jCommander.usage()

            return
        }

        if (OreSettings.pack) {
            //TexturePacker.Settings settings = new TexturePacker.Settings();
            //settings.maxWidth = 512;
            //settings.maxHeight = 512;
            //            settings.pot = true;
            //settings.fast = true; //fixme just to speed up debugging, overrides local settings(probably??)
            //lwjglfiles().internal("blah")
            TexturePacker.process("blocks", "../assets/packed", "blocks")
            TexturePacker.process("tiles", "../assets/packed", "tiles")
            TexturePacker.process("ui", "../assets/packed", "ui")
            TexturePacker.process("entities", "../assets/packed", "entities")
        }

        LwjglInput.keyRepeatTime = 0.08f
        LwjglInput.keyRepeatInitialTime = 0.15f

        LwjglApplication(OreClient(), config)
    }

    companion object {

        @JvmStatic fun main(arg: Array<String>) {
            DesktopLauncher().runGame(arg)
        }
    }
}
