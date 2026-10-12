package com.territorypaint.client;

/**
 * Entry point wrapper for JavaFX application to bypass JavaFX module runtime check in IDEs.
 */
public class MainLauncher {
    public static void main(String[] args) {
        GameApplication.main(args);
    }
}
