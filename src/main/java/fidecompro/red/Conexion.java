package fidecompro.red;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class Conexion {

    private final String host;
    private final int puerto;

    public Conexion(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    public List<List<String>> pedir(String comando, String token, String... campos)
            throws IOException {
        try (SSLSocket socket =
                     (SSLSocket) SSLSocketFactory.getDefault().createSocket()) {

            socket.connect(new InetSocketAddress(host, puerto), 5_000);
            socket.setSoTimeout(15_000);

            SSLParameters parametros = socket.getSSLParameters();
            parametros.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parametros);
            socket.startHandshake();

            DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
            DataInputStream entrada = new DataInputStream(socket.getInputStream());

            salida.writeUTF(comando);
            salida.writeUTF(token);
            Protocolo.escribirLista(salida, Arrays.asList(campos));
            salida.flush();

            boolean exito = entrada.readBoolean();
            String mensaje = entrada.readUTF();

            if (!exito) {
                throw new IOException(mensaje);
            }

            int cantidad = entrada.readInt();
            if (cantidad < 0 || cantidad > 100_000) {
                throw new IOException("Respuesta fuera de límite.");
            }

            List<List<String>> filas = new ArrayList<>(cantidad);
            for (int i = 0; i < cantidad; i++) {
                filas.add(Protocolo.leerLista(entrada));
            }

            return filas;
        }
    }
}