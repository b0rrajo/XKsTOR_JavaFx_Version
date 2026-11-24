package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.views;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Rutas;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Database;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.service.BackupService;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Dialogos;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.MainWindow;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.UI;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui.Vista;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Outras opcións: copia de seguridade, restauración e revisión da BD. */
public class OpcionsView implements Vista {

	private final MainWindow main;
	private final BackupService backup = new BackupService();
	private final TextArea informe = new TextArea();
	private final Button reparar = UI.boton("Corrixir dispoñibilidade", "reparar");
	private final Label ultimaCopia = new Label("Aínda non se fixo ningunha copia nesta sesión.");
	private final Node raiz;

	public OpcionsView(MainWindow main) {
		this.main = main;

		Button copia = UI.botonPrimario("Crear copia de seguridade…", "gardar");
		copia.setOnAction(e -> crearCopia());
		ultimaCopia.getStyleClass().add("nota");
		VBox tCopia = bloque("Copia de seguridade",
				"Garda todo o contido (catálogo, lectores e préstamos) nun ficheiro binario. "
						+ "O formato é compatible coa versión de consola do programa.",
				new HBox(10, copia), ultimaCopia);

		Button restaurar = UI.botonPerigo("Restaurar desde copia…", "restaurar");
		restaurar.setOnAction(e -> restaurar());
		VBox tRestaurar = bloque("Restaurar desde copia",
				"Substitúe TODOS os datos actuais polos dunha copia de seguridade. Antes de restaurar amosarase un "
						+ "resumo do contido da copia e pedirase confirmación.",
				new HBox(10, restaurar));

		Button revisar = UI.boton("Revisar base de datos", "revisar");
		revisar.setOnAction(e -> revisar());
		reparar.setOnAction(e -> reparar());
		reparar.setDisable(true);
		informe.setEditable(false);
		informe.setPrefRowCount(11);
		informe.getStyleClass().add("informe");
		informe.setPromptText("Preme «Revisar base de datos» para comprobar as táboas e a integridade.");
		VBox tRevisar = bloque("Revisión da base de datos",
				"Crea as táboas que falten, comproba a integridade do ficheiro SQLite e detecta obras marcadas "
						+ "como prestadas sen préstamo activo (ou viceversa).",
				new HBox(10, revisar, reparar), informe);

		Label ruta = new Label(Database.get().ficheiro().toString());
		ruta.getStyleClass().add("ruta");
		ruta.setWrapText(true);
		VBox tInfo = bloque("Ficheiro de base de datos", "A aplicación traballa sobre este ficheiro SQLite:", ruta);

		VBox col = UI.contedor(UI.cabeceira("Outras opcións",
				"Ferramentas de seguridade e mantemento da base de datos."), tCopia, tRestaurar, tRevisar, tInfo);
		ScrollPane sp = new ScrollPane(col);
		sp.setFitToWidth(true);
		sp.getStyleClass().add("scroll-transparente");
		raiz = sp;
	}

	private VBox bloque(String titulo, String texto, Node... extra) {
		Label t = new Label(titulo);
		t.getStyleClass().add("subtitulo");
		Label d = new Label(texto);
		d.setWrapText(true);
		d.getStyleClass().add("vista-descricion");
		VBox v = new VBox(10, t, d);
		v.getChildren().addAll(extra);
		v.getStyleClass().add("bloque");
		v.setPadding(new Insets(16, 18, 16, 18));
		VBox.setVgrow(v, Priority.NEVER);
		return v;
	}

	@Override
	public Node getNode() { return raiz; }

	@Override
	public void refrescar() {
		// nada que recargar: a vista non mostra datos en táboas
	}

	private void crearCopia() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Gardar copia de seguridade");
		fc.setInitialFileName("copia_seguridade_" + LocalDate.now());
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showSaveDialog(main.getWindow());
		if (f == null) return;
		try {
			BackupService.Resumo r = backup.crearCopia(f.toPath());
			ultimaCopia.setText("Última copia: " + f.getAbsolutePath() + " (" + r + ")");
			main.estado("Copia de seguridade creada: " + f.getName());
			Dialogos.info(main.getWindow(), "Copia creada correctamente",
					"Gardáronse " + r + " en:\n" + f.getAbsolutePath());
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao crear a copia", e.getMessage());
		}
	}

	private void restaurar() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Seleccionar copia de seguridade");
		fc.setInitialDirectory(Rutas.cartafolInicial());
		File f = fc.showOpenDialog(main.getWindow());
		if (f == null) return;
		try {
			BackupService.Resumo r = backup.inspeccionar(f.toPath());
			if (!Dialogos.confirmar(main.getWindow(), "Restaurar a copia " + f.getName() + "?",
					"A copia contén " + r + ".\n\nTODOS os datos actuais serán substituídos. "
							+ "Recoméndase crear antes unha copia do estado actual.")) {
				return;
			}
			backup.restaurar(f.toPath());
			main.refrescarTodo();
			main.estado("Base de datos restaurada desde " + f.getName());
			Dialogos.info(main.getWindow(), "Restauración completada",
					"Restauráronse " + r + " desde:\n" + f.getAbsolutePath());
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro ao restaurar a copia", e.getMessage());
		}
	}

	public void revisar() {
		try {
			List<String> r = Database.get().revisar();
			informe.setText("Revisión feita o " + LocalDate.now() + "\n\n" + String.join("\n", r));
			boolean incoherente = r.stream().anyMatch(s -> s.startsWith("[AVISO] Obras"));
			reparar.setDisable(!incoherente);
			main.estado("Revisión da base de datos feita con éxito.");
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro na revisión", e.getMessage());
		}
	}

	private void reparar() {
		try {
			int n = Database.get().repararDispoñibilidade();
			main.refrescarTodo();
			revisar();
			Dialogos.info(main.getWindow(), "Corrección feita", "Corrixíronse " + n + " obras.");
		} catch (Exception e) {
			Dialogos.erro(main.getWindow(), "Erro na corrección", e.getMessage());
		}
	}
}
