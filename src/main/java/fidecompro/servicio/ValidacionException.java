package fidecompro.servicio;

public final class ValidacionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}