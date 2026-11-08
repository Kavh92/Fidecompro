package fidecompro.modelo;

public final class Usuario extends Entidad {

    private static final long serialVersionUID = 1L;

    public final String nombre;
    public final String hash;
    public final String sal;
    public final boolean activo;

    public Usuario(int id, String nombre, String hash, String sal) {
        super(id);
        this.nombre = nombre;
        this.hash = hash;
        this.sal = sal;
        this.activo = true;
    }

    @Override
    public String descripcion() {
        return nombre;
    }
}