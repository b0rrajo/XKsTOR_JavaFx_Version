package com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Rutas de traballo da aplicación. Cando XsTOR está instalada (por exemplo en
 * C:\Program Files) o directorio da aplicación é de só lectura, así que a base
 * de datos gárdase na carpeta de datos do usuario.
 */
public final class Rutas {

	private Rutas() {}

	/** Carpeta de datos do usuario: %APPDATA%\XsTOR en Windows, ~/.xstor no resto. */
	public static Path carpetaDatos() {
		String appdata = System.getenv("APPDATA");
		Path dir = (appdata != null && !appdata.isBlank())
				? Path.of(appdata, "XsTOR")
				: Path.of(System.getProperty("user.home"), ".xstor");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			return Path.of(System.getProperty("user.home"));
		}
		return dir;
	}

	/**
	 * Base de datos por defecto. Se existe un xestorBibliotecas.db no directorio
	 * de traballo e aínda non hai ningún na carpeta de datos, cópiase (migración
	 * desde a versión sen instalador).
	 */
	public static Path baseDatosPorDefecto() {
		Path destino = carpetaDatos().resolve(Database.FICHEIRO_POR_DEFECTO);
		Path antiga = Path.of(Database.FICHEIRO_POR_DEFECTO).toAbsolutePath();
		if (!Files.exists(destino) && Files.isRegularFile(antiga)) {
			try {
				Files.copy(antiga, destino, StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException ignored) {
				// se falla, crearase unha base de datos baleira
			}
		}
		return destino;
	}

	/** Cartafol inicial dos diálogos de ficheiros (Documentos ou, se non existe, a carpeta persoal). */
	public static File cartafolInicial() {
		File home = new File(System.getProperty("user.home"));
		File docs = new File(home, "Documents");
		if (!docs.isDirectory()) docs = new File(home, "Documentos");
		return docs.isDirectory() ? docs : home;
	}
}
