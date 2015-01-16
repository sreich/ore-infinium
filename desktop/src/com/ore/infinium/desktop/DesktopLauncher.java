package com.ore.infinium.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.ore.infinium.OreClient;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1600;
		config.height = 900;
		config.resizable = false;
		config.title = "Ore Infinium";
		config.vSyncEnabled = false;
		//config.foregroundFPS = 0;
		//config.backgroundFPS = 0;
		new LwjglApplication(new OreClient(), config);
	}
}
