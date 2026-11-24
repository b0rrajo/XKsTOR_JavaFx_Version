package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Rutas;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.CatalogoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Obra;
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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
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

/** Xestión do catálogo: alta, modificación, baixa, procura e CSV. */
public class CatalogoView implements Vista {

	private final MainWindow main;
	private final CatalogoDAO dao = new CatalogoDAO();
	private final ImportExportService io = new ImportExportService();

	private final ObservableList<Obra> datos = FXCollections.observableArrayList();
	private final FilteredList<Obra> filtrados = new FilteredList<>(datos);
	private final TableView<Obra> taboa = new TableView<>(filtrados);
	private final TextField buscador = UI.buscador("Buscar por código, título, autor…");
	private final ChoiceBox<String> filtro = new ChoiceBox<>(
			FXCollections.observableArrayList("Todas", "Dispoñibles", "Prestadas"));
	private final Label pe = UI.pe("");
	private final VBox raiz;

	public CatalogoView(MainWindow main) {
		this.main = main;

		Button nova = UI.botonPrimario("Nova obra", "engadir");
		Button editar = UI.boton("Editar", "editar");
		Button baixa = UI.botonPerigo("Descatalogar", "borrar");
		Button importar = UI.boton("Importar CSV", "importar");
		Button exportar = UI.boton("Exportar CSV", "exportar");
		editar.disableProperty().bind(taboa.getSelectionModel().selectedItemProperty().isNull());
		baixa.disableProperty().bind(taboa.getSelectionModel().selectedItemProperty().isNull());

		nova.setOnAction(e -> novaObra());
		editar.setOnAction(e -> editar(taboa.getSelectionModel().getSelectedItem()));
		baixa.setOnAction(e -> descatalogar(taboa.getSelectionModel().getSelectedItem()));
		importar.setOnAction(e -> importar());
		exportar.setOnAction(e -> exportar());

		filtro.getSelectionModel().selectFirst();
		filtro.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		buscador.textProperty().addListener((o, a, b) -> aplicarFiltro());
		filtro.valueProperty().addListener((o, a, b) -> aplicarFiltro());

		taboa.getColumns().add(UI.columna("Código", Obra::getCodObra, 100));
		taboa.getColumns().add(UI.columna("Título", Obra::getTituloObra, 280));
		taboa.getColumns().add(UI.columna("Autor", Obra::getAutorObra, 190));
		taboa.getColumns().add(UI.columna("Editorial", Obra::getEditorialObra, 150));
		taboa.getColumns().add(UI.columna("Ano", Obra::getAnoObra, 60));
		taboa.getColumns().add(UI.columnaEstado("Dispoñible", Obra::isDisponible, "SÍ", "PRESTADA", 115));
		taboa.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		taboa.setPlaceholder(new Label("Non hai obras que amosar. Usa «Nova obra» ou «Importar CSV»."));
		taboa.setRowFactory(tv -> {
			TableRow<Obra> fila = new TableRow<>();
			fila.setOnMouseClicked(ev -> {
				if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2 && !fila.isEmpty()) {
					editar(fila.getItem());
				}
			});
			return fila;
		});
		VBox.setVgrow(taboa, Priority.ALWAYS);

		raiz = UI.contedor(
				UI.cabeceira("Catálogo bibliográfico",
						"Rexistra novas obras, modifica os seus datos ou descatalógaas. Dobre clic nunha fila para editala."),
				UI.barra(nova, editar, baixa, UI.sepV(), buscador, filtro, UI.espazador(), importar, exportar),
				taboa, pe);
	}

	@Override
	public Node getNode() { return raiz; }

	@Override
	public void refrescar() {
		try {
			datos.setAll(dao.listar());
			aplicarFiltro();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao ler o catálogo", e.getMessage());
		}
	}

	private void aplicarFiltro() {
		String txt = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase();
		String f = filtro.getValue();
		filtrados.setPredicate(o -> {
			if ("Dispoñibles".equals(f) && !o.isDisponible()) return false;
			if ("Prestadas".equals(f) && o.isDisponible()) return false;
			if (txt.isEmpty()) return true;
			return o.getCodObra().toLowerCase().contains(txt) || o.getTituloObra().toLowerCase().contains(txt)
					|| o.getAutorObra().toLowerCase().contains(txt)
					|| o.getEditorialObra().toLowerCase().contains(txt)
					|| String.valueOf(o.getAnoObra()).contains(txt);
		});
		long disp = datos.stream().filter(Obra::isDisponible).count();
		pe.setText(filtrados.size() + " de " + datos.size() + " obras amosadas · " + disp + " dispoñibles · "
				+ (datos.size() - disp) + " en préstamo");
	}

	// ------------------------------------------------------------ accións

	public void novaObra() {
		dialogoObra(null).showAndWait().ifPresent(o -> {
			main.estado("Obra «" + o.getTituloObra() + "» catalogada con éxito.");
			refrescar();
			seleccionar(o.getCodObra());
		});
	}

	private void editar(Obra o) {
		if (o == null) return;
		dialogoObra(o).showAndWait().ifPresent(r -> {
			main.estado("Obra " + r.getCodObra() + " modificada.");
			refrescar();
			seleccionar(r.getCodObra());
		});
	}

	private void descatalogar(Obra o) {
		if (o == null) return;
		if (!Dialogos.confirmar(main.getWindow(), "Descatalogar a obra " + o.getCodObra() + "?",
				"«" + o.getTituloObra() + "» de " + o.getAutorObra()
						+ "\n\nA obra eliminarase do catálogo. Esta acción non se pode desfacer.")) {
			return;
		}
		try {
			dao.descatalogar(o.getCodObra());
			main.estado("Obra " + o.getCodObra() + " descatalogada.");
			refrescar();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Non se pode descatalogar", e.getMessage());
		}
	}

	private void importar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Importar catálogo desde CSV");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros CSV (*.csv)", "*.csv"));
		fc.setInitialFileName("exportCatalogo.csv");
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showOpenDialog(main.getWindow());
		if (f == null) return;
		try {
			ResultadoImportacion r = io.importarCatalogoCSV(f.toPath());
			refrescar();
			main.refrescarTodo();
			main.estado("Importación CSV: " + r.getInsertados() + " insertadas, " + r.getRechazados() + " rexeitadas.");
			Dialogos.resultadoImportacion(main.getWindow(), "obras", r).showAndWait();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro durante a importación", e.getMessage());
		}
	}

	private void exportar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Exportar catálogo a CSV");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros CSV (*.csv)", "*.csv"));
		fc.setInitialFileName("exportCatalogo.csv");
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showSaveDialog(main.getWindow());
		if (f == null) return;
		try {
			int n = io.exportarCatalogoCSV(f.toPath());
			main.estado("Exportadas " + n + " obras a " + f.getName());
			Dialogos.info(main.getWindow(), "Exportación completada",
					"Exportáronse " + n + " obras a:\n" + f.getAbsolutePath());
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro durante a exportación", e.getMessage());
		}
	}

	private void seleccionar(String cod) {
		for (Obra o : filtrados) {
			if (o.getCodObra().equals(cod)) {
				taboa.getSelectionModel().select(o);
				taboa.scrollTo(o);
				return;
			}
		}
	}

	// ----------------------------------------------------------- diálogo

	/**
	 * Formulario de alta (existente == null) ou modificación. Garda na BD ao
	 * premer «Gardar»; se hai erros amósaos sen pechar o diálogo.
	 */
	public Dialog<Obra> dialogoObra(Obra existente) {
		boolean alta = existente == null;
		Dialog<Obra> d = new Dialog<>();
		d.initOwner(main.getWindow());
		d.setTitle(alta ? "Nova catalogación" : "Modificar obra");
		d.setHeaderText(alta ? "Rexistrar unha nova obra no catálogo"
				: "Modificar os datos da obra " + existente.getCodObra());
		Dialogos.estilar(d.getDialogPane());
		ButtonType gardar = new ButtonType("Gardar", ButtonType.OK.getButtonData());
		d.getDialogPane().getButtonTypes().addAll(gardar, ButtonType.CANCEL);

		TextField cod = new TextField(alta ? "" : existente.getCodObra());
		TextField tit = new TextField(alta ? "" : existente.getTituloObra());
		TextField aut = new TextField(alta ? "" : existente.getAutorObra());
		TextField edi = new TextField(alta ? "" : existente.getEditorialObra());
		TextField ano = new TextField(alta ? "" : String.valueOf(existente.getAnoObra()));
		cod.setPromptText("p. ex. LIB-0001");
		tit.setPromptText("Título da obra");
		aut.setPromptText("Nome do autor/a");
		edi.setPromptText("Editorial");
		ano.setPromptText("p. ex. " + LocalDate.now().getYear());
		ano.textProperty().addListener((o, a, b) -> {
			if (!b.matches("\\d{0,4}")) ano.setText(a);
		});
		cod.setDisable(!alta);

		Label erro = new Label();
		erro.getStyleClass().add("erro-formulario");
		erro.setWrapText(true);

		GridPane g = formulario();
		g.addRow(0, new Label("Código *"), cod);
		g.addRow(1, new Label("Título *"), tit);
		g.addRow(2, new Label("Autor *"), aut);
		g.addRow(3, new Label("Editorial *"), edi);
		g.addRow(4, new Label("Ano de publicación *"), ano);
		g.add(erro, 0, 5, 2, 1);
		if (!alta) {
			Label nota = new Label("O código non se pode cambiar. Estado actual: "
					+ (existente.isDisponible() ? "dispoñible" : "en préstamo") + ".");
			nota.getStyleClass().add("nota");
			g.add(nota, 0, 6, 2, 1);
		}
		d.getDialogPane().setContent(g);

		Node bGardar = d.getDialogPane().lookupButton(gardar);
		bGardar.addEventFilter(ActionEvent.ACTION, ev -> {
			try {
				int a = ano.getText().isBlank() ? -1 : Integer.parseInt(ano.getText());
				Obra o = new Obra(cod.getText().trim(), tit.getText(), aut.getText(), edi.getText(), a,
						alta || existente.isDisponible());
				if (alta) dao.catalogar(o);
				else dao.modificar(o);
				d.setResult(o);
			} catch (Exception ex) {
				erro.setText("Erro: " + ex.getMessage());
				ev.consume();
			}
		});
		d.setResultConverter(bt -> bt == gardar ? d.getResult() : null);
		javafx.application.Platform.runLater(() -> (alta ? cod : tit).requestFocus());
		return d;
	}

	/** Grella estándar para formularios de dous columnas. */
	public static GridPane formulario() {
		GridPane g = new GridPane();
		g.setHgap(12);
		g.setVgap(10);
		g.setPadding(new Insets(16, 18, 8, 18));
		g.getStyleClass().add("formulario");
		javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
		c1.setMinWidth(140);
		javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
		c2.setPrefWidth(300);
		c2.setHgrow(Priority.ALWAYS);
		g.getColumnConstraints().addAll(c1, c2);
		return g;
	}

	/** Usado polo panel e polas probas: lista actual. */
	public List<Obra> getDatos() { return datos; }

	public TableView<Obra> getTaboa() { return taboa; }
	public TextField getBuscador() { return buscador; }
}
