package fidecompro.cliente;

import fidecompro.red.Conexion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class ClienteApp extends JPanel {

    private final Conexion conexion = new Conexion(
            System.getProperty("fidecompro.host", "localhost"),
            Integer.getInteger("fidecompro.puerto", 5050)
    );

    private String token = "";
    private String usuario = "";
    private boolean ocupado;

    private final Color azul = new Color(30, 54, 78);
    private final Color fondo = new Color(240, 245, 248);

    private final JLabel estado = new JLabel(" ");

    private JPanel cuerpo;
    private JFrame ventana;

    public ClienteApp() {
        setPreferredSize(new Dimension(1100, 720));
        ingreso();
    }

    private void pantalla(String titulo) {
        removeAll();
        setLayout(new BorderLayout());

        String identificacionUsuario = usuario.isEmpty()
                ? ""
                : "Usuario: " + usuario;

        JLabel encabezado = new JLabel(
                "  Fidecompro     |     "
                + titulo
                + "                         "
                + identificacionUsuario
        );

        encabezado.setOpaque(true);
        encabezado.setBackground(azul);
        encabezado.setForeground(Color.WHITE);

        encabezado.setFont(
                new Font("SansSerif", Font.BOLD, 19)
        );

        encabezado.setPreferredSize(
                new Dimension(0, 65)
        );

        add(encabezado, BorderLayout.NORTH);

        cuerpo = new JPanel(
                new BorderLayout(12, 12)
        );

        cuerpo.setBorder(
                new EmptyBorder(22, 25, 22, 25)
        );

        cuerpo.setBackground(fondo);

        add(cuerpo, BorderLayout.CENTER);

        estado.setText(" ");

        estado.setBorder(
                new EmptyBorder(8, 25, 12, 15)
        );

        add(estado, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private JPanel botones() {
        return new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
    }

    private JButton boton(
            JPanel panel,
            String texto,
            Runnable tarea) {

        JButton boton = new JButton(texto);

        boton.setBackground(azul);
        boton.setForeground(Color.WHITE);

        boton.addActionListener(evento -> {
            if (!ocupado) {
                tarea.run();
            }
        });

        panel.add(boton);

        return boton;
    }

    private JPanel formulario(
            String[] etiquetas,
            JComponent[] campos) {

        JPanel formulario = new JPanel(
                new GridLayout(0, 2, 16, 8)
        );

        formulario.setOpaque(false);

        for (int i = 0; i < etiquetas.length; i++) {
            JPanel celda = new JPanel(
                    new BorderLayout(4, 4)
            );

            celda.setOpaque(false);

            celda.add(
                    new JLabel(etiquetas[i]),
                    BorderLayout.NORTH
            );

            celda.add(
                    campos[i],
                    BorderLayout.CENTER
            );

            formulario.add(celda);
        }

        return formulario;
    }

    /**
     * Ejecuta la comunicación fuera del hilo de eventos de Swing.
     */
    private <T> void trabajar(
            Callable<T> tarea,
            Consumer<T> exito) {

        if (ocupado) {
            return;
        }

        ocupado = true;
        estado.setText("Procesando...");

        if (ventana != null) {
            ventana.getGlassPane().setVisible(true);

            ventana.getGlassPane().setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.WAIT_CURSOR
                    )
            );
        }

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                ocupado = false;

                if (ventana != null) {
                    ventana.getGlassPane().setVisible(false);
                }

                try {
                    T resultado = get();

                    estado.setText("Operación completada.");

                    exito.accept(resultado);

                } catch (Exception e) {
                    Throwable causa = e.getCause() == null
                            ? e
                            : e.getCause();

                    String mensaje = causa.getMessage();

                    if (causa instanceof java.net.ConnectException) {
                        mensaje = "No hay conexión con el servidor. "
                                + "Inícielo y vuelva a intentar.";
                    }

                    estado.setText(mensaje);

                    JOptionPane.showMessageDialog(
                            ClienteApp.this,
                            mensaje,
                            "Revise los datos o la conexión",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void ingreso() {
        pantalla("Ingreso al sistema");

        JTextField nombre = new JTextField("caja01");
        JPasswordField clave = new JPasswordField();

        JPanel datos = formulario(
                new String[]{
                    "Usuario",
                    "Contraseña"
                },
                new JComponent[]{
                    nombre,
                    clave
                }
        );

        JPanel caja = new JPanel(
                new BorderLayout(10, 20)
        );

        caja.setBorder(
                new EmptyBorder(100, 220, 150, 220)
        );

        caja.setOpaque(false);
        caja.add(datos, BorderLayout.NORTH);

        JPanel acciones = botones();

        JButton entrar = boton(
                acciones,
                "Ingresar",
                () -> {
                    String nombreIngresado = nombre.getText();
                    String claveIngresada =
                            new String(clave.getPassword());

                    clave.setText("");

                    trabajar(
                            () -> conexion.pedir(
                                    "LOGIN",
                                    "",
                                    nombreIngresado,
                                    claveIngresada
                            ),
                            resultado -> {
                                token = resultado.get(0).get(0);
                                usuario = resultado.get(0).get(1);

                                menu();
                            }
                    );
                }
        );

        caja.add(acciones, BorderLayout.CENTER);

        cuerpo.add(caja);

        if (getRootPane() != null) {
            getRootPane().setDefaultButton(entrar);
        }

        revalidate();
    }

    private void menu() {
        pantalla("Menú principal");

        JPanel opciones = new JPanel(
                new GridLayout(3, 2, 20, 20)
        );

        opciones.setOpaque(false);

        boton(
                opciones,
                "Clientes",
                this::clientes
        );

        boton(
                opciones,
                "Productos e inventario",
                this::productos
        );

        JButton venta = boton(
                opciones,
                "Nueva factura | Próxima etapa",
                () -> {
                }
        );

        venta.setEnabled(false);

        JButton consulta = boton(
                opciones,
                "Consulta de facturas | Próxima etapa",
                () -> {
                }
        );

        consulta.setEnabled(false);

        boton(
                opciones,
                "Movimientos de inventario",
                this::movimientos
        );

        boton(
                opciones,
                "Cerrar sesión",
                () -> salir(false)
        );

        cuerpo.add(
                new JLabel(
                        "Seleccione una opción. "
                        + "Facturación pendiente para la siguiente entrega."
                ),
                BorderLayout.NORTH
        );

        cuerpo.add(opciones);

        revalidate();
    }

    private void salir(boolean cerrar) {
        trabajar(
                () -> conexion.pedir("LOGOUT", token),
                resultado -> {
                    token = "";
                    usuario = "";

                    if (cerrar && ventana != null) {
                        ventana.dispose();
                    } else {
                        ingreso();
                    }
                }
        );
    }

    private DefaultTableModel modelo(String... nombres) {
        return new DefaultTableModel(nombres, 0) {

            @Override
            public boolean isCellEditable(
                    int fila,
                    int columna) {

                return false;
            }
        };
    }

    private JScrollPane desplazar(JTable tabla) {
        JScrollPane panel = new JScrollPane(tabla);

        panel.setColumnHeaderView(
                tabla.getTableHeader()
        );

        return panel;
    }

    private JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);

        tabla.setRowHeight(27);

        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        return tabla;
    }

    private void clientes() {
        pantalla("Registro de clientes");

        JTextField identificacion = new JTextField();
        JTextField nombre = new JTextField();
        JTextField telefono = new JTextField();
        JTextField correo = new JTextField();
        JTextField direccion = new JTextField();
        JTextField buscar = new JTextField();

        JComboBox<String> activo = new JComboBox<>(
                new String[]{"Activo", "Inactivo"}
        );

        JPanel arriba = new JPanel(
                new BorderLayout(10, 10)
        );

        arriba.setOpaque(false);

        arriba.add(
                formulario(
                        new String[]{
                            "Identificación *",
                            "Nombre completo *",
                            "Teléfono",
                            "Correo",
                            "Dirección",
                            "Estado",
                            "Buscar por identificación o nombre"
                        },
                        new JComponent[]{
                            identificacion,
                            nombre,
                            telefono,
                            correo,
                            direccion,
                            activo,
                            buscar
                        }
                ),
                BorderLayout.CENTER
        );

        DefaultTableModel modelo = modelo(
                "ID",
                "Identificación",
                "Nombre",
                "Teléfono",
                "Correo",
                "Dirección",
                "Activo"
        );

        JTable tabla = tabla(modelo);

        int[] seleccionado = {0};

        Runnable limpiar = () -> {
            seleccionado[0] = 0;

            identificacion.setText("");
            nombre.setText("");
            telefono.setText("");
            correo.setText("");
            direccion.setText("");

            activo.setSelectedIndex(0);

            tabla.clearSelection();
        };

        Runnable cargar = () -> {
            String consulta = buscar.getText();

            trabajar(
                    () -> conexion.pedir(
                            "CLIENTES",
                            token,
                            consulta
                    ),
                    resultado -> {
                        modelo.setRowCount(0);

                        for (List<String> datos : resultado) {
                            Object[] fila = datos
                                    .subList(0, 7)
                                    .toArray();

                            fila[6] = Boolean.parseBoolean(
                                    datos.get(6)
                            ) ? "Activo" : "Inactivo";

                            modelo.addRow(fila);
                        }

                        if (resultado.isEmpty()) {
                            estado.setText(
                                    "No se encontraron clientes."
                            );
                        }
                    }
            );
        };

        tabla.getSelectionModel().addListSelectionListener(
                evento -> {
                    if (evento.getValueIsAdjusting()
                            || tabla.getSelectedRow() < 0) {

                        return;
                    }

                    int fila = tabla.convertRowIndexToModel(
                            tabla.getSelectedRow()
                    );

                    seleccionado[0] = Integer.parseInt(
                            modelo.getValueAt(fila, 0).toString()
                    );

                    identificacion.setText(
                            modelo.getValueAt(fila, 1).toString()
                    );

                    nombre.setText(
                            modelo.getValueAt(fila, 2).toString()
                    );

                    telefono.setText(
                            modelo.getValueAt(fila, 3).toString()
                    );

                    correo.setText(
                            modelo.getValueAt(fila, 4).toString()
                    );

                    direccion.setText(
                            modelo.getValueAt(fila, 5).toString()
                    );

                    activo.setSelectedIndex(
                            modelo.getValueAt(fila, 6)
                                    .equals("Activo") ? 0 : 1
                    );
                }
        );

        JPanel acciones = botones();

        boton(
                acciones,
                "Guardar nuevo",
                () -> guardarCliente(
                        0,
                        identificacion,
                        nombre,
                        telefono,
                        correo,
                        direccion,
                        activo,
                        () -> {
                            limpiar.run();
                            cargar.run();
                        }
                )
        );

        boton(
                acciones,
                "Editar seleccionado",
                () -> {
                    if (seleccionado[0] == 0) {
                        aviso(
                                "Seleccione un cliente en la tabla."
                        );

                        return;
                    }

                    guardarCliente(
                            seleccionado[0],
                            identificacion,
                            nombre,
                            telefono,
                            correo,
                            direccion,
                            activo,
                            () -> {
                                limpiar.run();
                                cargar.run();
                            }
                    );
                }
        );

        boton(acciones, "Buscar", cargar);
        boton(acciones, "Limpiar", limpiar);
        boton(acciones, "Volver", this::menu);

        arriba.add(acciones, BorderLayout.SOUTH);

        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(desplazar(tabla));

        revalidate();

        cargar.run();
    }

    private void guardarCliente(
            int seleccionado,
            JTextField identificacion,
            JTextField nombre,
            JTextField telefono,
            JTextField correo,
            JTextField direccion,
            JComboBox<String> activo,
            Runnable luego) {

        String[] parametros = {
            String.valueOf(seleccionado),
            identificacion.getText(),
            nombre.getText(),
            telefono.getText(),
            correo.getText(),
            direccion.getText(),
            String.valueOf(activo.getSelectedIndex() == 0)
        };

        trabajar(
                () -> conexion.pedir(
                        "GUARDAR_CLIENTE",
                        token,
                        parametros
                ),
                resultado -> luego.run()
        );
    }

    private record Opcion(String id, String nombre) {

        @Override
        public String toString() {
            return nombre;
        }
    }

    private void productos() {
        pantalla("Productos e inventario");

        JTextField codigo = new JTextField();
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        JTextField buscar = new JTextField();

        JComboBox<Opcion> categorias = new JComboBox<>();

        JPanel arriba = new JPanel(
                new BorderLayout(10, 10)
        );

        arriba.setOpaque(false);

        arriba.add(
                formulario(
                        new String[]{
                            "Código *",
                            "Nombre *",
                            "Categoría *",
                            "Precio unitario en colones *",
                            "Buscar por código o nombre"
                        },
                        new JComponent[]{
                            codigo,
                            nombre,
                            categorias,
                            precio,
                            buscar
                        }
                ),
                BorderLayout.CENTER
        );

        DefaultTableModel modelo = modelo(
                "ID",
                "Código",
                "Nombre",
                "Categoría",
                "Precio",
                "Existencias"
        );

        JTable tabla = tabla(modelo);

        Runnable cargar = () -> {
            String consulta = buscar.getText();

            trabajar(
                    () -> conexion.pedir(
                            "PRODUCTOS",
                            token,
                            consulta
                    ),
                    resultado -> {
                        modelo.setRowCount(0);

                        for (List<String> fila : resultado) {
                            modelo.addRow(
                                    fila.subList(0, 6).toArray()
                            );
                        }

                        if (resultado.isEmpty()) {
                            estado.setText(
                                    "No se encontraron productos. "
                                    + "Los nuevos inician con cero existencias."
                            );
                        }
                    }
            );
        };

        Consumer<String> cargarCategorias = seleccion -> trabajar(
                () -> conexion.pedir("CATEGORIAS", token),
                resultado -> {
                    categorias.removeAllItems();

                    for (List<String> fila : resultado) {
                        Opcion opcion = new Opcion(
                                fila.get(0),
                                fila.get(1)
                        );

                        categorias.addItem(opcion);

                        if (opcion.id().equals(seleccion)) {
                            categorias.setSelectedItem(opcion);
                        }
                    }

                    cargar.run();
                }
        );

        JPanel acciones = botones();

        boton(
                acciones,
                "Guardar producto",
                () -> {
                    Opcion categoria = (Opcion)
                            categorias.getSelectedItem();

                    if (categoria == null) {
                        aviso(
                                "Registre una categoría primero."
                        );

                        return;
                    }

                    String codigoIngresado = codigo.getText();
                    String nombreIngresado = nombre.getText();
                    String precioIngresado = precio.getText();

                    trabajar(
                            () -> conexion.pedir(
                                    "CREAR_PRODUCTO",
                                    token,
                                    codigoIngresado,
                                    nombreIngresado,
                                    categoria.id(),
                                    precioIngresado
                            ),
                            resultado -> {
                                codigo.setText("");
                                nombre.setText("");
                                precio.setText("");

                                cargar.run();
                            }
                    );
                }
        );

        boton(
                acciones,
                "Nueva categoría",
                () -> {
                    JTextField nombreCategoria = new JTextField();
                    JTextField descripcion = new JTextField();

                    JPanel formularioCategoria = formulario(
                            new String[]{
                                "Nombre *",
                                "Descripción"
                            },
                            new JComponent[]{
                                nombreCategoria,
                                descripcion
                            }
                    );

                    int respuesta = JOptionPane.showConfirmDialog(
                            this,
                            formularioCategoria,
                            "Nueva categoría",
                            JOptionPane.OK_CANCEL_OPTION
                    );

                    if (respuesta == JOptionPane.OK_OPTION) {
                        String nombreIngresado =
                                nombreCategoria.getText();

                        String descripcionIngresada =
                                descripcion.getText();

                        trabajar(
                                () -> conexion.pedir(
                                        "CREAR_CATEGORIA",
                                        token,
                                        nombreIngresado,
                                        descripcionIngresada
                                ),
                                resultado ->
                                        cargarCategorias.accept(
                                                resultado.get(0).get(0)
                                        )
                        );
                    }
                }
        );

        boton(
                acciones,
                "Registrar entrada",
                () -> {
                    int seleccion = tabla.getSelectedRow();

                    if (seleccion < 0) {
                        aviso(
                                "Seleccione un producto."
                        );

                        return;
                    }

                    int fila = tabla.convertRowIndexToModel(
                            seleccion
                    );

                    String productoId = modelo
                            .getValueAt(fila, 0)
                            .toString();

                    JTextField cantidad = new JTextField();
                    JTextField motivo = new JTextField();

                    JPanel formularioEntrada = formulario(
                            new String[]{
                                "Producto",
                                "Existencias actuales",
                                "Cantidad recibida *",
                                "Motivo *"
                            },
                            new JComponent[]{
                                new JLabel(
                                        modelo.getValueAt(fila, 2)
                                                .toString()
                                ),
                                new JLabel(
                                        modelo.getValueAt(fila, 5)
                                                .toString()
                                ),
                                cantidad,
                                motivo
                            }
                    );

                    int respuesta = JOptionPane.showConfirmDialog(
                            this,
                            formularioEntrada,
                            "Entrada de inventario",
                            JOptionPane.OK_CANCEL_OPTION
                    );

                    if (respuesta == JOptionPane.OK_OPTION) {
                        String cantidadIngresada =
                                cantidad.getText();

                        String motivoIngresado =
                                motivo.getText();

                        trabajar(
                                () -> conexion.pedir(
                                        "ENTRADA",
                                        token,
                                        productoId,
                                        cantidadIngresada,
                                        motivoIngresado
                                ),
                                resultado -> cargar.run()
                        );
                    }
                }
        );

        boton(acciones, "Buscar", cargar);
        boton(acciones, "Volver", this::menu);

        arriba.add(acciones, BorderLayout.SOUTH);

        cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(desplazar(tabla));

        revalidate();

        cargarCategorias.accept("");
    }

    private void movimientos() {
        pantalla("Movimientos de inventario");

        DefaultTableModel modelo = modelo(
                "ID",
                "Código producto",
                "Cantidad",
                "Usuario",
                "Fecha",
                "Motivo"
        );

        cuerpo.add(
                desplazar(tabla(modelo))
        );

        JPanel acciones = botones();

        boton(
                acciones,
                "Volver",
                this::menu
        );

        cuerpo.add(
                acciones,
                BorderLayout.SOUTH
        );

        revalidate();

        trabajar(
                () -> conexion.pedir(
                        "MOVIMIENTOS",
                        token
                ),
                resultado -> {
                    for (List<String> fila : resultado) {
                        modelo.addRow(fila.toArray());
                    }
                }
        );
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Fidecompro",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            ClienteApp aplicacion = new ClienteApp();

            JFrame ventana = new JFrame(
                    "Fidecompro | Inventario y facturación | Avance 2"
            );

            aplicacion.ventana = ventana;

            ventana.setContentPane(aplicacion);
            ventana.setMinimumSize(new Dimension(980, 650));
            ventana.pack();
            ventana.setLocationRelativeTo(null);

            ventana.setDefaultCloseOperation(
                    JFrame.DO_NOTHING_ON_CLOSE
            );

            ventana.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent evento) {
                    if (aplicacion.ocupado) {
                        return;
                    }

                    if (aplicacion.token.isEmpty()) {
                        ventana.dispose();
                    } else {
                        aplicacion.salir(true);
                    }
                }
            });

            ventana.setVisible(true);
        });
    }
}