package com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.InvalidArgumentsException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.NotRegisteredException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Operacion;

/** Préstamos e devolucións (táboa PrestamoDevolucion). */
public class PrestamoDAO {

	private static final String SELECT_BASE = """
			SELECT p.codOperacion, p.codLector, p.codObra, p.fechaPrestamo, p.fechaDevolucion,
			       l.nombreLector || ' ' || l.apellidosLector AS nomeLector, c.tituloObra
			FROM PrestamoDevolucion p
			LEFT JOIN Lectores l ON l.codLector = p.codLector
			LEFT JOIN Catalogo c ON c.codObra = p.codObra
			""";

	private Connection con() { return Database.get().conexion(); }

	public List<Operacion> listar() throws SQLException {
		return consultar(SELECT_BASE + " ORDER BY p.codOperacion");
	}

	public List<Operacion> listarActivos() throws SQLException {
		return consultar(SELECT_BASE + " WHERE p.fechaDevolucion = '' ORDER BY p.codOperacion");
	}

	private List<Operacion> consultar(String sql) throws SQLException {
		List<Operacion> lista = new ArrayList<>();
		try (PreparedStatement ps = con().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				lista.add(new Operacion(rs.getInt("codOperacion"), rs.getString("codLector"), rs.getString("codObra"),
						rs.getString("fechaPrestamo"), rs.getString("fechaDevolucion"), rs.getString("nomeLector"),
						rs.getString("tituloObra")));
			}
		}
		return lista;
	}

	/**
	 * Novo préstamo: comproba lector e obra, rexistra a operación e bloquea a
	 * obra. Faise nunha transacción para que as dúas táboas queden coherentes.
	 */
	public void prestar(String codLector, String codObra, LocalDate data)
			throws SQLException, NotRegisteredException, InvalidArgumentsException {
		if (!new LectorDAO().existe(codLector)) {
			throw new NotRegisteredException("O lector " + codLector + " non existe");
		}
		var obra = new CatalogoDAO().buscar(codObra);
		if (obra == null) {
			throw new NotRegisteredException("A obra " + codObra + " non existe");
		}
		if (!obra.isDisponible()) {
			throw new InvalidArgumentsException("A obra " + codObra + " non está dispoñible para empréstito");
		}
		executarEnTransaccion(() -> {
			try (PreparedStatement ins = con().prepareStatement(
					"INSERT INTO PrestamoDevolucion (codLector, codObra, fechaPrestamo, fechaDevolucion) VALUES (?, ?, ?, '')");
					PreparedStatement blo = con()
							.prepareStatement("UPDATE Catalogo SET disponibleObra = 0 WHERE codObra = ?")) {
				ins.setString(1, codLector);
				ins.setString(2, codObra);
				ins.setString(3, data.toString());
				ins.executeUpdate();
				blo.setString(1, codObra);
				blo.executeUpdate();
			}
		});
	}

	/** Devolución dunha operación concreta: pecha o préstamo e libera a obra. */
	public void devolver(int codOperacion, LocalDate data) throws SQLException, InvalidArgumentsException {
		String codObra;
		try (PreparedStatement ps = con().prepareStatement(
				"SELECT codObra FROM PrestamoDevolucion WHERE codOperacion = ? AND fechaDevolucion = ''")) {
			ps.setInt(1, codOperacion);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					throw new InvalidArgumentsException("A operación " + codOperacion + " non é un préstamo activo");
				}
				codObra = rs.getString(1);
			}
		}
		executarEnTransaccion(() -> {
			try (PreparedStatement dev = con().prepareStatement(
					"UPDATE PrestamoDevolucion SET fechaDevolucion = ? WHERE codOperacion = ?");
					PreparedStatement lib = con()
							.prepareStatement("UPDATE Catalogo SET disponibleObra = 1 WHERE codObra = ?")) {
				dev.setString(1, data.toString());
				dev.setInt(2, codOperacion);
				dev.executeUpdate();
				lib.setString(1, codObra);
				lib.executeUpdate();
			}
		});
	}

	@FunctionalInterface
	interface AccionSQL {
		void executar() throws SQLException;
	}

	private void executarEnTransaccion(AccionSQL accion) throws SQLException {
		Connection c = con();
		boolean auto = c.getAutoCommit();
		c.setAutoCommit(false);
		try {
			accion.executar();
			c.commit();
		} catch (SQLException e) {
			c.rollback();
			throw e;
		} finally {
			c.setAutoCommit(auto);
		}
	}
}
