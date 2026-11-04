package fidecompro.red;

import fidecompro.servicio.RepositorioDatos;
import fidecompro.servicio.ServicioInventario;
import fidecompro.servicio.ValidacionException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public final class Servidor {

    public static void main(String[] args) throws Exception {
        int puerto = Integer.getInteger(
                "fidecompro.puerto",
                5050
        );

        Path archivo = Path.of(
                System.getProperty(
                        "fidecompro.datos",
                        "datos/fidecompro.ser"
                )
        );

        ServicioInventario servicio = new ServicioInventario(
                new RepositorioDatos(archivo)
        );

        ThreadPoolExecutor hilos = new ThreadPoolExecutor(
                4,
                8,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64)
        );

        try (SSLServerSocket servidor = (SSLServerSocket)
                SSLServerSocketFactory.getDefault()
                        .createServerSocket(
                                puerto,
                                50,
                                InetAddress.getLoopbackAddress())) {

            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {
                        try {
                            servidor.close();
                        } catch (IOException ignored) {
                        }

                        hilos.shutdownNow();
                    })
            );

            System.out.println(
                    "Fidecompro listo en localhost:"
                    + puerto
                    + " | Datos: "
                    + archivo.toAbsolutePath()
            );

            while (!servidor.isClosed()) {
                SSLSocket socket = (SSLSocket) servidor.accept();

                try {
                    hilos.execute(
                            () -> atender(socket, servicio)
                    );

                } catch (RejectedExecutionException e) {
                    socket.close();
                }
            }

        } finally {
            hilos.shutdownNow();
        }
    }

    private static void atender(
            SSLSocket socket,
            ServicioInventario servicio) {

        try (socket) {
            socket.setSoTimeout(15000);

            DataInputStream entrada = new DataInputStream(
                    socket.getInputStream()
            );

            DataOutputStream salida = new DataOutputStream(
                    socket.getOutputStream()
            );

            String comando = entrada.readUTF();
            String token = entrada.readUTF();

            List<String> campos = Protocolo.leerLista(entrada);

            try {
                List<List<String>> filas = servicio.ejecutar(
                        comando,
                        token,
                        campos
                );

                salida.writeBoolean(true);
                salida.writeUTF("");
                salida.writeInt(filas.size());

                for (List<String> fila : filas) {
                    Protocolo.escribirLista(
                            salida,
                            fila
                    );
                }

            } catch (ValidacionException e) {
                salida.writeBoolean(false);
                salida.writeUTF(e.getMessage());

            } catch (IOException e) {
                System.err.println(
                        "No se pudo persistir: " + e.getMessage()
                );

                salida.writeBoolean(false);

                salida.writeUTF(
                        "No se pudo guardar. No se aplicaron cambios; "
                        + "revise el servidor."
                );
            }

            salida.flush();

        } catch (IOException e) {
            System.err.println(
                    "Conexión finalizada: " + e.getMessage()
            );
        }
    }
}