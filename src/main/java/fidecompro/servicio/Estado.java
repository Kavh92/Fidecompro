package fidecompro.servicio;

import fidecompro.modelo.Categoria;
import fidecompro.modelo.Cliente;
import fidecompro.modelo.MovimientoInventario;
import fidecompro.modelo.Producto;
import fidecompro.modelo.Usuario;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Estado implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
    public final Map<Integer, Categoria> categorias = new LinkedHashMap<>();
    public final Map<Integer, Producto> productos = new LinkedHashMap<>();
    public final List<MovimientoInventario> movimientos = new ArrayList<>();
    public final Map<String, Usuario> usuarios = new LinkedHashMap<>();

    public int siguiente = 1;

    public Estado copia() {
        Estado copia = new Estado();
        copia.clientes.putAll(clientes);
        copia.categorias.putAll(categorias);
        copia.productos.putAll(productos);
        copia.movimientos.addAll(movimientos);
        copia.usuarios.putAll(usuarios);
        copia.siguiente = siguiente;
        return copia;
    }
}