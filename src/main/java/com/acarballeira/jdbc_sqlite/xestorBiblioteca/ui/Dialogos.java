package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import java.util.Optional;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.ResultadoImportacion;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Utilidades para amosar mensaxes, erros e confirmacións. */
public final class Dialogos {

	private Dialogos() {
	}

	public static Alert crear(Window owner, AlertType tipo, String titulo, String cabeceira, String texto) {
		Alert a = new Alert(tipo);
		if (owner != null) a.initOwner(owner);
		a.setTitle(titulo);
		a.setHeaderText(cabeceira);
		a.setContentText(texto);
		estilar(a.getDialogPane());
		return a;
	}

	public static void estilar(DialogPane pane) {
		var css = Dialogos.class.getResource("estilos.css");
		if (css != null) pane.getStylesheets().add(css.toExternalForm());
		pane.getStyleClass().add("xestor-dialog");
		pane.setMinWidth(440);
	}

	public static void info(Window owner, String cabeceira, String texto) {
		crear(owner, AlertType.INFORMATION, "Información", cabeceira, texto).showAndWait();
	}

	public static void erro(Window owner, String cabeceira, String texto) {
		crear(owner, AlertType.ERROR, "Erro", cabeceira, texto).showAndWait();
	}

	public static boolean confirmar(Window owner, String cabeceira, String texto) {
		Optional<ButtonType> r = crear(owner, AlertType.CONFIRMATION, "Confirmar", cabeceira, texto).showAndWait();
		return r.isPresent() && r.get() == ButtonType.OK;
	}

	/** Diálogo co resumo dunha importación e o detalle das liñas rexeitadas. */
	public static Alert resultadoImportacion(Window owner, String que, ResultadoImportacion r) {
		Alert a = crear(owner, r.getRechazados() == 0 ? AlertType.INFORMATION : AlertType.WARNING,
				"Resumo da importación", "Importación de " + que + " finalizada",
				null);
		Label resumo = new Label("Rexistros insertados: " + r.getInsertados() + "\nRexistros rexeitados: "
				+ r.getRechazados());
		resumo.getStyleClass().add("resumo-importacion");
		VBox box = new VBox(10, resumo);
		if (!r.getMensaxes().isEmpty()) {
			TextArea detalle = new TextArea(String.join("\n", r.getMensaxes()));
			detalle.setEditable(false);
			detalle.setWrapText(true);
			detalle.setPrefRowCount(6);
			box.getChildren().addAll(new Label("Detalle dos rexeitados:"), detalle);
		}
		a.getDialogPane().setContent(box);
		return a;
	}
}
