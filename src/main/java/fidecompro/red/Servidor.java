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

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int puerto = Integer.getInteger("fidecompro.puerto", 5050);
        Path archivo = Path.of(System.getProperty(
                "fidecompro.datos", "datos/fidecompro.ser"));

        ServicioInventario servicio =
                new ServicioInventario(new RepositorioDatos(archivo));

        ThreadPoolExecutor hilos = new ThreadPoolExecutor(
                4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64));

        try (SSLServerSocket servidor = (SSLServerSocket)
                SSLServerSocketFactory.getDefault().createServerSocket(
                        puerto, 50, InetAddress.getLoopbackAddress())) {

            System.out.println("Fidecompro listo en localhost:" + puerto);
            System.out.println("Datos: " + archivo.toAbsolutePath());

            while (!servidor.isClosed()) {
                SSLSocket cliente = (SSLSocket) servidor.accept();

                try {
                    hilos.execute(() -> atender(cliente, servicio));
                } catch (RejectedExecutionException ex) {
                    cliente.close();
                }
            }
        } finally {
            hilos.shutdownNow();
        }
    }

    private static void atender(SSLSocket cliente, ServicioInventario servicio) {
        try (cliente) {
            cliente.setSoTimeout(15_000);

            DataInputStream entrada = new DataInputStream(cliente.getInputStream());
            DataOutputStream salida = new DataOutputStream(cliente.getOutputStream());

            String comando = entrada.readUTF();
            String token = entrada.readUTF();
            List<String> campos = Protocolo.leerLista(entrada);

            List<List<String>> filas;
            try {
                filas = servicio.ejecutar(comando, token, campos);
            } catch (ValidacionException ex) {
                enviarError(salida, ex.getMessage());
                return;
            } catch (IOException ex) {
                System.err.println("Error de persistencia: " + ex.getMessage());
                enviarError(salida, "No se pudo guardar. Revise el servidor.");
                return;
            }

            salida.writeBoolean(true);
            salida.writeUTF("");
            salida.writeInt(filas.size());

            for (List<String> fila : filas) {
                Protocolo.escribirLista(salida, fila);
            }

            salida.flush();
        } catch (IOException ex) {
            System.err.println("Conexión finalizada: " + ex.getMessage());
        }
    }

    private static void enviarError(DataOutputStream salida, String mensaje)
            throws IOException {
        salida.writeBoolean(false);
        salida.writeUTF(mensaje);
        salida.flush();
    }
}