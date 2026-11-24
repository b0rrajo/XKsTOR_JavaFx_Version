package com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.InvalidArgumentsException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.NotRegisteredException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.UndestroyException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Lector;

/** Operacións CRUD sobre a táboa Lectores. */
public class LectorDAO {

	private Connection con() { return Database.get().conexion(); }

	public List<Lector> listar() throws SQLException {
		List<Lector> lista = new ArrayList<>();
		try (PreparedStatement ps = con().prepareStatement("SELECT * FROM Lectores ORDER BY codLector");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				lista.add(mapear(rs));
			}
		}
		return lista;
	}

	public Lector buscar(String codLector) throws SQLException {
		try (PreparedStatement ps = con().prepareStatement("SELECT * FROM Lectores WHERE codLector = ?")) {
			ps.setString(1, codLector);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? mapear(rs) : null;
			}
		}
	}

	public boolean existe(String codLector) throws SQLException {
		return buscar(codLector) != null;
	}

	/** Número de préstamos sen devolver que ten o lector. */
	public int prestamosActivos(String codLector) throws SQLException {
		try (PreparedStatement ps = con().prepareStatement(
				"SELECT COUNT(*) FROM PrestamoDevolucion WHERE codLector = ? AND fechaDevolucion = ''")) {
			ps.setString(1, codLector);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	public void alta(Lector l) throws SQLException, InvalidArgumentsException, NotRegisteredException {
		validar(l);
		if (existe(l.getCodLector())) {
			throw new InvalidArgumentsException("Xa existe un lector co código " + l.getCodLector());
		}
		try (PreparedStatement ps = con().prepareStatement(
				"INSERT INTO Lectores (codLector, dniLector, nombreLector, apellidosLector, edadLector) VALUES (?, ?, ?, ?, ?)")) {
			ps.setString(1, l.getCodLector().trim());
			ps.setString(2, l.getDniLector().trim());
			ps.setString(3, l.getNombreLector().trim());
			ps.setString(4, l.getApellidosLector().trim());
			ps.setInt(5, l.getEdadLector());
			ps.executeUpdate();
		}
		if (!existe(l.getCodLector())) {
			throw new NotRegisteredException("Erro inesperado: non se rexistrou o lector na táboa");
		}
	}

	public void modificar(Lector l) throws SQLException, InvalidArgumentsException {
		validar(l);
		try (PreparedStatement ps = con().prepareStatement(
				"UPDATE Lectores SET dniLector = ?, nombreLector = ?, apellidosLector = ?, edadLector = ? WHERE codLector = ?")) {
			ps.setString(1, l.getDniLector().trim());
			ps.setString(2, l.getNombreLector().trim());
			ps.setString(3, l.getApellidosLector().trim());
			ps.setInt(4, l.getEdadLector());
			ps.setString(5, l.getCodLector());
			ps.executeUpdate();
		}
	}

	/** Baixa: elimina o lector se non ten préstamos pendentes. */
	public void baixa(String codLector) throws SQLException, UndestroyException, NotRegisteredException {
		if (!existe(codLector)) {
			throw new NotRegisteredException("Non existe ningún lector co código " + codLector);
		}
		if (prestamosActivos(codLector) > 0) {
			throw new UndestroyException("Existen préstamos sen devolver neste usuario");
		}
		try (PreparedStatement ps = con().prepareStatement("DELETE FROM Lectores WHERE codLector = ?")) {
			ps.setString(1, codLector);
			ps.executeUpdate();
		}
	}

	public static void validar(Lector l) throws InvalidArgumentsException {
		if (l.getCodLector() == null || l.getCodLector().isBlank()) throw new InvalidArgumentsException("O código de lector é obrigatorio");
		if (l.getDniLector() == null || l.getDniLector().isBlank()) throw new InvalidArgumentsException("O DNI é obrigatorio");
		if (l.getNombreLector() == null || l.getNombreLector().isBlank()) throw new InvalidArgumentsException("O nome é obrigatorio");
		if (l.getApellidosLector() == null || l.getApellidosLector().isBlank()) throw new InvalidArgumentsException("Os apelidos son obrigatorios");
		if (l.getEdadLector() <= 0 || l.getEdadLector() > 120) throw new InvalidArgumentsException("A idade debe estar entre 1 e 120");
	}

	static Lector mapear(ResultSet rs) throws SQLException {
		return new Lector(rs.getString("codLector"), rs.getString("dniLector"), rs.getString("nombreLector"),
				rs.getString("apellidosLector"), rs.getInt("edadLector"));
	}
}
