package bo.onesweb.core;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClienteServicios {

    private final HttpClient cliente;
    private final IntegracionesProperties configuracion;
    private final CatalogoServicios catalogo;
    private final Semaphore cupos = new Semaphore(4);

    public ClienteServicios(
        HttpClient cliente,
        IntegracionesProperties configuracion,
        CatalogoServicios catalogo
    ) {
        this.cliente = cliente;
        this.configuracion = configuracion;
        this.catalogo = catalogo;
    }

    public HttpResponse<byte[]> consultar(String servicio) {
        HttpRequest peticion = HttpRequest.newBuilder()
            .uri(catalogo.obtenerUrl(servicio))
            .timeout(configuracion.timeoutRespuesta())
            .header("Accept", "application/json")
            .GET()
            .build();

        return ejecutar(peticion);
    }

    public HttpResponse<byte[]> crear(String servicio, byte[] cuerpo) {
        if (cuerpo.length > 10 * 1024 * 1024) {
            throw new IntegracionException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "solicitud_demasiado_grande",
                "El reporte supera el limite de 10 MiB"
            );
        }

        HttpRequest peticion = HttpRequest.newBuilder()
            .uri(catalogo.obtenerUrl(servicio))
            .timeout(configuracion.timeoutRespuesta())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
            .build();

        return ejecutar(peticion);
    }

    private HttpResponse<byte[]> ejecutar(HttpRequest peticion) {
        if (!cupos.tryAcquire()) {
            throw new IntegracionException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "core_ocupado",
                "El core alcanzo su limite de consultas simultaneas"
            );
        }

        CompletableFuture<HttpResponse<byte[]>> pendiente = null;

        try {
            pendiente = cliente.sendAsync(
                peticion, informacion -> new RespuestaLimitada()
            );

            return pendiente.get(
                configuracion.timeoutRespuesta().toMillis(),
                TimeUnit.MILLISECONDS
            );
        } catch (TimeoutException excepcion) {
            throw tiempoAgotado();
        } catch (ExecutionException excepcion) {
            Throwable causa = excepcion.getCause();

            if (causa instanceof IntegracionException error) {
                throw error;
            }
            if (causa instanceof HttpTimeoutException) {
                throw tiempoAgotado();
            }

            throw new IntegracionException(
                HttpStatus.BAD_GATEWAY,
                "servicio_no_disponible",
                "No se pudo obtener respuesta del servicio"
            );
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();

            throw new IntegracionException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "consulta_interrumpida",
                "La consulta fue interrumpida"
            );
        } finally {
            if (pendiente != null && !pendiente.isDone()) {
                pendiente.cancel(true);
            }
            cupos.release();
        }
    }

    private IntegracionException tiempoAgotado() {
        return new IntegracionException(
            HttpStatus.GATEWAY_TIMEOUT,
            "tiempo_agotado",
            "El servicio no respondio dentro del tiempo permitido"
        );
    }
}
