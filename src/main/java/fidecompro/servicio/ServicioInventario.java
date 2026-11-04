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
    private Estado estado;

    // Las sesiones no se guardan en el archivo.
    private final Map<String, String> sesiones = new HashMap<>();

    public ServicioInventario(RepositorioDatos repositorio)
            throws Exception {

        this.repositorio = repositorio;
        this.estado = repositorio.cargar();

        // Usuario académico para probar el sistema.
        if (estado.usuarios.isEmpty()) {
            String sal = Claves.nuevaSal();

            Usuario usuario = new Usuario(
                    estado.siguiente++,
                    "caja01",
                    Claves.hash("Fidecompro2026!", sal),
                    sal
            );

            estado.usuarios.put("caja01", usuario);
            repositorio.guardar(estado);
        }
    }

    private static String requerido(String valor, String campo)
            throws ValidacionException {

        if (valor == null || valor.trim().isEmpty()) {
            throw new ValidacionException(
                    campo + " es obligatorio."
            );
        }

        if (valor.length() > 250) {
            throw new ValidacionException(
                    campo + " supera 250 caracteres."
            );
        }

        return valor.trim();
    }

    private static String texto(String valor)
            throws ValidacionException {

        if (valor.length() > 250) {
            throw new ValidacionException(
                    "El campo supera 250 caracteres."
            );
        }

        return valor.trim();
    }

    /**
     * El estado en memoria cambia solo después de guardar correctamente.
     */
    private void publicar(Estado nuevo) throws IOException {
        repositorio.guardar(nuevo);
        estado = nuevo;
    }

    public synchronized List<List<String>> ejecutar(
            String comando,
            String token,
            List<String> parametros)
            throws ValidacionException, IOException {

        if (comando.equals("LOGIN")) {
            argumentos(parametros, 2);

            String nombre = requerido(
                    parametros.get(0),
                    "Usuario"
            );

            Usuario usuario = estado.usuarios.get(nombre);

            if (usuario == null
                    || !usuario.activo
                    || !Claves.coincide(
                            parametros.get(1),
                            usuario.sal,
                            usuario.hash)) {

                throw new ValidacionException(
                        "Usuario o contraseña incorrectos."
                );
            }

            String nuevoToken = UUID.randomUUID().toString();

            sesiones.put(nuevoToken, usuario.nombre);

            return List.of(
                    List.of(nuevoToken, usuario.nombre)
            );
        }

        String usuarioActual = sesiones.get(token);

        if (usuarioActual == null) {
            throw new ValidacionException(
                    "La sesión no es válida. Ingrese nuevamente."
            );
        }

        switch (comando) {
            case "LOGOUT": {
                argumentos(parametros, 0);

                sesiones.remove(token);

                return List.of();
            }

            case "CLIENTES": {
                argumentos(parametros, 1);

                String consulta = parametros.get(0)
                        .trim()
                        .toLowerCase(Locale.ROOT);

                List<List<String>> resultado = new ArrayList<>();

                for (Cliente cliente : estado.clientes.values()) {
                    boolean coincideIdentificacion =
                            cliente.identificacion
                                    .toLowerCase(Locale.ROOT)
                                    .contains(consulta);

                    boolean coincideNombre = cliente.nombre
                            .toLowerCase(Locale.ROOT)
                            .contains(consulta);

                    if (coincideIdentificacion || coincideNombre) {
                        resultado.add(List.of(
                                String.valueOf(cliente.id),
                                cliente.identificacion,
                                cliente.nombre,
                                cliente.telefono,
                                cliente.correo,
                                cliente.direccion,
                                String.valueOf(cliente.activo),
                                etiqueta(cliente)
                        ));
                    }
                }

                return resultado;
            }

            case "GUARDAR_CLIENTE": {
                argumentos(parametros, 7);

                int id = entero(parametros.get(0));

                String identificacion = requerido(
                        parametros.get(1),
                        "Identificación"
                );

                String nombre = requerido(
                        parametros.get(2),
                        "Nombre"
                );

                String telefono = texto(parametros.get(3));
                String correo = texto(parametros.get(4));
                String direccion = texto(parametros.get(5));

                if (!correo.isEmpty()
                        && !correo.matches(
                                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

                    throw new ValidacionException(
                            "El correo no tiene una estructura válida."
                    );
                }

                String valorEstado = parametros.get(6);

                if (!valorEstado.equals("true")
                        && !valorEstado.equals("false")) {

                    throw new ValidacionException(
                            "Estado inválido."
                    );
                }

                if (id != 0
                        && !estado.clientes.containsKey(id)) {

                    throw new ValidacionException(
                            "El cliente ya no existe."
                    );
                }

                for (Cliente cliente : estado.clientes.values()) {
                    if (cliente.id != id
                            && cliente.identificacion
                                    .equalsIgnoreCase(identificacion)) {

                        throw new ValidacionException(
                                "La identificación ya está registrada."
                        );
                    }
                }

                Estado nuevo = estado.copia();

                if (id == 0) {
                    id = nuevo.siguiente++;
                }

                Cliente cliente = new Cliente(
                        id,
                        identificacion,
                        nombre,
                        telefono,
                        correo,
                        direccion,
                        Boolean.parseBoolean(valorEstado)
                );

                nuevo.clientes.put(id, cliente);

                publicar(nuevo);

                return List.of(
                        List.of(String.valueOf(id))
                );
            }

            case "CATEGORIAS": {
                argumentos(parametros, 0);

                List<List<String>> resultado = new ArrayList<>();

                for (Categoria categoria : estado.categorias.values()) {
                    if (categoria.activo) {
                        resultado.add(List.of(
                                String.valueOf(categoria.id),
                                etiqueta(categoria),
                                categoria.detalle
                        ));
                    }
                }

                return resultado;
            }

            case "CREAR_CATEGORIA": {
                argumentos(parametros, 2);

                String nombre = requerido(
                        parametros.get(0),
                        "Nombre de categoría"
                );

                String detalle = texto(parametros.get(1));

                for (Categoria categoria : estado.categorias.values()) {
                    if (categoria.nombre.equalsIgnoreCase(nombre)) {
                        throw new ValidacionException(
                                "La categoría ya está registrada."
                        );
                    }
                }

                Estado nuevo = estado.copia();
                int id = nuevo.siguiente++;

                nuevo.categorias.put(
                        id,
                        new Categoria(id, nombre, detalle)
                );

                publicar(nuevo);

                return List.of(
                        List.of(String.valueOf(id))
                );
            }

            case "PRODUCTOS": {
                argumentos(parametros, 1);

                String consulta = parametros.get(0)
                        .toLowerCase(Locale.ROOT);

                List<List<String>> resultado = new ArrayList<>();

                for (Producto producto : estado.productos.values()) {
                    boolean coincideCodigo = producto.codigo
                            .toLowerCase(Locale.ROOT)
                            .contains(consulta);

                    boolean coincideNombre = producto.nombre
                            .toLowerCase(Locale.ROOT)
                            .contains(consulta);

                    if (coincideCodigo || coincideNombre) {
                        resultado.add(List.of(
                                String.valueOf(producto.id),
                                producto.codigo,
                                producto.nombre,
                                producto.categoria.nombre,
                                producto.precio.toPlainString(),
                                String.valueOf(producto.existencias),
                                etiqueta(producto)
                        ));
                    }
                }

                return resultado;
            }

            case "CREAR_PRODUCTO": {
                argumentos(parametros, 4);

                String codigo = requerido(
                        parametros.get(0),
                        "Código"
                );

                String nombre = requerido(
                        parametros.get(1),
                        "Nombre"
                );

                int categoriaId = entero(parametros.get(2));

                Categoria categoria = estado.categorias.get(
                        categoriaId
                );

                if (categoria == null || !categoria.activo) {
                    throw new ValidacionException(
                            "Seleccione una categoría activa."
                    );
                }

                BigDecimal precio;

                try {
                    precio = new BigDecimal(
                            parametros.get(3)
                                    .trim()
                                    .replace(',', '.')
                    ).setScale(2, RoundingMode.UNNECESSARY);

                } catch (ArithmeticException | NumberFormatException e) {
                    throw new ValidacionException(
                            "Use un precio numérico con un máximo "
                            + "de dos decimales."
                    );
                }

                if (precio.signum() <= 0
                        || precio.compareTo(
                                new BigDecimal("999999999.99")) > 0) {

                    throw new ValidacionException(
                            "El precio debe ser positivo y menor "
                            + "a mil millones."
                    );
                }

                for (Producto producto : estado.productos.values()) {
                    if (producto.codigo.equalsIgnoreCase(codigo)) {
                        throw new ValidacionException(
                                "El código ya está registrado."
                        );
                    }
                }

                Estado nuevo = estado.copia();
                int id = nuevo.siguiente++;

                Producto producto = new Producto(
                        id,
                        codigo,
                        nombre,
                        categoria,
                        precio,
                        0
                );

                nuevo.productos.put(id, producto);

                publicar(nuevo);

                return List.of(
                        List.of(String.valueOf(id))
                );
            }

            case "ENTRADA": {
                argumentos(parametros, 3);

                int productoId = entero(parametros.get(0));
                int cantidad = entero(parametros.get(1));

                String motivo = requerido(
                        parametros.get(2),
                        "Motivo"
                );

                Producto producto = estado.productos.get(
                        productoId
                );

                if (producto == null || !producto.activo) {
                    throw new ValidacionException(
                            "Seleccione un producto activo."
                    );
                }

                if (cantidad <= 0) {
                    throw new ValidacionException(
                            "La cantidad debe ser un entero mayor que cero."
                    );
                }

                Estado nuevo = estado.copia();

                try {
                    nuevo.productos.put(
                            productoId,
                            producto.conEntrada(cantidad)
                    );

                } catch (ArithmeticException e) {
                    throw new ValidacionException(
                            "La cantidad supera el límite permitido."
                    );
                }

                MovimientoInventario movimiento =
                        new MovimientoInventario(
                                nuevo.siguiente++,
                                productoId,
                                cantidad,
                                usuarioActual,
                                motivo
                        );

                nuevo.movimientos.add(movimiento);

                publicar(nuevo);

                return List.of();
            }

            case "MOVIMIENTOS": {
                argumentos(parametros, 0);

                List<List<String>> resultado = new ArrayList<>();

                for (MovimientoInventario movimiento
                        : estado.movimientos) {

                    Producto producto = estado.productos.get(
                            movimiento.productoId
                    );

                    resultado.add(List.of(
                            String.valueOf(movimiento.id),
                            producto.codigo,
                            String.valueOf(movimiento.cantidad),
                            movimiento.usuario,
                            movimiento.fecha.toString(),
                            movimiento.motivo
                    ));
                }

                return resultado;
            }

            default:
                throw new ValidacionException(
                        "Operación desconocida."
                );
        }
    }

    private static void argumentos(
            List<String> parametros,
            int cantidad) throws ValidacionException {

        if (parametros.size() != cantidad) {
            throw new ValidacionException(
                    "Solicitud incompleta."
            );
        }
    }

    private static int entero(String valor)
            throws ValidacionException {

        try {
            return Integer.parseInt(valor.trim());

        } catch (NumberFormatException e) {
            throw new ValidacionException(
                    "Use una cantidad o identificador entero válido."
            );
        }
    }

    /**
     * Polimorfismo: se ejecuta la descripción correspondiente
     * al tipo real de la entidad recibida.
     */
    private static String etiqueta(Entidad entidad) {
        return entidad.descripcion();
    }
}