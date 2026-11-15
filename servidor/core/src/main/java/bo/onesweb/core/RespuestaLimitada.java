package bo.onesweb.core;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import org.springframework.http.HttpStatus;

public class RespuestaLimitada
        implements HttpResponse.BodySubscriber<byte[]> {

    private static final int LIMITE = 10 * 1024 * 1024;
    private final ByteArrayOutputStream datos = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> resultado = new CompletableFuture<>();
    private Flow.Subscription suscripcion;

    @Override
    public CompletionStage<byte[]> getBody() {
        return resultado;
    }

    @Override
    public void onSubscribe(Flow.Subscription suscripcion) {
        this.suscripcion = suscripcion;
        suscripcion.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> bloques) {
        for (ByteBuffer bloque : bloques) {
            if (bloque.remaining() > LIMITE - datos.size()) {
                suscripcion.cancel();
                resultado.completeExceptionally(new IntegracionException(
                    HttpStatus.BAD_GATEWAY,
                    "respuesta_demasiado_grande",
                    "El servicio supero el limite de respuesta permitido"
                ));
                return;
            }

            byte[] contenido = new byte[bloque.remaining()];
            bloque.get(contenido);
            datos.writeBytes(contenido);
        }

        suscripcion.request(1);
    }

    @Override
    public void onError(Throwable error) {
        resultado.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
        resultado.complete(datos.toByteArray());
    }
}
