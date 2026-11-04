package fidecompro.servicio;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persistencia provisional del avance 2.
 * La integración con JDBC corresponde a la siguiente etapa.
 */
public class RepositorioDatos {

    private final Path archivo;

    public RepositorioDatos(Path archivo) {
        this.archivo = archivo.toAbsolutePath();
    }

    public Estado cargar()
            throws IOException, ClassNotFoundException {

        if (!Files.exists(archivo)) {
            return new Estado();
        }

        try (ObjectInputStream entrada = new ObjectInputStream(
                Files.newInputStream(archivo))) {

            entrada.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter(
                            "maxdepth=30;"
                            + "maxrefs=200000;"
                            + "maxbytes=20000000;"
                            + "fidecompro.modelo.*;"
                            + "fidecompro.servicio.Estado;"
                            + "java.base/*;"
                            + "!*"
                    )
            );

            return (Estado) entrada.readObject();
        }
    }

    public void guardar(Estado estado) throws IOException {
        Files.createDirectories(archivo.getParent());

        Path temporal = Files.createTempFile(
                archivo.getParent(),
                "fidecompro-",
                ".tmp"
        );

        try {
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    Files.newOutputStream(temporal))) {

                salida.writeObject(estado);
            }

            Files.move(
                    temporal,
                    archivo,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}