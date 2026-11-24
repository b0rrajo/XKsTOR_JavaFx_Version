package com.acarballeira.jdbc_sqlite.xestorBiblioteca.ui;

import javafx.scene.Node;

/** Contrato común das pantallas que se amosan na zona central. */
public interface Vista {
	Node getNode();

	/** Volve ler os datos da base de datos. */
	void refrescar();
}
