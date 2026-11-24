package com.acarballeira.jdbc_sqlite.xestorBiblioteca.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.CatalogoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.LectorDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.PrestamoDAO;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.exceptions.IncorrectFormatException;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Lector;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Obra;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.Operacion;
import com.acarballeira.jdbc_sqlite.xestorBiblioteca.model.ResultadoImportacion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Importación e exportación nos mesmos formatos que a versión de consola:
 * catálogo en CSV (separado por ';'), lectores en JSON e rexistro de
 * préstamos en TXT.
 */
public class ImportExportService {

	public static final String CABECEIRA_CSV = "codObra;tituloObra;autorObra;editorialObra;anoObra;disponibleObra";

	private final CatalogoDAO catalogoDAO = new CatalogoDAO();
	private final LectorDAO lectorDAO = new LectorDAO();
	private final PrestamoDAO prestamoDAO = new PrestamoDAO();

	// ------------------------------------------------------------------ CSV

	public ResultadoImportacion importarCatalogoCSV(Path ficheiro) throws IOException, SQLException {
		ResultadoImportacion res = new ResultadoImportacion();
		try (BufferedReader br = Files.newBufferedReader(ficheiro, StandardCharsets.UTF_8)) {
			String linea;
			boolean primeira = true;
			int num = 0;
			while ((linea = br.readLine()) != null) {
				num++;
				if (primeira) {
					primeira = false;
					if (linea.startsWith("﻿")) linea = linea.substring(1); // BOM de Excel
					if (linea.toLowerCase().startsWith("codobra;")) continue;
				}
				if (linea.isBlank()) continue;
				String[] d = linea.split(";", -1);
				if (d.length != 6) {
					res.rechazado("Liña " + num + ": número de columnas incorrecto (" + d.length + ")");
					continue;
				}
				boolean baleiro = false;
				for (String s : d) baleiro |= s.isBlank();
				if (baleiro) {
					res.rechazado("Liña " + num + ": faltan datos obrigatorios");
					continue;
				}
				int ano, disp;
				try {
					ano = Integer.parseInt(d[4].trim());
					disp = Integer.parseInt(d[5].trim());
				} catch (NumberFormatException e) {
					res.rechazado("Liña " + num + ": formato numérico incorrecto (ano ou dispoñibilidade)");
					continue;
				}
				if (disp != 0 && disp != 1) {
					res.rechazado("Liña " + num + ": dispoñibilidade debe ser 0 ou 1");
					continue;
				}
				String cod = d[0].trim();
				if (catalogoDAO.existe(cod)) {
					res.rechazado("Liña " + num + ": rexistro duplicado (xa existe " + cod + ")");
					continue;
				}
				try (var ps = com.acarballeira.jdbc_sqlite.xestorBiblioteca.dao.Database.get().conexion()
						.prepareStatement("INSERT INTO Catalogo (codObra, tituloObra, autorObra, editorialObra, anoObra, disponibleObra) VALUES (?, ?, ?, ?, ?, ?)")) {
					ps.setString(1, cod);
					ps.setString(2, d[1].trim());
					ps.setString(3, d[2].trim());
					ps.setString(4, d[3].trim());
					ps.setInt(5, ano);
					ps.setInt(6, disp);
					ps.executeUpdate();
					res.insertado();
				} catch (SQLException e) {
					res.rechazado("Liña " + num + ": erro SQL — " + e.getMessage());
				}
			}
		}
		return res;
	}

	public int exportarCatalogoCSV(Path ficheiro) throws IOException, SQLException {
		List<Obra> obras = catalogoDAO.listar();
		try (BufferedWriter bw = Files.newBufferedWriter(ficheiro, StandardCharsets.UTF_8)) {
			bw.write(CABECEIRA_CSV);
			bw.newLine();
			for (Obra o : obras) {
				bw.write(String.join(";", limpar(o.getCodObra()), limpar(o.getTituloObra()), limpar(o.getAutorObra()),
						limpar(o.getEditorialObra()), String.valueOf(o.getAnoObra()), o.isDisponible() ? "1" : "0"));
				bw.newLine();
			}
		}
		return obras.size();
	}

	// ----------------------------------------------------------------- JSON

	public ResultadoImportacion importarLectoresJSON(Path ficheiro)
			throws IOException, SQLException, IncorrectFormatException {
		ResultadoImportacion res = new ResultadoImportacion();
		JsonElement raiz;
		try (Reader r = Files.newBufferedReader(ficheiro, StandardCharsets.UTF_8)) {
			raiz = JsonParser.parseReader(r);
		} catch (JsonParseException e) {
			throw new IncorrectFormatException("Ilexible: o ficheiro non é JSON válido");
		}
		if (!raiz.isJsonArray()) {
			throw new IncorrectFormatException("Ilexible: o JSON debe ser unha lista [ ... ] de lectores");
		}
		JsonArray arr = raiz.getAsJsonArray();
		int i = 0;
		for (JsonElement el : arr) {
			i++;
			if (!el.isJsonObject()) {
				res.rechazado("Elemento " + i + ": non é un obxecto JSON");
				continue;
			}
			JsonObject o = el.getAsJsonObject();
			String[] campos = { "codLector", "dniLector", "nombreLector", "apellidosLector", "edadLector" };
			boolean ok = true;
			for (String c : campos) ok &= campoValido(o, c);
			if (!ok) {
				res.rechazado("Elemento " + i + ": faltan datos obrigatorios");
				continue;
			}
			int idade;
			try {
				idade = Integer.parseInt(o.get("edadLector").getAsString().trim());
			} catch (NumberFormatException e) {
				res.rechazado("Elemento " + i + ": idade non numérica");
				continue;
			}
			Lector l = new Lector(o.get("codLector").getAsString().trim(), o.get("dniLector").getAsString().trim(),
					o.get("nombreLector").getAsString().trim(), o.get("apellidosLector").getAsString().trim(), idade);
			try {
				lectorDAO.alta(l);
				res.insertado();
			} catch (Exception e) {
				res.rechazado("Elemento " + i + " (" + l.getCodLector() + "): " + e.getMessage());
			}
		}
		return res;
	}

	public int exportarLectoresJSON(Path ficheiro) throws IOException, SQLException {
		JsonArray arr = new JsonArray();
		List<Lector> lectores = lectorDAO.listar();
		for (Lector l : lectores) {
			JsonObject o = new JsonObject();
			o.addProperty("codLector", l.getCodLector());
			o.addProperty("dniLector", l.getDniLector());
			o.addProperty("nombreLector", l.getNombreLector());
			o.addProperty("apellidosLector", l.getApellidosLector());
			o.addProperty("edadLector", l.getEdadLector());
			arr.add(o);
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (Writer w = Files.newBufferedWriter(ficheiro, StandardCharsets.UTF_8)) {
			gson.toJson(arr, w);
		}
		return lectores.size();
	}

	// ------------------------------------------------------------------ TXT

	public int exportarRexistroTXT(Path ficheiro) throws IOException, SQLException {
		List<Operacion> ops = prestamoDAO.listar();
		LocalDate hoxe = LocalDate.now();
		try (BufferedWriter w = Files.newBufferedWriter(ficheiro, StandardCharsets.UTF_8)) {
			w.write("==================== REXISTRO DE PRÉSTAMOS E DEVOLUCIÓNS ====================\n");
			w.write("DATA - " + hoxe + "\n");
			w.write("=============================================================================\n");
			w.write(String.format("%-10s %-14s %-14s %-18s %-18s", "Rexistro", "Código Lector", "Código Libro",
					"Data de Préstamo", "Data de Devolución"));
			w.newLine();
			for (Operacion o : ops) {
				w.write(String.format("%-10s %-14s %-14s %-18s %-18s", o.getCodOperacion(), o.getCodLector(),
						o.getCodObra(), o.getFechaPrestamo(), o.isActivo() ? "(pendente)" : o.getFechaDevolucion()));
				w.newLine();
			}
		}
		return ops.size();
	}

	private static String limpar(String t) {
		return t == null ? "" : t.replace(";", ",").trim();
	}

	private static boolean campoValido(JsonObject o, String c) {
		return o.has(c) && !o.get(c).isJsonNull() && o.get(c).isJsonPrimitive()
				&& !o.get(c).getAsString().isBlank();
	}
}
