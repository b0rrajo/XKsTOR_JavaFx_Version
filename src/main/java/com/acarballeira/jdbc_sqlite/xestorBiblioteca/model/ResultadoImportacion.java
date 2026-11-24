package com.acarballeira.jdbc_sqlite.xestorBiblioteca.model;

import java.util.ArrayList;
import java.util.List;

/** Resumo dunha importación masiva: contadores e mensaxes de rexeitamento. */
public class ResultadoImportacion {
	private int insertados;
	private int rechazados;
	private final List<String> mensaxes = new ArrayList<>();

	public void insertado() { insertados++; }
	public void rechazado(String motivo) { rechazados++; mensaxes.add(motivo); }

	public int getInsertados() { return insertados; }
	public int getRechazados() { return rechazados; }
	public List<String> getMensaxes() { return mensaxes; }
}
