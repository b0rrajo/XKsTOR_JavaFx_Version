package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Orientation;

/** Pequenas fábricas de compoñentes para manter un aspecto homoxéneo. */
public final class UI {

	private UI() {
	}

	/** Botón con icona vectorial (ver {@link Iconas}); icona pode ser null. */
	public static Button boton(String texto, String icona, String... clases) {
		Button b = new Button(texto);
		if (icona != null) {
			b.setGraphic(Iconas.de(icona));
			b.setGraphicTextGap(7);
		}
		b.getStyleClass().addAll(clases);
		b.setMinWidth(Region.USE_PREF_SIZE);
		return b;
	}

	public static Button boton(String texto) {
		return boton(texto, null);
	}

	public static Button botonPrimario(String texto, String icona) {
		return boton(texto, icona, "boton-primario");
	}

	public static Button botonPerigo(String texto, String icona) {
		return boton(texto, icona, "boton-perigo");
	}

	public static Region espazador() {
		Region r = new Region();
		HBox.setHgrow(r, Priority.ALWAYS);
		return r;
	}

	public static Separator sepV() {
		return new Separator(Orientation.VERTICAL);
	}

	public static TextField buscador(String prompt) {
		TextField tf = new TextField();
		tf.setPromptText(prompt);
		tf.getStyleClass().add("buscador");
		tf.setPrefWidth(240);
		tf.setMinWidth(150);
		HBox.setHgrow(tf, Priority.SOMETIMES);
		return tf;
	}

	/** Cabeceira de vista: título grande e unha liña descritiva. */
	public static VBox cabeceira(String titulo, String descricion) {
		Label t = new Label(titulo);
		t.getStyleClass().add("vista-titulo");
		Label d = new Label(descricion);
		d.getStyleClass().add("vista-descricion");
		d.setWrapText(true);
		return new VBox(2, t, d);
	}

	public static HBox barra(Node... nodos) {
		HBox h = new HBox(8, nodos);
		h.setAlignment(Pos.CENTER_LEFT);
		h.getStyleClass().add("barra-ferramentas");
		h.setPadding(new Insets(8, 10, 8, 10));
		return h;
	}

	public static VBox contedor(Node... nodos) {
		VBox v = new VBox(14, nodos);
		v.setPadding(new Insets(22, 26, 20, 26));
		v.getStyleClass().add("vista");
		return v;
	}

	public static <S, T> TableColumn<S, T> columna(String titulo, Function<S, T> valor, double ancho) {
		TableColumn<S, T> c = new TableColumn<>(titulo);
		c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(valor.apply(cd.getValue())));
		c.setPrefWidth(ancho);
		if (ancho <= 130) {
			// columnas curtas (códigos, datas, estado): non deixar que medren demasiado
			c.setMinWidth(ancho);
			c.setMaxWidth(ancho * 1.3);
		}
		return c;
	}

	/** Columna que pinta unha "etiqueta" verde/vermella segundo un booleano. */
	public static <S> TableColumn<S, Boolean> columnaEstado(String titulo, Function<S, Boolean> valor,
			String textoSi, String textoNon, double ancho) {
		TableColumn<S, Boolean> c = columna(titulo, valor, ancho);
		c.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(Boolean item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
				} else {
					Label l = new Label(item ? textoSi : textoNon);
					l.getStyleClass().addAll("etiqueta", item ? "etiqueta-ok" : "etiqueta-ko");
					setGraphic(l);
					setText(null);
				}
			}
		});
		c.setStyle("-fx-alignment: CENTER;");
		return c;
	}

	/** Tarxeta con número grande para o panel de inicio. */
	public static VBox tarxeta(Label valor, String etiqueta, String clase) {
		valor.getStyleClass().add("tarxeta-valor");
		Label l = new Label(etiqueta);
		l.getStyleClass().add("tarxeta-etiqueta");
		VBox v = new VBox(4, valor, l);
		v.getStyleClass().addAll("tarxeta", clase);
		v.setPadding(new Insets(16, 18, 16, 18));
		HBox.setHgrow(v, Priority.ALWAYS);
		v.setMaxWidth(Double.MAX_VALUE);
		return v;
	}

	/** Selector de data con formato dd/MM/aaaa independente da configuración rexional. */
	public static javafx.scene.control.DatePicker selectorData(java.time.LocalDate inicial) {
		var dp = new javafx.scene.control.DatePicker(inicial);
		var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
		dp.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(java.time.LocalDate d) { return d == null ? "" : fmt.format(d); }

			@Override
			public java.time.LocalDate fromString(String s) {
				return s == null || s.isBlank() ? null : java.time.LocalDate.parse(s.trim(), fmt);
			}
		});
		dp.setPromptText("dd/mm/aaaa");
		return dp;
	}

	public static Label pe(String texto) {
		Label l = new Label(texto);
		l.getStyleClass().add("pe-taboa");
		return l;
	}
}
