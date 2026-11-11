package com.conary.rosita;

/**
 * Lanzador principal del sistema.
 * Al no heredar directamente de javafx.application.Application,
 * evita la restriccion del launcher de la JVM cuando se ejecuta desde el IDE.
 */
public class MainLauncher {

    public static void main(String[] args) {
        App.main(args);
    }
}

