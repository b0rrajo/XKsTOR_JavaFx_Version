package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Database;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views.CatalogoView;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views.LectoresView;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views.OpcionsView;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views.PanelView;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views.PrestamosView;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Xanela principal: cabeceira, menú lateral de navegación, zona central coa
 * vista activa e barra de estado inferior.
 */
public class MainWindow {

	public enum Seccion {
		PANEL("inicio", "Inicio"), PRESTAMOS("prestamos", "Préstamos"), CATALOGO("catalogo", "Catálogo"),
		LECTORES("lectores", "Lectores"), OPCIONS("opcions", "Outras opcións");

		final String icona, nome;

		Seccion(String icona, String nome) {
			this.icona = icona;
			this.nome = nome;
		}
	}

	private final Stage stage;
	private final BorderPane root = new BorderPane();
	private final Label estado = new Label("Listo.");
	private final Map<Seccion, Vista> vistas = new LinkedHashMap<>();
	private final Map<Seccion, ToggleButton> botons = new LinkedHashMap<>();
	private final Label tituloSeccion = new Label();

	public MainWindow(Stage stage) {
		this.stage = stage;
		root.getStyleClass().add("root-pane");

		vistas.put(Seccion.PANEL, new PanelView(this));
		vistas.put(Seccion.PRESTAMOS, new PrestamosView(this));
		vistas.put(Seccion.CATALOGO, new CatalogoView(this));
		vistas.put(Seccion.LECTORES, new LectoresView(this));
		vistas.put(Seccion.OPCIONS, new OpcionsView(this));

		root.setTop(crearCabeceira());
		root.setLeft(crearMenu());
		root.setBottom(crearBarraEstado());
		ir(Seccion.PANEL);
	}

	private HBox crearCabeceira() {
		HBox top = new HBox(12);
		top.getStyleClass().add("cabeceira");
		top.setAlignment(Pos.CENTER_LEFT);
		top.setPadding(new Insets(10, 20, 10, 16));
		var logoStream = MainWindow.class.getResourceAsStream("logo.jpg");
		if (logoStream != null) {
			ImageView logo = new ImageView(new Image(logoStream));
			logo.setFitHeight(36);
			logo.setPreserveRatio(true);
			top.getChildren().add(logo);
		}
		Label marca = new Label("XKsTOR");
		marca.getStyleClass().add("marca");
		Label sub = new Label("Xestor de Bibliotecas");
		sub.getStyleClass().add("submarca");
		Region esp = new Region();
		HBox.setHgrow(esp, Priority.ALWAYS);
		tituloSeccion.getStyleClass().add("titulo-seccion");
		top.getChildren().addAll(marca, sub, esp, tituloSeccion);
		return top;
	}

	private VBox crearMenu() {
		VBox menu = new VBox(4);
		menu.getStyleClass().add("menu-lateral");
		menu.setPadding(new Insets(16, 10, 16, 10));
		menu.setPrefWidth(200);
		ToggleGroup grupo = new ToggleGroup();
		for (Seccion s : Seccion.values()) {
			if (s == Seccion.OPCIONS) {
				Region esp = new Region();
				VBox.setVgrow(esp, Priority.ALWAYS);
				menu.getChildren().add(esp);
			}
			ToggleButton b = new ToggleButton(s.nome, Iconas.de(s.icona, 17));
			b.setGraphicTextGap(12);
			b.getStyleClass().add("boton-menu");
			b.setMaxWidth(Double.MAX_VALUE);
			b.setAlignment(Pos.CENTER_LEFT);
			b.setToggleGroup(grupo);
			b.setOnAction(e -> ir(s));
			botons.put(s, b);
			menu.getChildren().add(b);
		}
		// impide que quede ningún botón desmarcado
		grupo.selectedToggleProperty().addListener((obs, vello, novo) -> {
			if (novo == null && vello != null) vello.setSelected(true);
		});
		return menu;
	}

	private HBox crearBarraEstado() {
		HBox bar = new HBox(16);
		bar.getStyleClass().add("barra-estado");
		bar.setPadding(new Insets(6, 16, 6, 16));
		bar.setAlignment(Pos.CENTER_LEFT);
		Region esp = new Region();
		HBox.setHgrow(esp, Priority.ALWAYS);
		String data = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
				new Locale("gl", "ES")));
		Label db = new Label("BD: " + Database.get().ficheiro().getFileName());
		db.setTooltip(new javafx.scene.control.Tooltip(Database.get().ficheiro().toString()));
		bar.getChildren().addAll(estado, esp, db, new Label("·"), new Label(data));
		return bar;
	}

	/** Cambia a vista central á sección indicada e refresca os seus datos. */
	public void ir(Seccion s) {
		Vista v = vistas.get(s);
		v.refrescar();
		root.setCenter(v.getNode());
		botons.get(s).setSelected(true);
		tituloSeccion.setText(s.nome);
	}

	/** Refresca todas as vistas (tras restaurar unha copia, importar, etc.). */
	public void refrescarTodo() {
		vistas.values().forEach(Vista::refrescar);
	}

	public void estado(String mensaxe) {
		estado.setText(mensaxe);
	}

	@SuppressWarnings("unchecked")
	public <T extends Vista> T vista(Seccion s) {
		return (T) vistas.get(s);
	}

	public BorderPane getRoot() { return root; }
	public Stage getStage() { return stage; }
	public Window getWindow() { return stage; }
}
