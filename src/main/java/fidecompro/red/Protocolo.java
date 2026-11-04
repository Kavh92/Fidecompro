package fidecompro.red;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Protocolo {

    private Protocolo() {
    }

    public static void escribirLista(
            DataOutputStream salida,
            List<String> datos) throws IOException {

        if (datos.size() > 20) {
            throw new IOException("Demasiados campos.");
        }

        salida.writeInt(datos.size());

        for (String dato : datos) {
            salida.writeUTF(dato);
        }
    }

    public static List<String> leerLista(
            DataInputStream entrada) throws IOException {

        int cantidad = entrada.readInt();

        if (cantidad < 0 || cantidad > 20) {
            throw new IOException(
                    "Cantidad de campos inválida."
            );
        }

        List<String> resultado = new ArrayList<>();

        for (int i = 0; i < cantidad; i++) {
            resultado.add(entrada.readUTF());
        }

        return resultado;
    }
}