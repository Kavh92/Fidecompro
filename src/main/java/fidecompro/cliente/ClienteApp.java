package fidecompro.cliente;

import fidecompro.red.Conexion;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class ClienteApp extends JFrame {

    private final Conexion conexion = new Conexion(
            System.getProperty("fidecompro.host", "localhost"),
            Integer.getInteger("fidecompro.puerto", 5050));

    private final JPanel contenido = new JPanel(new BorderLayout(12, 12));
    private final JLabel estado = new JLabel(" ");
    private String token = "";
    private String usuario = "";
    private boolean ocupado;

    public ClienteApp() {
        super("Fidecompro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        contenido.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(contenido, BorderLayout.CENTER);
        estado.setBorder(new EmptyBorder(5, 15, 10, 15));
        add(estado, BorderLayout.SOUTH);

        mostrarIngreso();
    }

    private JPanel pantalla(String titulo) {
        contenido.removeAll();
        getRootPane().setDefaultButton(null);
        estado.setText(" ");

        JLabel encabezado = new JLabel(titulo);
        encabezado.setFont(encabezado.getFont().deriveFont(21f));
        contenido.add(encabezado, BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(10, 10));
        contenido.add(cuerpo, BorderLayout.CENTER);
        return cuerpo;
    }

    private void actualizarPantalla() {
        contenido.revalidate();
        contenido.repaint();
    }

    private void mostrarIngreso() {
        JPanel cuerpo = pantalla("Fidecompro | Iniciar sesión");
        JTextField nombre = new JTextField("caja01", 20);
        JPasswordField clave = new JPasswordField(20);

        JButton ingresar = boton("Ingresar", () -> {
            String nombreIngresado = nombre.getText().trim();
            String claveIngresada = new String(clave.getPassword());
            clave.setText("");

            solicitar("LOGIN", "", respuesta -> {
                token = respuesta.get(0).get(0);
                usuario = respuesta.get(0).get(1);
                mostrarMenu();
            }, nombreIngresado, claveIngresada);
        });

        JPanel caja = new JPanel(new BorderLayout(8, 8));
        caja.add(formulario(
                new String[]{"Usuario", "Contraseña"}, nombre, clave),
                BorderLayout.CENTER);
        caja.add(acciones(ingresar), BorderLayout.SOUTH);
        cuerpo.add(caja, BorderLayout.NORTH);
        getRootPane().setDefaultButton(ingresar);
        actualizarPantalla();
    }

    private void mostrarMenu() {
        JPanel cuerpo = pantalla("Menú principal | " + usuario);
        JPanel opciones = new JPanel(new GridLayout(0, 2, 10, 10));

        opciones.add(boton("Clientes", this::mostrarClientes));
        opciones.add(boton("Productos e inventario", this::mostrarProductos));
        opciones.add(boton("Movimientos de inventario", this::mostrarMovimientos));

        JButton facturacion = new JButton("Facturación | Pendiente");
        facturacion.setEnabled(false);
        opciones.add(facturacion);

        opciones.add(boton("Cerrar sesión", () ->
                solicitar("LOGOUT", token, respuesta -> {
                    token = "";
                    usuario = "";
                    mostrarIngreso();
                })));

        cuerpo.add(opciones, BorderLayout.NORTH);
        actualizarPantalla();
    }

    private void mostrarClientes() {
        JPanel cuerpo = pantalla("Clientes");
        JTextField identificacion = new JTextField();
        JTextField nombre = new JTextField();
        JTextField telefono = new JTextField();
        JTextField correo = new JTextField();
        JTextField direccion = new JTextField();
        JTextField busqueda = new JTextField(18);
        JCheckBox activo = new JCheckBox("Activo", true);

        DefaultTableModel modelo = modeloTabla(
                "ID", "Identificación", "Nombre", "Teléfono",
                "Correo", "Dirección", "Activo");
        JTable tabla = tabla(modelo);

        Runnable limpiar = () -> {
            tabla.clearSelection();
            identificacion.setText("");
            nombre.setText("");
            telefono.setText("");
            correo.setText("");
            direccion.setText("");
            activo.setSelected(true);
        };

        Runnable cargar = () -> solicitar("CLIENTES", token, filas -> {
            limpiar.run();
            llenarTabla(modelo, filas);
            estado.setText(filas.isEmpty()
                    ? "No se encontraron clientes."
                    : "Clientes encontrados: " + filas.size());
        }, busqueda.getText());

        tabla.getSelectionModel().addListSelectionListener(evento -> {
            int fila = filaSeleccionada(tabla);
            if (evento.getValueIsAdjusting() || fila < 0) {
                return;
            }

            identificacion.setText(valor(modelo, fila, 1));
            nombre.setText(valor(modelo, fila, 2));
            telefono.setText(valor(modelo, fila, 3));
            correo.setText(valor(modelo, fila, 4));
            direccion.setText(valor(modelo, fila, 5));
            activo.setSelected(Boolean.parseBoolean(valor(modelo, fila, 6)));
        });

        Consumer<String> guardar = id -> solicitar(
                "GUARDAR_CLIENTE", token, respuesta -> cargar.run(),
                id, identificacion.getText(), nombre.getText(),
                telefono.getText(), correo.getText(), direccion.getText(),
                String.valueOf(activo.isSelected()));

        JButton editar = boton("Guardar edición", () -> {
            int fila = filaSeleccionada(tabla);
            if (fila < 0) {
                aviso("Seleccioná un cliente en la tabla.");
                return;
            }
            guardar.accept(valor(modelo, fila, 0));
        });

        JPanel arriba = new JPanel(new BorderLayout(8, 8));
        arriba.add(formulario(
                new String[]{"Identificación *", "Nombre *", "Teléfono",
                    "Correo", "Dirección", "Estado"},
                identificacion, nombre, telefono, correo, direccion, activo),
                BorderLayout.CENTER);

        arriba.add(acciones(
                boton("Guardar nuevo", () -> guardar.accept("0")),
                editar,
                boton("Limpiar", limpiar),
                new JLabel("Buscar:"),
                busqueda,
                boton("Buscar", cargar),
                boton("Volver", this::mostrarMenu)), BorderLayout.SOUTH);

        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        actualizarPantalla();
        cargar.run();
    }

    private void mostrarProductos() {
        JPanel cuerpo = pantalla("Productos e inventario");
        JTextField codigo = new JTextField();
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        JTextField busqueda = new JTextField(14);
        JComboBox<CategoriaOpcion> categorias = new JComboBox<>();

        DefaultTableModel modelo = modeloTabla(
                "ID", "Código", "Nombre", "Categoría", "Precio", "Existencias");
        JTable tabla = tabla(modelo);

        Runnable cargarProductos = () -> solicitar("PRODUCTOS", token, filas -> {
            llenarTabla(modelo, filas);
            estado.setText(filas.isEmpty()
                    ? "No se encontraron productos."
                    : "Productos encontrados: " + filas.size());
        }, busqueda.getText());

        Consumer<String> cargarCategorias = idSeleccionado ->
                solicitar("CATEGORIAS", token, filas -> {
                    categorias.removeAllItems();
                    for (List<String> fila : filas) {
                        CategoriaOpcion opcion =
                                new CategoriaOpcion(fila.get(0), fila.get(1));
                        categorias.addItem(opcion);

                        if (opcion.id().equals(idSeleccionado)) {
                            categorias.setSelectedItem(opcion);
                        }
                    }
                    cargarProductos.run();
                });

        JButton nuevaCategoria = boton("Nueva categoría", () -> {
            JTextField nombreCategoria = new JTextField(20);
            JTextField detalle = new JTextField(20);

            JPanel campos = formulario(
                    new String[]{"Nombre *", "Detalle"}, nombreCategoria, detalle);

            if (confirmar("Nueva categoría", campos)) {
                solicitar("CREAR_CATEGORIA", token,
                        respuesta -> cargarCategorias.accept(respuesta.get(0).get(0)),
                        nombreCategoria.getText(), detalle.getText());
            }
        });

        JButton guardarProducto = boton("Guardar producto", () -> {
            CategoriaOpcion categoria = (CategoriaOpcion) categorias.getSelectedItem();
            if (categoria == null) {
                aviso("Registrá o seleccioná una categoría.");
                return;
            }

            solicitar("CREAR_PRODUCTO", token, respuesta -> {
                codigo.setText("");
                nombre.setText("");
                precio.setText("");
                cargarProductos.run();
            }, codigo.getText(), nombre.getText(), categoria.id(), precio.getText());
        });

        JButton registrarEntrada = boton("Registrar entrada", () -> {
            int fila = filaSeleccionada(tabla);
            if (fila < 0) {
                aviso("Seleccioná un producto en la tabla.");
                return;
            }

            String id = valor(modelo, fila, 0);
            JTextField cantidad = new JTextField(15);
            JTextField motivo = new JTextField(25);

            JPanel campos = formulario(
                    new String[]{"Producto", "Cantidad *", "Motivo *"},
                    new JLabel(valor(modelo, fila, 2)), cantidad, motivo);

            if (confirmar("Entrada de inventario", campos)) {
                solicitar("ENTRADA", token, respuesta -> cargarProductos.run(),
                        id, cantidad.getText(), motivo.getText());
            }
        });

        JPanel arriba = new JPanel(new BorderLayout(8, 8));
        arriba.add(formulario(
                new String[]{"Código *", "Nombre *", "Categoría *", "Precio en colones *"},
                codigo, nombre, categorias, precio), BorderLayout.CENTER);

        arriba.add(acciones(
                nuevaCategoria, guardarProducto, registrarEntrada,
                new JLabel("Buscar:"), busqueda, boton("Buscar", cargarProductos),
                boton("Volver", this::mostrarMenu)), BorderLayout.SOUTH);

        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        actualizarPantalla();
        cargarCategorias.accept("");
    }

    private void mostrarMovimientos() {
        JPanel cuerpo = pantalla("Movimientos de inventario");
        DefaultTableModel modelo = modeloTabla(
                "ID", "Código del producto", "Cantidad", "Usuario", "Fecha", "Motivo");

        cuerpo.add(new JScrollPane(tabla(modelo)), BorderLayout.CENTER);
        cuerpo.add(acciones(boton("Volver", this::mostrarMenu)), BorderLayout.SOUTH);
        actualizarPantalla();

        solicitar("MOVIMIENTOS", token, filas -> {
            llenarTabla(modelo, filas);
            estado.setText("Movimientos encontrados: " + filas.size());
        });
    }

    private void solicitar(String comando, String sesion,
                           Consumer<List<List<String>>> alCompletar,
                           String... campos) {
        if (ocupado) {
            return;
        }

        ocupado = true;
        estado.setText("Procesando...");

        new SwingWorker<List<List<String>>, Void>() {
            @Override
            protected List<List<String>> doInBackground() throws IOException {
                return conexion.pedir(comando, sesion, campos);
            }

            @Override
            protected void done() {
                ocupado = false;

                try {
                    List<List<String>> respuesta = get();
                    estado.setText("Operación completada.");
                    alCompletar.accept(respuesta);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    aviso("La operación fue interrumpida.");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();

                    if (causa instanceof ConnectException) {
                        aviso("No se pudo conectar. Revisá que el servidor esté abierto.");
                    } else if (causa instanceof SSLHandshakeException) {
                        aviso("Falló la conexión TLS. Revisá el certificado y el truststore.");
                    } else {
                        aviso(causa.getMessage());
                    }
                }
            }
        }.execute();
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(evento -> {
            if (!ocupado) {
                accion.run();
            }
        });
        return boton;
    }

    private JPanel acciones(JComponent... componentes) {
        JPanel panel = new JPanel();
        for (JComponent componente : componentes) {
            panel.add(componente);
        }
        return panel;
    }

    private JPanel formulario(String[] etiquetas, JComponent... campos) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < etiquetas.length; i++) {
            JLabel etiqueta = new JLabel(etiquetas[i]);
            etiqueta.setLabelFor(campos[i]);
            panel.add(etiqueta);
            panel.add(campos[i]);
        }
        return panel;
    }

    private DefaultTableModel modeloTabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        return tabla;
    }

    private void llenarTabla(DefaultTableModel modelo, List<List<String>> filas) {
        modelo.setRowCount(0);
        for (List<String> fila : filas) {
            modelo.addRow(fila.subList(0, modelo.getColumnCount()).toArray());
        }
    }

    private int filaSeleccionada(JTable tabla) {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? -1 : tabla.convertRowIndexToModel(fila);
    }

    private String valor(DefaultTableModel modelo, int fila, int columna) {
        return modelo.getValueAt(fila, columna).toString();
    }

    private boolean confirmar(String titulo, JPanel campos) {
        return JOptionPane.showConfirmDialog(
                this, campos, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void aviso(String mensaje) {
        estado.setText(mensaje);
        JOptionPane.showMessageDialog(this, mensaje, "Fidecompro",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private record CategoriaOpcion(String id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteApp().setVisible(true));
    }
}