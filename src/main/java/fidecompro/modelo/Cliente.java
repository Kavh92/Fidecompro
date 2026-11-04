package fidecompro.modelo;

public final class Cliente extends Entidad {

    private static final long serialVersionUID = 1L;

    public final String identificacion;
    public final String nombre;
    public final String telefono;
    public final String correo;
    public final String direccion;
    public final boolean activo;

    public Cliente(
            int id,
            String identificacion,
            String nombre,
            String telefono,
            String correo,
            String direccion,
            boolean activo) {

        super(id);

        this.identificacion = identificacion;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.activo = activo;
    }

    @Override
    public String descripcion() {
        return identificacion + " - " + nombre;
    }
}