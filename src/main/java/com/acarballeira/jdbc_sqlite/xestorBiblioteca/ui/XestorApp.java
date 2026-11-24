package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import java.nio.file.Path;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Database;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Rutas;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * XKsTOR — Xestor de Bibliotecas (versión JavaFX).
 *
 * Arranca a base de datos SQLite (xestorBibliotecas.db na carpeta de datos do
 * usuario, ou a ruta pasada como primeiro argumento) e amosa a xanela
 * principal.
 *
 * @author Daniel Borrajo Fernández
 * @version REV 1.1 - INTERFACE 1.0
 */
public class XestorApp extends Application {

	public static final String TITULO = "XKsTOR · Xestor de Bibliotecas";

	@Override
	public void start(Stage stage) throws Exception {
		var params = getParameters().getUnnamed();
		Path db = params.isEmpty() ? Rutas.baseDatosPorDefecto() : Path.of(params.get(0));
		try {
			Database.abrir(db);
		} catch (Exception e) {
			Dialogos.erro(null, "Non se puido abrir a base de datos", e.getMessage());
			return;
		}

		MainWindow main = new MainWindow(stage);
		Scene scene = crearEscena(main);
		stage.setTitle(TITULO);
		stage.setScene(scene);
		stage.setMinWidth(1100);
		stage.setMinHeight(640);
		var logo = XestorApp.class.getResourceAsStream("logo.jpg");
		if (logo != null) stage.getIcons().add(new Image(logo));
		stage.setOnCloseRequest(e -> Database.get().pechar());
		stage.show();
	}

	/** Crea a escena coa folla de estilos aplicada. */
	public static Scene crearEscena(MainWindow main) {
		Scene scene = new Scene(main.getRoot(), 1280, 780);
		scene.getStylesheets().add(XestorApp.class.getResource("estilos.css").toExternalForm());
		return scene;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
