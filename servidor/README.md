# Ones Web — Core de integración

Backend Java 21 con Spring Boot, Nginx y Docker Compose.
Base preparada para integrar los servicios del equipo.
La integración con el proveedor de identidad y los servicios reales está pendiente.

## Componentes

- gateway: Nginx sin root; recibe las peticiones y dirige /api/ al core.
- core: valida solicitudes, verifica JWT y consulta servicios configurados.
- ones-web-net: red Docker compartida por estos contenedores.

El core no publica un puerto directamente al host.
El gateway escucha en 127.0.0.1:8080 dentro del servidor Ubuntu.

## Arranque

Requiere Docker Engine y Docker Compose 2.24.4 o posterior.

Desde la carpeta del proyecto:

    [ -f .env ] || (umask 077; cp .env.example .env)
    chmod 600 .env
    sudo docker compose up -d --build --wait

Compose combina automáticamente compose.yaml y compose.override.yaml.
Utilizar ambos archivos para conservar las restricciones de seguridad.

Para detener los contenedores:

    sudo docker compose stop

Para iniciarlos nuevamente:

    sudo docker compose up -d --wait

## Configuración

Editar .env únicamente en el servidor.

| Variable | Contenido |
| --- | --- |
| REPORTES_URL | URL completa de la operación GET del servicio de reportes |
| REPORTES_CREAR_URL | URL completa de la operación POST del servicio de reportes |
| ZONAS_URL | Reservada para la futura integración de zonas |
| JWT_ISSUER | Emisor exacto de los tokens; URL HTTPS |
| JWT_JWKS_URI | URL HTTPS de las claves públicas del proveedor de identidad |
| JWT_AUDIENCE | Audiencia que identifica esta API en los tokens |

Las URLs de servicios admiten HTTP o HTTPS, sin credenciales,
parámetros de consulta ni fragmentos en la URL configurada.
Dentro de Docker se utilizan nombres de servicio y puertos internos.

Después de modificar .env:

    sudo docker compose up -d --wait core

Si falta algún dato de identidad, ningún token será aceptado.
Las claves privadas del proveedor de identidad no pertenecen a este proyecto.

## API disponible

| Método | Ruta | Acceso |
| --- | --- | --- |
| GET | /api/health | Público |
| GET | /api/reportes | Permiso reportes:leer:todos |
| POST | /api/reportes | Permiso reportes:crear |

El token se envía mediante Authorization: Bearer.
Los permisos se obtienen de scope o scp.
El permiso de lectura global debe reservarse a usuarios autorizados
para consultar todos los reportes.

Se verifican firma, emisor, audiencia, sujeto y fechas.
Se admiten firmas RS256 y ES256, y tipos JWT o at+jwt.
Los tokens deben contener sub, aud, iss, iat y exp.
Se contempla una tolerancia de reloj de 30 segundos.

Las demás rutas quedan denegadas.
Configurar ZONAS_URL no crea una ruta de zonas automáticamente.

El servicio de reportes debe responder:
- GET: 200 con JSON, o 204 sin contenido.
- POST: 201 con un objeto JSON de confirmación.

## Datos del reporte

- idCliente: UUID obligatorio.
- categoria: texto obligatorio, máximo 80 caracteres.
- descripcion: texto obligatorio, máximo 2000 caracteres.
- fecha: fecha y hora ISO 8601 con zona/desplazamiento.
- ubicacion: latitud, longitud y precisionMetros.
- evidencias: entre 1 y 3 objetos.

Latitud: entre -90 y 90.
Longitud: entre -180 y 180.
Precisión: desde 0 hasta menos de 10 metros.

Cada evidencia contiene:
- tipo: texto obligatorio, máximo 32 caracteres.
- formato: tipo MIME, máximo 80 caracteres.
- contenidoBase64: Base64 válido, máximo 3 MiB decodificados.

El cuerpo JSON completo admite hasta 10 MiB, incluyendo Base64.
El core valida codificación y tamaño; el servicio que almacene
los archivos debe validar también su contenido y formatos permitidos.

## Protección y límites

- Contenedores sin root, sin capacidades adicionales y con raíz de solo lectura.
- Directorios temporales y registros con tamaño limitado.
- Límites de memoria, CPU y procesos.
- Nginx: 10 peticiones por segundo por IP, con ráfagas de hasta 20 excedentes.
- Nginx: hasta 20 conexiones activas contabilizadas por IP.
- Core: hasta 4 solicitudes simultáneas, con excepción de GET /api/health.
- JSON: se rechazan campos desconocidos, duplicados y contenido adicional.
- JSON: límites de tamaño, longitud de valores y profundidad.
- Consultas a servicios con tiempos de espera y respuestas limitadas.
- Los POST no tienen reintentos implementados por la aplicación.

Las conexiones que compartan una IP, por NAT o túneles, compartirán
los límites por IP.

## Integración pendiente

Solicitar por cada servicio:
1. Imagen Docker o Dockerfile y sus instrucciones de arranque.
2. Nombre del servicio, puerto interno y ruta de salud.
3. Rutas, métodos, cuerpos JSON y códigos de respuesta.
4. Nombres de variables necesarias, sin compartir valores secretos.
5. Mecanismo de autenticación y reglas de acceso.

Antes de habilitar servicios reales, acordar y adaptar:
- La autenticación entre servicios y la propagación de identidad.
  El cliente HTTP actual no reenvía el token ni la identidad del usuario.
- La comprobación de propiedad de los recursos y permisos específicos.
- La persistencia y deduplicación de idCliente.
- Los formatos y almacenamiento seguro de evidencias.
- Los contratos de respuestas, filtros y paginación.
- El frontend, HTTPS y la configuración de red y DNS para otros equipos.

El core no implementa actualmente login, base de datos ni almacenamiento
persistente de reportes.

## Git y secretos

Nunca guardar en Git:
- .env ni archivos con credenciales.
- Contraseñas, tokens o claves privadas.
- Directorios .ssh o secrets.

.env.example contiene únicamente nombres y valores vacíos.

infra/ssh/00-ones-web.conf conserva las reglas SSH de esta VM.
Antes de aplicarlas en otro servidor, adaptar AllowUsers y comprobar
que el usuario autorizado puede entrar con su llave.

## Estado de validación

Las imágenes se construyeron y los contenedores alcanzaron Healthy.
La ruta de salud indica que el proceso responde; no comprueba servicios externos.
La integración de identidad, permisos y servicios reales requiere validación
cuando estén disponibles sus configuraciones y contratos.

## Referencias

- https://docs.docker.com/compose/
- https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html
- https://nginx.org/en/docs/
