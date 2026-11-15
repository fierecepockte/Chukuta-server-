package bo.onesweb.core;

import org.springframework.http.HttpStatus;

public class IntegracionException extends RuntimeException {

    private final HttpStatus estado;
    private final String error;

    public IntegracionException(
        HttpStatus estado,
        String error,
        String mensaje
    ) {
        super(mensaje);
        this.estado = estado;
        this.error = error;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getError() {
        return error;
    }
}
