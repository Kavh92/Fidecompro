package fidecompro.modelo;

import java.math.BigDecimal;

public final class Producto extends Entidad {

    private static final long serialVersionUID = 1L;

    public final String codigo;
    public final String nombre;
    public final Categoria categoria;
    public final BigDecimal precio;
    public final int existencias;
    public final boolean activo;

    public Producto(int id, String codigo, String nombre,
                    Categoria categoria, BigDecimal precio, int existencias) {
        super(id);
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.existencias = existencias;
        this.activo = true;
    }

    public Producto conEntrada(int cantidad) {
        int nuevasExistencias = Math.addExact(existencias, cantidad);
        return new Producto(id, codigo, nombre, categoria, precio, nuevasExistencias);
    }

    @Override
    public String descripcion() {
        return codigo + " - " + nombre;
    }
}