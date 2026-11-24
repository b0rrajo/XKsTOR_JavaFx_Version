package com.acarballeira.jdbc_sqlite.xestorBiblioteca.model;

/** Obra (libro ou material bibliográfico) do catálogo. */
public class Obra {
	private final String codObra;
	private String tituloObra;
	private String autorObra;
	private String editorialObra;
	private int anoObra;
	private boolean disponible;

	public Obra(String codObra, String tituloObra, String autorObra, String editorialObra, int anoObra,
			boolean disponible) {
		this.codObra = codObra;
		this.tituloObra = tituloObra;
		this.autorObra = autorObra;
		this.editorialObra = editorialObra;
		this.anoObra = anoObra;
		this.disponible = disponible;
	}

	public String getCodObra() { return codObra; }
	public String getTituloObra() { return tituloObra; }
	public String getAutorObra() { return autorObra; }
	public String getEditorialObra() { return editorialObra; }
	public int getAnoObra() { return anoObra; }
	public boolean isDisponible() { return disponible; }

	public void setTituloObra(String v) { tituloObra = v; }
	public void setAutorObra(String v) { autorObra = v; }
	public void setEditorialObra(String v) { editorialObra = v; }
	public void setAnoObra(int v) { anoObra = v; }
	public void setDisponible(boolean v) { disponible = v; }

	@Override
	public String toString() {
		return codObra + " — " + tituloObra + " (" + autorObra + ")";
	}
}
