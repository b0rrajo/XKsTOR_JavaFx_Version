package com.acarballeira.jdbc_sqlite.xestorBiblioteca.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Database;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.IncorrectFormatException;

/**
 * Copia de seguridade binaria. O formato (tres List&lt;Object[]&gt;
 * serializadas: catálogo, lectores e préstamos) é idéntico ao da versión de
 * consola, polo que as copias son intercambiables entre ambas.
 */
public class BackupService {

	/** Resumo dunha copia: número de filas de cada táboa. */
	public record Resumo(int obras, int lectores, int operacions) {
		@Override
		public String toString() {
			return obras + " obras, " + lectores + " lectores e " + operacions + " operacións";
		}
	}

	private Connection con() { return Database.get().conexion(); }

	public Resumo crearCopia(Path destino) throws SQLException, IOException {
		List<Object[]> catalogo = new ArrayList<>();
		List<Object[]> lectores = new ArrayList<>();
		List<Object[]> prestamos = new ArrayList<>();
		try (Statement st = con().createStatement()) {
			try (ResultSet rs = st.executeQuery(
					"SELECT codObra, tituloObra, autorObra, editorialObra, anoObra, disponibleObra FROM Catalogo")) {
				while (rs.next()) {
					catalogo.add(new Object[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
							rs.getInt(5), rs.getInt(6) });
				}
			}
			try (ResultSet rs = st.executeQuery(
					"SELECT codLector, dniLector, nombreLector, apellidosLector, edadLector FROM Lectores")) {
				while (rs.next()) {
					lectores.add(new Object[] { rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
							rs.getInt(5) });
				}
			}
			try (ResultSet rs = st.executeQuery(
					"SELECT codOperacion, codLector, codObra, fechaPrestamo, fechaDevolucion FROM PrestamoDevolucion")) {
				while (rs.next()) {
					prestamos.add(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
							rs.getString(5) });
				}
			}
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(destino))) {
			oos.writeObject(catalogo);
			oos.writeObject(lectores);
			oos.writeObject(prestamos);
		}
		return new Resumo(catalogo.size(), lectores.size(), prestamos.size());
	}

	/** Le a copia sen tocar a base de datos (para amosar o resumo antes de restaurar). */
	public Resumo inspeccionar(Path origen) throws IOException, IncorrectFormatException {
		Copia c = ler(origen);
		return new Resumo(c.catalogo.size(), c.lectores.size(), c.prestamos.size());
	}

	/** Substitúe todo o contido da base de datos polo da copia, nunha transacción. */
	public Resumo restaurar(Path origen) throws IOException, SQLException, IncorrectFormatException {
		Copia c = ler(origen);
		Connection con = con();
		con.setAutoCommit(false);
		try {
			try (Statement st = con.createStatement()) {
				st.executeUpdate("DELETE FROM PrestamoDevolucion");
				st.executeUpdate("DELETE FROM Catalogo");
				st.executeUpdate("DELETE FROM Lectores");
			}
			try (PreparedStatement ps = con.prepareStatement(
					"INSERT INTO Catalogo (codObra, tituloObra, autorObra, editorialObra, anoObra, disponibleObra) VALUES (?, ?, ?, ?, ?, ?)")) {
				for (Object[] f : c.catalogo) {
					ps.setString(1, (String) f[0]);
					ps.setString(2, (String) f[1]);
					ps.setString(3, (String) f[2]);
					ps.setString(4, (String) f[3]);
					ps.setInt(5, (Integer) f[4]);
					ps.setInt(6, (Integer) f[5]);
					ps.executeUpdate();
				}
			}
			try (PreparedStatement ps = con.prepareStatement(
					"INSERT INTO Lectores (codLector, dniLector, nombreLector, apellidosLector, edadLector) VALUES (?, ?, ?, ?, ?)")) {
				for (Object[] f : c.lectores) {
					ps.setString(1, (String) f[0]);
					ps.setString(2, (String) f[1]);
					ps.setString(3, (String) f[2]);
					ps.setString(4, (String) f[3]);
					ps.setInt(5, (Integer) f[4]);
					ps.executeUpdate();
				}
			}
			try (PreparedStatement ps = con.prepareStatement(
					"INSERT INTO PrestamoDevolucion (codOperacion, codLector, codObra, fechaPrestamo, fechaDevolucion) VALUES (?, ?, ?, ?, ?)")) {
				for (Object[] f : c.prestamos) {
					ps.setInt(1, (Integer) f[0]);
					ps.setString(2, (String) f[1]);
					ps.setString(3, (String) f[2]);
					ps.setString(4, (String) f[3]);
					ps.setString(5, (String) f[4]);
					ps.executeUpdate();
				}
			}
			con.commit();
		} catch (SQLException | RuntimeException e) {
			con.rollback();
			throw e;
		} finally {
			con.setAutoCommit(true);
		}
		return new Resumo(c.catalogo.size(), c.lectores.size(), c.prestamos.size());
	}

	private record Copia(List<Object[]> catalogo, List<Object[]> lectores, List<Object[]> prestamos) {
	}

	@SuppressWarnings("unchecked")
	private Copia ler(Path origen) throws IOException, IncorrectFormatException {
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(origen))) {
			return new Copia((List<Object[]>) ois.readObject(), (List<Object[]>) ois.readObject(),
					(List<Object[]>) ois.readObject());
		} catch (ClassNotFoundException | ClassCastException | java.io.StreamCorruptedException
				| java.io.EOFException e) {
			throw new IncorrectFormatException("O ficheiro non é unha copia de seguridade válida");
		}
	}
}
