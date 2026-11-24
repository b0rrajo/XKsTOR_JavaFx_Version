package com.acarballeira.jdbc_sqlite.xestorBiblioteca.model;

/**
 * Rexistro da táboa PrestamoDevolucion. Inclúe, para mostrar na interface, o
 * nome do lector e o título da obra (obtidos mediante LEFT JOIN).
 */
public class Operacion {
	private final int codOperacion;
	private final String codLector;
	private final String codObra;
	private final String fechaPrestamo;
	private final String fechaDevolucion;
	private final String nomeLector;
	private final String tituloObra;

	public Operacion(int codOperacion, String codLector, String codObra, String fechaPrestamo,
			String fechaDevolucion, String nomeLector, String tituloObra) {
		this.codOperacion = codOperacion;
		this.codLector = codLector;
		this.codObra = codObra;
		this.fechaPrestamo = fechaPrestamo;
		this.fechaDevolucion = fechaDevolucion == null ? "" : fechaDevolucion;
		this.nomeLector = nomeLector;
		this.tituloObra = tituloObra;
	}

	public int getCodOperacion() { return codOperacion; }
	public String getCodLector() { return codLector; }
	public String getCodObra() { return codObra; }
	public String getFechaPrestamo() { return fechaPrestamo; }
	public String getFechaDevolucion() { return fechaDevolucion; }
	public String getNomeLector() { return nomeLector == null ? "(lector dado de baixa)" : nomeLector; }
	public String getTituloObra() { return tituloObra == null ? "(obra descatalogada)" : tituloObra; }
	/** @return true se o préstamo segue aberto (sen data de devolución). */
	public boolean isActivo() { return fechaDevolucion.isBlank(); }
}
