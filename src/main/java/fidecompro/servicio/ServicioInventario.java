package fidecompro.servicio;

import fidecompro.modelo.Categoria;
import fidecompro.modelo.Cliente;
import fidecompro.modelo.Entidad;
import fidecompro.modelo.MovimientoInventario;
import fidecompro.modelo.Producto;
import fidecompro.modelo.Usuario;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ServicioInventario {

    private final RepositorioDatos repositorio;
    private final Map<String, String> sesiones = new HashMap<>();
    private Estado estado;

    public ServicioInventario(RepositorioDatos repositorio)
            throws IOException, ClassNotFoundException {
        this.repositorio = repositorio;
        this.estado = repositorio.cargar();

        if (estado.usuarios.isEmpty()) {
            String sal = Claves.nuevaSal();
            String hash = Claves.hash("Fidecompro2026!", sal);
            Usuario usuario = new Usuario(estado.siguiente++, "caja01", hash, sal);
            estado.usuarios.put(usuario.nombre, usuario);
            repositorio.guardar(estado);
        }
    }

    public synchronized List<List<String>> ejecutar(
            String comando, String token, List<String> campos)
            throws ValidacionException, IOException {

        if ("LOGIN".equals(comando)) {
            return iniciarSesion(campos);
        }

        String usuario = sesiones.get(token);
        if (usuario == null) {
            throw new ValidacionException(
                    "La sesión no es válida. Ingrese nuevamente.");
        }

        return switch (comando) {
            case "LOGOUT" -> {
                validarCampos(campos, 0);
                sesiones.remove(token);
                yield List.of();
            }
            case "CLIENTES" -> buscarClientes(campos);
            case "GUARDAR_CLIENTE" -> guardarCliente(campos);
            case "CATEGORIAS" -> listarCategorias(campos);
            case "CREAR_CATEGORIA" -> crearCategoria(campos);
            case "PRODUCTOS" -> buscarProductos(campos);
            case "CREAR_PRODUCTO" -> crearProducto(campos);
            case "ENTRADA" -> registrarEntrada(campos, usuario);
            case "MOVIMIENTOS" -> listarMovimientos(campos);
            default -> throw new ValidacionException("Operación desconocida.");
        };
    }

    private List<List<String>> iniciarSesion(List<String> campos)
            throws ValidacionException {
        validarCampos(campos, 2);
        String nombre = requerido(campos.get(0), "Usuario");
        Usuario usuario = estado.usuarios.get(nombre);

        if (usuario == null || !usuario.activo
                || !Claves.coincide(campos.get(1), usuario.sal, usuario.hash)) {
            throw new ValidacionException("Usuario o contraseña incorrectos.");
        }

        String token = UUID.randomUUID().toString();
        sesiones.put(token, usuario.nombre);
        return List.of(List.of(token, usuario.nombre));
    }

    private List<List<String>> buscarClientes(List<String> campos)
            throws ValidacionException {
        validarCampos(campos, 1);
        String criterio = campos.get(0).trim().toLowerCase(Locale.ROOT);
        List<List<String>> filas = new ArrayList<>();

        for (Cliente cliente : estado.clientes.values()) {
            if (cliente.identificacion.toLowerCase(Locale.ROOT).contains(criterio)
                    || cliente.nombre.toLowerCase(Locale.ROOT).contains(criterio)) {
                filas.add(List.of(
                        String.valueOf(cliente.id), cliente.identificacion,
                        cliente.nombre, cliente.telefono, cliente.correo,
                        cliente.direccion, String.valueOf(cliente.activo),
                        etiqueta(cliente)));
            }
        }
        return filas;
    }

    private List<List<String>> guardarCliente(List<String> campos)
            throws ValidacionException, IOException {
        validarCampos(campos, 7);

        int id = entero(campos.get(0));
        String identificacion = requerido(campos.get(1), "Identificación");
        String nombre = requerido(campos.get(2), "Nombre");
        String telefono = texto(campos.get(3));
        String correo = texto(campos.get(4));
        String direccion = texto(campos.get(5));
        String activo = campos.get(6);

        if (!correo.isEmpty() && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidacionException("El correo no tiene una estructura válida.");
        }
        if (!"true".equals(activo) && !"false".equals(activo)) {
            throw new ValidacionException("Estado inválido.");
        }
        if (id != 0 && !estado.clientes.containsKey(id)) {
            throw new ValidacionException("El cliente ya no existe.");
        }

        for (Cliente cliente : estado.clientes.values()) {
            if (cliente.id != id
                    && cliente.identificacion.equalsIgnoreCase(identificacion)) {
                throw new ValidacionException("La identificación ya está registrada.");
            }
        }

        Estado nuevo = estado.copia();
        if (id == 0) {
            id = nuevo.siguiente++;
        }
        Cliente cliente = new Cliente(id, identificacion, nombre, telefono,
                correo, direccion, Boolean.parseBoolean(activo));
        nuevo.clientes.put(id, cliente);
        publicar(nuevo);

        return List.of(List.of(String.valueOf(id)));
    }

    private List<List<String>> listarCategorias(List<String> campos)
            throws ValidacionException {
        validarCampos(campos, 0);
        List<List<String>> filas = new ArrayList<>();

        for (Categoria categoria : estado.categorias.values()) {
            if (categoria.activo) {
                filas.add(List.of(String.valueOf(categoria.id),
                        etiqueta(categoria), categoria.detalle));
            }
        }
        return filas;
    }

    private List<List<String>> crearCategoria(List<String> campos)
            throws ValidacionException, IOException {
        validarCampos(campos, 2);
        String nombre = requerido(campos.get(0), "Nombre de categoría");
        String detalle = texto(campos.get(1));

        for (Categoria categoria : estado.categorias.values()) {
            if (categoria.nombre.equalsIgnoreCase(nombre)) {
                throw new ValidacionException("La categoría ya está registrada.");
            }
        }

        Estado nuevo = estado.copia();
        int id = nuevo.siguiente++;
        nuevo.categorias.put(id, new Categoria(id, nombre, detalle));
        publicar(nuevo);

        return List.of(List.of(String.valueOf(id)));
    }

    private List<List<String>> buscarProductos(List<String> campos)
            throws ValidacionException {
        validarCampos(campos, 1);
        String criterio = campos.get(0).trim().toLowerCase(Locale.ROOT);
        List<List<String>> filas = new ArrayList<>();

        for (Producto producto : estado.productos.values()) {
            if (producto.codigo.toLowerCase(Locale.ROOT).contains(criterio)
                    || producto.nombre.toLowerCase(Locale.ROOT).contains(criterio)) {
                filas.add(List.of(
                        String.valueOf(producto.id), producto.codigo, producto.nombre,
                        producto.categoria.nombre, producto.precio.toPlainString(),
                        String.valueOf(producto.existencias), etiqueta(producto)));
            }
        }
        return filas;
    }

    private List<List<String>> crearProducto(List<String> campos)
            throws ValidacionException, IOException {
        validarCampos(campos, 4);

        String codigo = requerido(campos.get(0), "Código");
        String nombre = requerido(campos.get(1), "Nombre");
        Categoria categoria = estado.categorias.get(entero(campos.get(2)));
        if (categoria == null || !categoria.activo) {
            throw new ValidacionException("Seleccione una categoría activa.");
        }

        BigDecimal precio;
        try {
            precio = new BigDecimal(campos.get(3).trim().replace(',', '.'))
                    .setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new ValidacionException(
                    "Use un precio numérico con un máximo de dos decimales.");
        }

        if (precio.signum() <= 0 || precio.compareTo(new BigDecimal("999999999.99")) > 0) {
            throw new ValidacionException("El precio debe ser positivo y menor a mil millones.");
        }

        for (Producto producto : estado.productos.values()) {
            if (producto.codigo.equalsIgnoreCase(codigo)) {
                throw new ValidacionException("El código ya está registrado.");
            }
        }

        Estado nuevo = estado.copia();
        int id = nuevo.siguiente++;
        nuevo.productos.put(id, new Producto(id, codigo, nombre, categoria, precio, 0));
        publicar(nuevo);

        return List.of(List.of(String.valueOf(id)));
    }

    private List<List<String>> registrarEntrada(List<String> campos, String usuario)
            throws ValidacionException, IOException {
        validarCampos(campos, 3);
        int id = entero(campos.get(0));
        int cantidad = entero(campos.get(1));
        String motivo = requerido(campos.get(2), "Motivo");

        Producto producto = estado.productos.get(id);
        if (producto == null || !producto.activo) {
            throw new ValidacionException("Seleccione un producto activo.");
        }
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser un entero mayor que cero.");
        }

        Producto actualizado;
        try {
            actualizado = producto.conEntrada(cantidad);
        } catch (ArithmeticException ex) {
            throw new ValidacionException("La cantidad supera el límite permitido.");
        }

        Estado nuevo = estado.copia();
        nuevo.productos.put(id, actualizado);
        nuevo.movimientos.add(
                new MovimientoInventario(nuevo.siguiente++, id, cantidad, usuario, motivo));
        publicar(nuevo);

        return List.of();
    }

    private List<List<String>> listarMovimientos(List<String> campos)
            throws ValidacionException {
        validarCampos(campos, 0);
        List<List<String>> filas = new ArrayList<>();

        for (MovimientoInventario movimiento : estado.movimientos) {
            Producto producto = estado.productos.get(movimiento.productoId);
            filas.add(List.of(String.valueOf(movimiento.id), producto.codigo,
                    String.valueOf(movimiento.cantidad), movimiento.usuario,
                    movimiento.fecha.toString(), movimiento.motivo));
        }
        return filas;
    }

    private void publicar(Estado nuevo) throws IOException {
        repositorio.guardar(nuevo);
        estado = nuevo;
    }

    private static void validarCampos(List<String> campos, int cantidad)
            throws ValidacionException {
        if (campos.size() != cantidad) {
            throw new ValidacionException("Solicitud incompleta.");
        }
    }

    private static String requerido(String valor, String campo)
            throws ValidacionException {
        String resultado = texto(valor);
        if (resultado.isEmpty()) {
            throw new ValidacionException(campo + " es obligatorio.");
        }
        return resultado;
    }

    private static String texto(String valor) throws ValidacionException {
        if (valor.length() > 250) {
            throw new ValidacionException("El campo supera 250 caracteres.");
        }
        return valor.trim();
    }

    private static int entero(String valor) throws ValidacionException {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            throw new ValidacionException("Use una cantidad o identificador entero válido.");
        }
    }

    private static String etiqueta(Entidad entidad) {
        return entidad.descripcion();
    }
}