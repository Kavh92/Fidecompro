package fidecompro.modelo;

import java.time.LocalDateTime;

public final class MovimientoInventario extends Entidad {

    private static final long serialVersionUID = 1L;

    public final int productoId;
    public final int cantidad;
    public final String usuario;
    public final String motivo;
    public final LocalDateTime fecha;

    public MovimientoInventario(int id, int productoId, int cantidad,
                                String usuario, String motivo) {
        super(id);
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.usuario = usuario;
        this.motivo = motivo;
        this.fecha = LocalDateTime.now();
    }

    @Override
    public String descripcion() {
        return "Entrada de " + cantidad + " unidades: " + motivo;
    }
}