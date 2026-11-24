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
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Obra;

/** Operacións CRUD sobre a táboa Catalogo. */
public class CatalogoDAO {

	private Connection con() { return Database.get().conexion(); }

	public List<Obra> listar() throws SQLException {
		List<Obra> lista = new ArrayList<>();
		try (PreparedStatement ps = con().prepareStatement("SELECT * FROM Catalogo ORDER BY codObra");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				lista.add(mapear(rs));
			}
		}
		return lista;
	}

	public List<Obra> listarDisponibles() throws SQLException {
		List<Obra> lista = new ArrayList<>();
		try (PreparedStatement ps = con()
				.prepareStatement("SELECT * FROM Catalogo WHERE disponibleObra = 1 ORDER BY codObra");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				lista.add(mapear(rs));
			}
		}
		return lista;
	}

	public Obra buscar(String codObra) throws SQLException {
		try (PreparedStatement ps = con().prepareStatement("SELECT * FROM Catalogo WHERE codObra = ?")) {
			ps.setString(1, codObra);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? mapear(rs) : null;
			}
		}
	}

	public boolean existe(String codObra) throws SQLException {
		return buscar(codObra) != null;
	}

	/** Catalogar: dá de alta unha obra nova (sempre dispoñible). */
	public void catalogar(Obra o) throws SQLException, InvalidArgumentsException, NotRegisteredException {
		validar(o);
		if (existe(o.getCodObra())) {
			throw new InvalidArgumentsException("Xa existe unha obra co código " + o.getCodObra());
		}
		try (PreparedStatement ps = con().prepareStatement(
				"INSERT INTO Catalogo (codObra, tituloObra, autorObra, editorialObra, anoObra, disponibleObra) VALUES (?, ?, ?, ?, ?, 1)")) {
			ps.setString(1, o.getCodObra().trim());
			ps.setString(2, o.getTituloObra().trim());
			ps.setString(3, o.getAutorObra().trim());
			ps.setString(4, o.getEditorialObra().trim());
			ps.setInt(5, o.getAnoObra());
			ps.executeUpdate();
		}
		if (!existe(o.getCodObra())) {
			throw new NotRegisteredException("Erro inesperado: non se rexistrou a obra na táboa");
		}
	}

	/** Modifica os datos descritivos dunha obra (o código non se pode cambiar). */
	public void modificar(Obra o) throws SQLException, InvalidArgumentsException {
		validar(o);
		try (PreparedStatement ps = con().prepareStatement(
				"UPDATE Catalogo SET tituloObra = ?, autorObra = ?, editorialObra = ?, anoObra = ? WHERE codObra = ?")) {
			ps.setString(1, o.getTituloObra().trim());
			ps.setString(2, o.getAutorObra().trim());
			ps.setString(3, o.getEditorialObra().trim());
			ps.setInt(4, o.getAnoObra());
			ps.setString(5, o.getCodObra());
			ps.executeUpdate();
		}
	}

	/** Descatalogar: elimina a obra se non está prestada actualmente. */
	public void descatalogar(String codObra) throws SQLException, UndestroyException, NotRegisteredException {
		Obra o = buscar(codObra);
		if (o == null) {
			throw new NotRegisteredException("Non existe ningunha obra co código " + codObra);
		}
		if (!o.isDisponible()) {
			throw new UndestroyException("Non se pode eliminar: a obra está actualmente en préstamo");
		}
		try (PreparedStatement ps = con().prepareStatement("DELETE FROM Catalogo WHERE codObra = ?")) {
			ps.setString(1, codObra);
			ps.executeUpdate();
		}
	}

	public static void validar(Obra o) throws InvalidArgumentsException {
		if (o.getCodObra() == null || o.getCodObra().isBlank()) throw new InvalidArgumentsException("O código da obra é obrigatorio");
		if (o.getTituloObra() == null || o.getTituloObra().isBlank()) throw new InvalidArgumentsException("O título é obrigatorio");
		if (o.getAutorObra() == null || o.getAutorObra().isBlank()) throw new InvalidArgumentsException("O autor é obrigatorio");
		if (o.getEditorialObra() == null || o.getEditorialObra().isBlank()) throw new InvalidArgumentsException("A editorial é obrigatoria");
		int anoActual = java.time.Year.now().getValue();
		if (o.getAnoObra() < 0 || o.getAnoObra() > anoActual) {
			throw new InvalidArgumentsException("O ano debe estar entre 0 e " + anoActual);
		}
	}

	static Obra mapear(ResultSet rs) throws SQLException {
		return new Obra(rs.getString("codObra"), rs.getString("tituloObra"), rs.getString("autorObra"),
				rs.getString("editorialObra"), rs.getInt("anoObra"), rs.getInt("disponibleObra") == 1);
	}
}
