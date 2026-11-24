package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Rutas;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.LectorDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.PrestamoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Lector;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Operacion;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.ResultadoImportacion;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.service.ImportExportService;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Dialogos;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.MainWindow;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.UI;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Vista;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Xestión de lectores: alta, modificación, baixa, procura e JSON. */
public class LectoresView implements Vista {

	private final MainWindow main;
	private final LectorDAO dao = new LectorDAO();
	private final PrestamoDAO prestamoDAO = new PrestamoDAO();
	private final ImportExportService io = new ImportExportService();

	private final ObservableList<Lector> datos = FXCollections.observableArrayList();
	private final FilteredList<Lector> filtrados = new FilteredList<>(datos);
	private final TableView<Lector> taboa = new TableView<>(filtrados);
	private final TextField buscador = UI.buscador("Buscar por código, DNI, nome…");
	private final Map<String, Integer> activos = new HashMap<>();
	private final Label pe = UI.pe("");
	private final VBox raiz;

	public LectoresView(MainWindow main) {
		this.main = main;

		Button alta = UI.botonPrimario("Alta de lector", "engadir");
		Button editar = UI.boton("Editar", "editar");
		Button baixa = UI.botonPerigo("Dar de baixa", "borrar");
		Button importar = UI.boton("Importar JSON", "importar");
		Button exportar = UI.boton("Exportar JSON", "exportar");
		editar.disableProperty().bind(taboa.getSelectionModel().selectedItemProperty().isNull());
		baixa.disableProperty().bind(taboa.getSelectionModel().selectedItemProperty().isNull());

		alta.setOnAction(e -> novoLector());
		editar.setOnAction(e -> editar(taboa.getSelectionModel().getSelectedItem()));
		baixa.setOnAction(e -> darBaixa(taboa.getSelectionModel().getSelectedItem()));
		importar.setOnAction(e -> importar());
		exportar.setOnAction(e -> exportar());
		buscador.textProperty().addListener((o, a, b) -> aplicarFiltro());

		taboa.getColumns().add(UI.columna("Código", Lector::getCodLector, 100));
		taboa.getColumns().add(UI.columna("DNI", Lector::getDniLector, 110));
		taboa.getColumns().add(UI.columna("Nome", Lector::getNombreLector, 140));
		taboa.getColumns().add(UI.columna("Apelidos", Lector::getApellidosLector, 220));
		taboa.getColumns().add(UI.columna("Idade", Lector::getEdadLector, 65));
		taboa.getColumns().add(UI.columna("Préstamos activos", (Lector l) -> activos.getOrDefault(l.getCodLector(), 0), 130));
		taboa.getColumns().get(4).setStyle("-fx-alignment: CENTER;");
		taboa.getColumns().get(5).setStyle("-fx-alignment: CENTER;");
		taboa.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		taboa.setPlaceholder(new Label("Non hai lectores que amosar. Usa «Alta de lector» ou «Importar JSON»."));
		taboa.setRowFactory(tv -> {
			TableRow<Lector> fila = new TableRow<>();
			fila.setOnMouseClicked(ev -> {
				if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2 && !fila.isEmpty()) {
					editar(fila.getItem());
				}
			});
			return fila;
		});
		VBox.setVgrow(taboa, Priority.ALWAYS);

		raiz = UI.contedor(
				UI.cabeceira("Lectores",
						"Dá de alta ou de baixa usuarios da biblioteca e consulta cantos préstamos teñen pendentes."),
				UI.barra(alta, editar, baixa, UI.sepV(), buscador, UI.espazador(), importar, exportar), taboa, pe);
	}

	@Override
	public Node getNode() { return raiz; }

	@Override
	public void refrescar() {
		try {
			activos.clear();
			for (Operacion op : prestamoDAO.listarActivos()) {
				activos.merge(op.getCodLector(), 1, Integer::sum);
			}
			datos.setAll(dao.listar());
			taboa.refresh();
			aplicarFiltro();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao ler os lectores", e.getMessage());
		}
	}

	private void aplicarFiltro() {
		String txt = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase();
		filtrados.setPredicate(l -> txt.isEmpty() || l.getCodLector().toLowerCase().contains(txt)
				|| l.getDniLector().toLowerCase().contains(txt) || l.getNomeCompleto().toLowerCase().contains(txt));
		long conPrestamo = datos.stream().filter(l -> activos.containsKey(l.getCodLector())).count();
		pe.setText(filtrados.size() + " de " + datos.size() + " lectores amosados · " + conPrestamo
				+ " con préstamos pendentes");
	}

	public void novoLector() {
		dialogoLector(null).showAndWait().ifPresent(l -> {
			main.estado("Lector " + l.getCodLector() + " (" + l.getNomeCompleto() + ") rexistrado con éxito.");
			refrescar();
			seleccionar(l.getCodLector());
		});
	}

	private void editar(Lector l) {
		if (l == null) return;
		dialogoLector(l).showAndWait().ifPresent(r -> {
			main.estado("Lector " + r.getCodLector() + " modificado.");
			refrescar();
			seleccionar(r.getCodLector());
		});
	}

	private void darBaixa(Lector l) {
		if (l == null) return;
		if (!Dialogos.confirmar(main.getWindow(), "Dar de baixa ao lector " + l.getCodLector() + "?",
				l.getNomeCompleto() + " (DNI " + l.getDniLector() + ")\n\nEsta acción non se pode desfacer.")) {
			return;
		}
		try {
			dao.baixa(l.getCodLector());
			main.estado("Lector " + l.getCodLector() + " dado de baixa.");
			refrescar();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Non se pode dar de baixa", e.getMessage());
		}
	}

	private void importar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Importar lectores desde JSON");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros JSON (*.json)", "*.json"));
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showOpenDialog(main.getWindow());
		if (f == null) return;
		try {
			ResultadoImportacion r = io.importarLectoresJSON(f.toPath());
			refrescar();
			main.estado("Importación JSON: " + r.getInsertados() + " insertados, " + r.getRechazados() + " rexeitados.");
			Dialogos.resultadoImportacion(main.getWindow(), "lectores", r).showAndWait();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro durante a importación", e.getMessage());
		}
	}

	private void exportar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Exportar lectores a JSON");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros JSON (*.json)", "*.json"));
		fc.setInitialFileName("Lectores.json");
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showSaveDialog(main.getWindow());
		if (f == null) return;
		try {
			int n = io.exportarLectoresJSON(f.toPath());
			main.estado("Exportados " + n + " lectores a " + f.getName());
			Dialogos.info(main.getWindow(), "Exportación completada",
					"Exportáronse " + n + " lectores a:\n" + f.getAbsolutePath());
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro durante a exportación", e.getMessage());
		}
	}

	private void seleccionar(String cod) {
		for (Lector l : filtrados) {
			if (l.getCodLector().equals(cod)) {
				taboa.getSelectionModel().select(l);
				taboa.scrollTo(l);
				return;
			}
		}
	}

	public Dialog<Lector> dialogoLector(Lector existente) {
		boolean alta = existente == null;
		Dialog<Lector> d = new Dialog<>();
		d.initOwner(main.getWindow());
		d.setTitle(alta ? "Nova alta de usuario" : "Modificar lector");
		d.setHeaderText(alta ? "Rexistrar un novo lector na biblioteca"
				: "Modificar os datos do lector " + existente.getCodLector());
		Dialogos.estilar(d.getDialogPane());
		ButtonType gardar = new ButtonType("Gardar", ButtonType.OK.getButtonData());
		d.getDialogPane().getButtonTypes().addAll(gardar, ButtonType.CANCEL);

		TextField cod = new TextField(alta ? "" : existente.getCodLector());
		TextField dni = new TextField(alta ? "" : existente.getDniLector());
		TextField nom = new TextField(alta ? "" : existente.getNombreLector());
		TextField ape = new TextField(alta ? "" : existente.getApellidosLector());
		TextField ida = new TextField(alta ? "" : String.valueOf(existente.getEdadLector()));
		cod.setPromptText("p. ex. LEC-001");
		dni.setPromptText("p. ex. 12345678Z");
		nom.setPromptText("Nome");
		ape.setPromptText("Apelidos");
		ida.setPromptText("Anos");
		ida.textProperty().addListener((o, a, b) -> {
			if (!b.matches("\\d{0,3}")) ida.setText(a);
		});
		cod.setDisable(!alta);

		Label erro = new Label();
		erro.getStyleClass().add("erro-formulario");
		erro.setWrapText(true);

		GridPane g = CatalogoView.formulario();
		g.addRow(0, new Label("Código de lector *"), cod);
		g.addRow(1, new Label("DNI *"), dni);
		g.addRow(2, new Label("Nome *"), nom);
		g.addRow(3, new Label("Apelidos *"), ape);
		g.addRow(4, new Label("Idade *"), ida);
		g.add(erro, 0, 5, 2, 1);
		d.getDialogPane().setContent(g);

		Node bGardar = d.getDialogPane().lookupButton(gardar);
		bGardar.addEventFilter(ActionEvent.ACTION, ev -> {
			try {
				int i = ida.getText().isBlank() ? -1 : Integer.parseInt(ida.getText());
				Lector l = new Lector(cod.getText().trim(), dni.getText().trim().toUpperCase(), nom.getText(),
						ape.getText(), i);
				if (alta) dao.alta(l);
				else dao.modificar(l);
				d.setResult(l);
			} catch (Exception ex) {
				erro.setText("Erro: " + ex.getMessage());
				ev.consume();
			}
		});
		d.setResultConverter(bt -> bt == gardar ? d.getResult() : null);
		javafx.application.Platform.runLater(() -> (alta ? cod : dni).requestFocus());
		return d;
	}

	public TableView<Lector> getTaboa() { return taboa; }
}
