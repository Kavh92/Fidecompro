package fidecompro.red;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Protocolo {

    private static final int MAX_CAMPOS = 20;

    private Protocolo() {
    }

    public static void escribirLista(DataOutputStream salida, List<String> campos)
            throws IOException {
        if (campos.size() > MAX_CAMPOS) {
            throw new IOException("Demasiados campos.");
        }

        salida.writeInt(campos.size());

        for (String campo : campos) {
            salida.writeUTF(campo);
        }
    }

    public static List<String> leerLista(DataInputStream entrada)
            throws IOException {
        int cantidad = entrada.readInt();

        if (cantidad < 0 || cantidad > MAX_CAMPOS) {
            throw new IOException("Cantidad de campos inválida.");
        }

        List<String> campos = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            campos.add(entrada.readUTF());
        }

        return campos;
    }
}