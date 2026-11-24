package com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Xestiona a conexión única coa base de datos SQLite e a creación/revisión das
 * táboas. Usa o mesmo ficheiro e o mesmo esquema que a versión de consola
 * (xestorBibliotecas.db), polo que ambas versións son compatibles.
 */
public final class Database {

	public static final String FICHEIRO_POR_DEFECTO = "xestorBibliotecas.db";

	private static Database instancia;
	private final Connection conexion;
	private final Path ficheiro;

	private Database(Path ficheiro) throws SQLException {
		this.ficheiro = ficheiro.toAbsolutePath();
		this.conexion = DriverManager.getConnection("jdbc:sqlite:" + this.ficheiro);
		comprobarEsquema();
	}

	/** Abre (ou crea) a base de datos indicada. Só se chama unha vez ao arrancar. */
	public static synchronized Database abrir(Path ficheiro) throws SQLException {
		if (instancia != null) {
			instancia.pechar();
		}
		instancia = new Database(ficheiro);
		return instancia;
	}

	public static Database get() {
		if (instancia == null) {
			throw new IllegalStateException("A base de datos non está aberta");
		}
		return instancia;
	}

	public Connection conexion() { return conexion; }
	public Path ficheiro() { return ficheiro; }

	/** Crea as táboas se non existen (equivalente a database_check do orixinal). */
	public void comprobarEsquema() throws SQLException {
		try (Statement st = conexion.createStatement()) {
			st.execute("""
					CREATE TABLE IF NOT EXISTS Catalogo(
					codObra TEXT PRIMARY KEY,
					tituloObra TEXT NOT NULL,
					autorObra TEXT NOT NULL,
					editorialObra TEXT NOT NULL,
					anoObra INTEGER NOT NULL,
					disponibleObra INTEGER NOT NULL DEFAULT 1
					)
					""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS Lectores(
					codLector TEXT PRIMARY KEY,
					dniLector TEXT NOT NULL,
					nombreLector TEXT NOT NULL,
					apellidosLector TEXT NOT NULL,
					edadLector INTEGER NOT NULL
					)
					""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS PrestamoDevolucion(
					codOperacion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
					codLector TEXT NOT NULL,
					codObra TEXT NOT NULL,
					fechaPrestamo TEXT NOT NULL,
					fechaDevolucion TEXT NOT NULL,
					FOREIGN KEY (codLector) REFERENCES Lectores(codLector),
					FOREIGN KEY (codObra) REFERENCES Catalogo(codObra)
					)
					""");
		}
	}

	/**
	 * Revisión completa: crea as táboas que falten, executa PRAGMA
	 * integrity_check e comproba incoherencias entre préstamos e dispoñibilidade.
	 *
	 * @return liñas de informe para mostrar ao usuario
	 */
	public List<String> revisar() throws SQLException {
		List<String> informe = new ArrayList<>();
		comprobarEsquema();
		informe.add("[OK]    Táboas Catalogo, Lectores e PrestamoDevolucion presentes.");
		try (Statement st = conexion.createStatement()) {
			try (ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
				String r = rs.next() ? rs.getString(1) : "?";
				informe.add(("ok".equalsIgnoreCase(r) ? "[OK]    " : "[ERRO]  ") + "Integridade do ficheiro SQLite: " + r);
			}
			informe.add("        Obras no catálogo: " + contar(st, "SELECT COUNT(*) FROM Catalogo"));
			informe.add("        Lectores rexistrados: " + contar(st, "SELECT COUNT(*) FROM Lectores"));
			informe.add("        Operacións rexistradas: " + contar(st, "SELECT COUNT(*) FROM PrestamoDevolucion"));
			informe.add("        Préstamos activos: "
					+ contar(st, "SELECT COUNT(*) FROM PrestamoDevolucion WHERE fechaDevolucion = ''"));

			int incoherentes = contar(st, """
					SELECT COUNT(*) FROM Catalogo c WHERE
					 (c.disponibleObra = 0 AND NOT EXISTS (SELECT 1 FROM PrestamoDevolucion p
					   WHERE p.codObra = c.codObra AND p.fechaDevolucion = ''))
					 OR (c.disponibleObra = 1 AND EXISTS (SELECT 1 FROM PrestamoDevolucion p
					   WHERE p.codObra = c.codObra AND p.fechaDevolucion = ''))
					""");
			informe.add((incoherentes == 0 ? "[OK]    " : "[AVISO] ") + "Obras con dispoñibilidade incoherente: " + incoherentes);

			int orfos = contar(st, """
					SELECT COUNT(*) FROM PrestamoDevolucion p WHERE p.fechaDevolucion = '' AND
					 (NOT EXISTS (SELECT 1 FROM Lectores l WHERE l.codLector = p.codLector)
					  OR NOT EXISTS (SELECT 1 FROM Catalogo c WHERE c.codObra = p.codObra))
					""");
			informe.add((orfos == 0 ? "[OK]    " : "[AVISO] ") + "Préstamos activos sen lector ou obra asociada: " + orfos);
		}
		return informe;
	}

	/**
	 * Corrixe a columna disponibleObra a partir dos préstamos activos reais.
	 *
	 * @return número de obras corrixidas
	 */
	public int repararDispoñibilidade() throws SQLException {
		try (Statement st = conexion.createStatement()) {
			int a = st.executeUpdate("""
					UPDATE Catalogo SET disponibleObra = 0 WHERE disponibleObra = 1 AND EXISTS
					 (SELECT 1 FROM PrestamoDevolucion p WHERE p.codObra = Catalogo.codObra AND p.fechaDevolucion = '')
					""");
			int b = st.executeUpdate("""
					UPDATE Catalogo SET disponibleObra = 1 WHERE disponibleObra = 0 AND NOT EXISTS
					 (SELECT 1 FROM PrestamoDevolucion p WHERE p.codObra = Catalogo.codObra AND p.fechaDevolucion = '')
					""");
			return a + b;
		}
	}

	private static int contar(Statement st, String sql) throws SQLException {
		try (ResultSet rs = st.executeQuery(sql)) {
			return rs.next() ? rs.getInt(1) : 0;
		}
	}

	public void pechar() {
		try {
			if (conexion != null && !conexion.isClosed()) {
				conexion.close();
			}
		} catch (SQLException ignored) {
			// nada que facer ao pechar
		}
	}
}
