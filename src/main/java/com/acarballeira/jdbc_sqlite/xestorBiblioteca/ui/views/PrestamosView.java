package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Rutas;
import java.io.File;
import java.time.LocalDate;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.CatalogoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.LectorDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.PrestamoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Lector;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Obra;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Operacion;
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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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

/** Xestión de préstamos e devolucións e rexistro histórico. */
public class PrestamosView implements Vista {

	private final MainWindow main;
	private final PrestamoDAO dao = new PrestamoDAO();
	private final LectorDAO lectorDAO = new LectorDAO();
	private final CatalogoDAO catalogoDAO = new CatalogoDAO();
	private final ImportExportService io = new ImportExportService();

	private final ObservableList<Operacion> datos = FXCollections.observableArrayList();
	private final FilteredList<Operacion> filtrados = new FilteredList<>(datos);
	private final TableView<Operacion> taboa = new TableView<>(filtrados);
	private final TextField buscador = UI.buscador("Buscar lector, obra, data…");
	private final ChoiceBox<String> filtro = new ChoiceBox<>(
			FXCollections.observableArrayList("Todas", "Activos", "Devoltos"));
	private final Label pe = UI.pe("");
	private final VBox raiz;

	public PrestamosView(MainWindow main) {
		this.main = main;

		Button prestar = UI.botonPrimario("Novo préstamo", "engadir");
		Button devolver = UI.boton("Rexistrar devolución", "devolver");
		Button exportar = UI.boton("Exportar rexistro (TXT)", "exportar");
		prestar.setOnAction(e -> novoPrestamo());
		devolver.setOnAction(e -> devolucion());
		exportar.setOnAction(e -> exportar());

		filtro.getSelectionModel().selectFirst();
		filtro.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		filtro.valueProperty().addListener((o, a, b) -> aplicarFiltro());
		buscador.textProperty().addListener((o, a, b) -> aplicarFiltro());

		taboa.getColumns().add(UI.columna("Nº", Operacion::getCodOperacion, 45));
		taboa.getColumns().add(UI.columna("Data préstamo", Operacion::getFechaPrestamo, 108));
		taboa.getColumns().add(UI.columna("Lector", Operacion::getCodLector, 85));
		taboa.getColumns().add(UI.columna("Nome do lector", Operacion::getNomeLector, 180));
		taboa.getColumns().add(UI.columna("Obra", Operacion::getCodObra, 85));
		taboa.getColumns().add(UI.columna("Título", Operacion::getTituloObra, 230));
		taboa.getColumns().add(UI.columna("Data devolución", (Operacion o) -> o.isActivo() ? "—" : o.getFechaDevolucion(), 118));
		taboa.getColumns().add(UI.columnaEstado("Estado", (Operacion o) -> !o.isActivo(), "DEVOLTO", "EN PRÉSTAMO", 125));
		taboa.getColumns().get(0).setStyle("-fx-alignment: CENTER;");
		taboa.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		taboa.setPlaceholder(new Label("Aínda non hai operacións rexistradas."));
		taboa.setRowFactory(tv -> {
			TableRow<Operacion> fila = new TableRow<>();
			fila.setOnMouseClicked(ev -> {
				if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2 && !fila.isEmpty()
						&& fila.getItem().isActivo()) {
					devolucionDirecta(fila.getItem());
				}
			});
			return fila;
		});
		VBox.setVgrow(taboa, Priority.ALWAYS);

		raiz = UI.contedor(
				UI.cabeceira("Préstamos e devolucións",
						"Presta obras dispoñibles aos lectores e rexistra as devolucións. Dobre clic nun préstamo activo para devolvelo."),
				UI.barra(prestar, devolver, UI.sepV(), buscador, filtro, UI.espazador(), exportar), taboa, pe);
	}

	@Override
	public Node getNode() { return raiz; }

	@Override
	public void refrescar() {
		try {
			datos.setAll(dao.listar());
			aplicarFiltro();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao ler os préstamos", e.getMessage());
		}
	}

	private void aplicarFiltro() {
		String txt = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase();
		String f = filtro.getValue();
		filtrados.setPredicate(o -> {
			if ("Activos".equals(f) && !o.isActivo()) return false;
			if ("Devoltos".equals(f) && o.isActivo()) return false;
			if (txt.isEmpty()) return true;
			return o.getCodLector().toLowerCase().contains(txt) || o.getNomeLector().toLowerCase().contains(txt)
					|| o.getCodObra().toLowerCase().contains(txt) || o.getTituloObra().toLowerCase().contains(txt)
					|| o.getFechaPrestamo().contains(txt) || o.getFechaDevolucion().contains(txt);
		});
		long act = datos.stream().filter(Operacion::isActivo).count();
		pe.setText(filtrados.size() + " de " + datos.size() + " operacións amosadas · " + act + " préstamos activos");
	}

	// ------------------------------------------------------------ accións

	public void novoPrestamo() {
		dialogoPrestamo().showAndWait().ifPresent(msg -> {
			main.estado(msg);
			main.refrescarTodo();
		});
	}

	/** Se hai un préstamo activo seleccionado devólveo; se non, abre o diálogo. */
	public void devolucion() {
		Operacion sel = taboa.getSelectionModel().getSelectedItem();
		if (sel != null && sel.isActivo()) {
			devolucionDirecta(sel);
			return;
		}
		dialogoDevolucion().showAndWait().ifPresent(msg -> {
			main.estado(msg);
			main.refrescarTodo();
		});
	}

	private void devolucionDirecta(Operacion op) {
		if (!Dialogos.confirmar(main.getWindow(), "Rexistrar a devolución?",
				"Obra: " + op.getCodObra() + " — " + op.getTituloObra() + "\nLector: " + op.getCodLector() + " — "
						+ op.getNomeLector() + "\nPrestada o " + op.getFechaPrestamo())) {
			return;
		}
		try {
			dao.devolver(op.getCodOperacion(), LocalDate.now());
			main.estado("Devolución rexistrada: " + op.getTituloObra() + " volve estar dispoñible.");
			main.refrescarTodo();
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Non se puido rexistrar a devolución", e.getMessage());
		}
	}

	private void exportar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Exportar rexistro de préstamos");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));
		fc.setInitialFileName("rexistros_" + LocalDate.now() + ".txt");
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showSaveDialog(main.getWindow());
		if (f == null) return;
		try {
			int n = io.exportarRexistroTXT(f.toPath());
			main.estado("Rexistro exportado (" + n + " operacións) a " + f.getName());
			Dialogos.info(main.getWindow(), "Exportación completada",
					"Exportáronse " + n + " operacións a:\n" + f.getAbsolutePath());
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro durante a exportación", e.getMessage());
		}
	}

	// ----------------------------------------------------------- diálogos

	public Dialog<String> dialogoPrestamo() {
		Dialog<String> d = new Dialog<>();
		d.initOwner(main.getWindow());
		d.setTitle("Novo préstamo");
		d.setHeaderText("Prestar unha obra a un lector");
		Dialogos.estilar(d.getDialogPane());
		ButtonType ok = new ButtonType("Prestar", ButtonType.OK.getButtonData());
		d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

		ComboBox<Lector> lector = new ComboBox<>();
		ComboBox<Obra> obra = new ComboBox<>();
		DatePicker data = UI.selectorData(LocalDate.now());
		lector.setPromptText("Selecciona o lector");
		obra.setPromptText("Selecciona unha obra dispoñible");
		lector.setMaxWidth(Double.MAX_VALUE);
		obra.setMaxWidth(Double.MAX_VALUE);
		lector.setVisibleRowCount(10);
		obra.setVisibleRowCount(10);
		Label info = new Label();
		info.getStyleClass().add("nota");
		Label erro = new Label();
		erro.getStyleClass().add("erro-formulario");
		erro.setWrapText(true);
		try {
			lector.getItems().setAll(lectorDAO.listar());
			obra.getItems().setAll(catalogoDAO.listarDisponibles());
		} catch (Exception e) {
			erro.setText("Erro: " + e.getMessage());
		}
		lector.valueProperty().addListener((o, a, l) -> {
			if (l == null) { info.setText(""); return; }
			try {
				int n = lectorDAO.prestamosActivos(l.getCodLector());
				info.setText(l.getNomeCompleto() + " ten " + n + (n == 1 ? " préstamo activo." : " préstamos activos."));
			} catch (Exception e) {
				info.setText("");
			}
		});
		if (obra.getItems().isEmpty()) erro.setText("Non hai ningunha obra dispoñible para prestar.");
		if (lector.getItems().isEmpty()) erro.setText("Non hai lectores rexistrados.");

		GridPane g = CatalogoView.formulario();
		g.addRow(0, new Label("Lector *"), lector);
		g.add(info, 1, 1);
		g.addRow(2, new Label("Obra *"), obra);
		g.addRow(3, new Label("Data do préstamo"), data);
		g.add(erro, 0, 4, 2, 1);
		d.getDialogPane().setContent(g);

		Node bOk = d.getDialogPane().lookupButton(ok);
		bOk.disableProperty().bind(lector.valueProperty().isNull().or(obra.valueProperty().isNull()));
		bOk.addEventFilter(ActionEvent.ACTION, ev -> {
			try {
				LocalDate dt = data.getValue() == null ? LocalDate.now() : data.getValue();
				if (dt.isAfter(LocalDate.now())) {
					throw new IllegalArgumentException("A data do préstamo non pode ser futura");
				}
				dao.prestar(lector.getValue().getCodLector(), obra.getValue().getCodObra(), dt);
				d.setResult("Préstamo rexistrado: «" + obra.getValue().getTituloObra() + "» a "
						+ lector.getValue().getNomeCompleto() + ".");
			} catch (Exception ex) {
				erro.setText("Erro: " + ex.getMessage());
				ev.consume();
			}
		});
		d.setResultConverter(bt -> bt == ok ? d.getResult() : null);
		return d;
	}

	public Dialog<String> dialogoDevolucion() {
		Dialog<String> d = new Dialog<>();
		d.initOwner(main.getWindow());
		d.setTitle("Nova devolución");
		d.setHeaderText("Rexistrar a devolución dunha obra prestada");
		Dialogos.estilar(d.getDialogPane());
		ButtonType ok = new ButtonType("Rexistrar devolución", ButtonType.OK.getButtonData());
		d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

		ComboBox<Operacion> prestamo = new ComboBox<>();
		prestamo.setPromptText("Selecciona o préstamo activo");
		prestamo.setMaxWidth(Double.MAX_VALUE);
		prestamo.setConverter(new javafx.util.StringConverter<>() {
			@Override
			public String toString(Operacion o) {
				return o == null ? "" : o.getCodObra() + " — " + o.getTituloObra() + "  (" + o.getNomeLector() + ")";
			}

			@Override
			public Operacion fromString(String s) { return null; }
		});
		DatePicker data = UI.selectorData(LocalDate.now());
		Label erro = new Label();
		erro.getStyleClass().add("erro-formulario");
		erro.setWrapText(true);
		Label info = new Label();
		info.getStyleClass().add("nota");
		try {
			prestamo.getItems().setAll(dao.listarActivos());
		} catch (Exception e) {
			erro.setText("Erro: " + e.getMessage());
		}
		if (prestamo.getItems().isEmpty()) erro.setText("Non hai ningún préstamo activo.");
		prestamo.valueProperty().addListener((o, a, op) -> info.setText(op == null ? ""
				: "Prestada o " + op.getFechaPrestamo() + " a " + op.getCodLector() + " (operación nº "
						+ op.getCodOperacion() + ")."));

		GridPane g = CatalogoView.formulario();
		g.addRow(0, new Label("Obra prestada *"), prestamo);
		g.add(info, 1, 1);
		g.addRow(2, new Label("Data de devolución"), data);
		g.add(erro, 0, 3, 2, 1);
		g.getColumnConstraints().get(1).setPrefWidth(380);
		d.getDialogPane().setContent(g);

		Node bOk = d.getDialogPane().lookupButton(ok);
		bOk.disableProperty().bind(prestamo.valueProperty().isNull());
		bOk.addEventFilter(ActionEvent.ACTION, ev -> {
			try {
				Operacion op = prestamo.getValue();
				LocalDate dt = data.getValue() == null ? LocalDate.now() : data.getValue();
				if (dt.isBefore(LocalDate.parse(op.getFechaPrestamo()))) {
					throw new IllegalArgumentException("A data de devolución non pode ser anterior á do préstamo");
				}
				dao.devolver(op.getCodOperacion(), dt);
				d.setResult("Devolución rexistrada: «" + op.getTituloObra() + "» volve estar dispoñible.");
			} catch (Exception ex) {
				erro.setText("Erro: " + ex.getMessage());
				ev.consume();
			}
		});
		d.setResultConverter(bt -> bt == ok ? d.getResult() : null);
		return d;
	}

	public TableView<Operacion> getTaboa() { return taboa; }
	public ChoiceBox<String> getFiltro() { return filtro; }
}
