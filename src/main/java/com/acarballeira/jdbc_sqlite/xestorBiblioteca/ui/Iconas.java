package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import java.util.Map;

import javafx.scene.layout.Region;

/**
 * Iconas vectoriais (camiños SVG de Material Design Icons, licenza Apache 2.0).
 * Debúxanse como Region con -fx-shape, así se ven igual en calquera sistema
 * operativo sen depender das fontes instaladas. A cor vén do CSS (.icona).
 */
public final class Iconas {

	private Iconas() {
	}

	private static final Map<String, String> SVG = Map.ofEntries(
			Map.entry("inicio", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
			Map.entry("prestamos", "M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"),
			Map.entry("catalogo", "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z"),
			Map.entry("lectores", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"),
			Map.entry("opcions", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"),
			Map.entry("engadir", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
			Map.entry("editar", "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"),
			Map.entry("borrar", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"),
			Map.entry("importar", "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"),
			Map.entry("exportar", "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z"),
			Map.entry("devolver", "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"),
			Map.entry("gardar", "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z"),
			Map.entry("restaurar", "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"),
			Map.entry("buscar", "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"),
			Map.entry("reparar", "M22.7 19l-9.1-9.1c.9-2.3.4-5-1.5-6.9-2-2-5-2.4-7.4-1.3L9 6 6 9 1.6 4.7C.4 7.1.9 10.1 2.9 12.1c1.9 1.9 4.6 2.4 6.9 1.5l9.1 9.1c.4.4 1 .4 1.4 0l2.3-2.3c.5-.4.5-1.1.1-1.4z"),
			Map.entry("revisar", "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"));

	public static Region de(String nome, double tamano) {
		Region r = new Region();
		String path = SVG.get(nome);
		if (path != null) r.setStyle("-fx-shape: \"" + path + "\";");
		r.getStyleClass().add("icona");
		r.setMinSize(tamano, tamano);
		r.setPrefSize(tamano, tamano);
		r.setMaxSize(tamano, tamano);
		return r;
	}

	public static Region de(String nome) {
		return de(nome, 15);
	}
}
