package fidecompro.servicio;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Claves {

    private Claves() {
    }

    public static String nuevaSal() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String hash(String clave, String sal) {
        byte[] bytesSal = Base64.getDecoder().decode(sal);
        PBEKeySpec especificacion =
                new PBEKeySpec(clave.toCharArray(), bytesSal, 120_000, 256);

        try {
            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] resultado = fabrica.generateSecret(especificacion).getEncoded();
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo derivar la clave.", ex);
        } finally {
            especificacion.clearPassword();
        }
    }

    public static boolean coincide(String clave, String sal, String esperado) {
        byte[] obtenido = Base64.getDecoder().decode(hash(clave, sal));
        byte[] guardado = Base64.getDecoder().decode(esperado);
        return MessageDigest.isEqual(obtenido, guardado);
    }
}