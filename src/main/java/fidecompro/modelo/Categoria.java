package fidecompro.modelo;

public final class Categoria extends Entidad {

    private static final long serialVersionUID = 1L;

    public final String nombre;
    public final String detalle;
    public final boolean activo;

    public Categoria(int id, String nombre, String detalle) {
        super(id);
        this.nombre = nombre;
        this.detalle = detalle;
        this.activo = true;
    }

    @Override
    public String descripcion() {
        return nombre;
    }
}