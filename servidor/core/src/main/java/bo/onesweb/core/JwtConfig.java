package bo.onesweb.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder decodificadorJwtCore(
            @Value("${JWT_ISSUER:}") String issuerConfigurado,
            @Value("${JWT_JWKS_URI:}") String jwksConfigurado,
            @Value("${JWT_AUDIENCE:}") String audienciaConfigurada) {

        String emisor = issuerConfigurado.trim();
        String jwks = jwksConfigurado.trim();
        String audiencia = audienciaConfigurada.trim();

        // Sin proveedor configurado, ningun token se acepta.
        if (emisor.isEmpty() || jwks.isEmpty() || audiencia.isEmpty()) {
            return token -> {
                throw new BadJwtException("Proveedor de identidad no configurado");
            };
        }

        exigirHttps(emisor);
        exigirHttps(jwks);

        HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

        JdkClientHttpRequestFactory peticiones =
            new JdkClientHttpRequestFactory(cliente);
        peticiones.setReadTimeout(Duration.ofSeconds(3));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            // El tipo se comprueba explicitamente en requisitos.
            .validateType(false)
            .restOperations(new RestTemplate(peticiones))
            .build();

        OAuth2TokenValidator<Jwt> requisitos = token -> {
            Instant emitido = token.getIssuedAt();
            Instant vence = token.getExpiresAt();
            String sujeto = token.getSubject();
            Object tipo = token.getHeaders().get("typ");

            boolean valido =
                ("JWT".equals(tipo) || "at+jwt".equals(tipo))
                && sujeto != null && !sujeto.isBlank()
                && token.getAudience() != null
                && token.getAudience().contains(audiencia)
                && emitido != null && vence != null
                && vence.isAfter(emitido)
                && !emitido.isAfter(Instant.now().plusSeconds(30));

            return valido
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "El token no cumple los requisitos", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
            new JwtIssuerValidator(emisor),
            new JwtTimestampValidator(Duration.ofSeconds(30)),
            requisitos
        ));

        return decoder;
    }

    private void exigirHttps(String direccion) {
        try {
            URI uri = URI.create(direccion);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || uri.getPort() == 0
                    || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException(
                "La identidad requiere URLs HTTPS validas, sin credenciales,"
                + " parametros ni fragmentos");
        }
    }
}
