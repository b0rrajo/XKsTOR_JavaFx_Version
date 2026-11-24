package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views;

import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.CatalogoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.LectorDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.PrestamoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Obra;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Operacion;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Dialogos;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.MainWindow;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.MainWindow.Seccion;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.UI;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Vista;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Pantalla de inicio: resumo en cifras, accesos rápidos e últimas operacións. */
public class PanelView implements Vista {

	private final MainWindow main;
	private final Label nObras = new Label("0");
	private final Label nDisp = new Label("0");
	private final Label nLectores = new Label("0");
	private final Label nActivos = new Label("0");
	private final TableView<Operacion> ultimas = new TableView<>();
	private final VBox raiz;

	public PanelView(MainWindow main) {
		this.main = main;

		HBox tarxetas = new HBox(14, UI.tarxeta(nObras, "Obras no catálogo", "t-azul"),
				UI.tarxeta(nDisp, "Obras dispoñibles", "t-verde"),
				UI.tarxeta(nLectores, "Lectores rexistrados", "t-violeta"),
				UI.tarxeta(nActivos, "Préstamos activos", "t-laranxa"));

		Button bPrestar = UI.botonPrimario("Novo préstamo", "engadir");
		Button bDevolver = UI.boton("Rexistrar devolución", "devolver");
		Button bObra = UI.boton("Nova obra", "catalogo");
		Button bLector = UI.boton("Novo lector", "lectores");
		bPrestar.setOnAction(e -> {
			main.ir(Seccion.PRESTAMOS);
			main.<PrestamosView>vista(Seccion.PRESTAMOS).novoPrestamo();
		});
		bDevolver.setOnAction(e -> {
			main.ir(Seccion.PRESTAMOS);
			main.<PrestamosView>vista(Seccion.PRESTAMOS).devolucion();
		});
		bObra.setOnAction(e -> {
			main.ir(Seccion.CATALOGO);
			main.<CatalogoView>vista(Seccion.CATALOGO).novaObra();
		});
		bLector.setOnAction(e -> {
			main.ir(Seccion.LECTORES);
			main.<LectoresView>vista(Seccion.LECTORES).novoLector();
		});
		Label accTit = new Label("Accións rápidas");
		accTit.getStyleClass().add("subtitulo");
		HBox accions = UI.barra(bPrestar, bDevolver, bObra, bLector);

		Label ultTit = new Label("Últimas operacións");
		ultTit.getStyleClass().add("subtitulo");
		ultimas.getColumns().add(UI.columna("Nº", Operacion::getCodOperacion, 45));
		ultimas.getColumns().add(UI.columna("Data", Operacion::getFechaPrestamo, 105));
		ultimas.getColumns().add(UI.columna("Lector", Operacion::getNomeLector, 200));
		ultimas.getColumns().add(UI.columna("Obra", Operacion::getTituloObra, 260));
		ultimas.getColumns().add(UI.columnaEstado("Estado", (Operacion o) -> !o.isActivo(), "DEVOLTO", "EN PRÉSTAMO", 125));
		ultimas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		ultimas.setPlaceholder(new Label("Aínda non hai operacións."));
		VBox.setVgrow(ultimas, Priority.ALWAYS);

		raiz = UI.contedor(UI.cabeceira("Benvido/a a XsTOR",
				"Resumo do estado da biblioteca. Usa o menú da esquerda para navegar entre as seccións."), tarxetas,
				accTit, accions, ultTit, ultimas);
	}

	@Override
	public Node getNode() { return raiz; }

	@Override
	public void refrescar() {
		try {
			List<Obra> obras = new CatalogoDAO().listar();
			nObras.setText(String.valueOf(obras.size()));
			nDisp.setText(String.valueOf(obras.stream().filter(Obra::isDisponible).count()));
			nLectores.setText(String.valueOf(new LectorDAO().listar().size()));
			List<Operacion> ops = new PrestamoDAO().listar();
			nActivos.setText(String.valueOf(ops.stream().filter(Operacion::isActivo).count()));
			// as 10 máis recentes primeiro
			List<Operacion> rev = new java.util.ArrayList<>(ops);
			java.util.Collections.reverse(rev);
			ultimas.getItems().setAll(rev.subList(0, Math.min(10, rev.size())));
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao ler os datos", e.getMessage());
		}
	}
}
