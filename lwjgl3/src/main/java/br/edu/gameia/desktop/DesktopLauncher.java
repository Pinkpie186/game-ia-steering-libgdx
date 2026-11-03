package br.edu.gameia.desktop;

import br.edu.gameia.SteeringApp;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class DesktopLauncher {
    public static void main(String[] args) {
        String frames = null;
        boolean demo = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--record") && i + 1 < args.length) frames = args[++i];
            if (args[i].equals("--demo")) demo = true;
        }
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Game IA - Comportamentos autônomos");
        config.setWindowedMode(1280, 800);
        config.setResizable(frames == null);
        config.useVsync(frames == null);
        config.setForegroundFPS(frames == null ? 60 : 0);
        config.setIdleFPS(frames == null ? 30 : 0);
        config.disableAudio(true);
        new Lwjgl3Application(new SteeringApp(frames, demo), config);
    }
}
