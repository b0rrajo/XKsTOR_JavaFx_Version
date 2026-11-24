package com.acarballeira.jdbc_sqlite.xestorBiblioteca;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.XestorApp;

/**
 * Punto de entrada. Clase separada que non estende Application para que o JAR
 * "fat" xerado con maven-shade arranque sen necesidade de module-path.
 */
public class Launcher {
	public static void main(String[] args) {
		XestorApp.main(args);
	}
}
