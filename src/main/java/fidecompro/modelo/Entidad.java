package fidecompro.modelo;

import java.io.Serializable;

/**
 * Define la identidad común de los registros del sistema.
 */
public abstract class Entidad implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int id;

    protected Entidad(int id) {
        this.id = id;
    }

    public abstract String descripcion();

    @Override
    public final String toString() {
        return descripcion();
    }
}