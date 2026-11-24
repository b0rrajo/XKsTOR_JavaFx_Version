package com.acarballeira.jdbc_sqlite.xestorBiblioteca.model;

/** Lector (usuario) da biblioteca. */
public class Lector {
	private final String codLector;
	private String dniLector;
	private String nombreLector;
	private String apellidosLector;
	private int edadLector;

	public Lector(String codLector, String dniLector, String nombreLector, String apellidosLector, int edadLector) {
		this.codLector = codLector;
		this.dniLector = dniLector;
		this.nombreLector = nombreLector;
		this.apellidosLector = apellidosLector;
		this.edadLector = edadLector;
	}

	public String getCodLector() { return codLector; }
	public String getDniLector() { return dniLector; }
	public String getNombreLector() { return nombreLector; }
	public String getApellidosLector() { return apellidosLector; }
	public int getEdadLector() { return edadLector; }
	public String getNomeCompleto() { return nombreLector + " " + apellidosLector; }

	public void setDniLector(String v) { dniLector = v; }
	public void setNombreLector(String v) { nombreLector = v; }
	public void setApellidosLector(String v) { apellidosLector = v; }
	public void setEdadLector(int v) { edadLector = v; }

	@Override
	public String toString() {
		return codLector + " — " + getNomeCompleto();
	}
}
