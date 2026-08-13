# AEMET API

Backend Spring Boot que expone una API para buscar municipios y consultar la previsión meteorológica del día siguiente usando AEMET OpenData.

## Qué se ha hecho

- Se ha creado una API REST con endpoints para municipios y previsiones.
- Se ha integrado AEMET OpenData mediante un adaptador HTTP independiente del dominio.
- Se ha definido un modelo de dominio para municipios, previsiones, temperatura, probabilidad de precipitación y estado de los datos.
- Se han separado los casos de uso de entrada y salida mediante puertos.
- Se han implementado servicios de dominio para buscar municipios y obtener previsiones.
- Se ha añadido persistencia en H2 para mantener el catálogo de municipios en memoria durante la ejecución.
- Se ha añadido caché Caffeine para reducir llamadas repetidas a AEMET.
- Se ha implementado un fallback con la última previsión conocida cuando el proveedor externo no está disponible.
- Se han añadido respuestas de error estables para límites de peticiones, respuestas inválidas y caídas de AEMET.
- Se ha configurado CORS para permitir el consumo desde el frontend local.
- Se ha añadido Actuator para facilitar la observabilidad de la aplicación.
- Se ha añadido una imagen Docker multietapa con Maven y Java Runtime 21.

## Por qué se ha hecho así

- La arquitectura basada en puertos y adaptadores evita que el dominio dependa de AEMET o de Spring MVC.
- El adaptador externo concentra los detalles de autenticación, URLs y formato de AEMET.
- La caché reduce latencia y protege el límite de peticiones del proveedor.
- El fallback permite devolver datos útiles durante una interrupción temporal de AEMET, marcándolos como `STALE`.
- H2 evita añadir una base de datos externa para este MVP y simplifica el arranque local.
- Los errores con códigos estables permiten que el frontend distinga un límite de peticiones de una indisponibilidad del proveedor.
- La imagen multietapa deja solo el JAR y el runtime en producción, reduciendo el tamaño y la superficie de la imagen.

## Requisitos

- Java 21.
- Maven 3.9 o Maven Wrapper.
- Una clave de AEMET OpenData.

## Configuración

Configura las variables de entorno antes de arrancar:

```env
AEMET_API_KEY=tu-clave-de-aemet
AEMET_BASE_URL=https://opendata.aemet.es/opendata
AEMET_TIMEOUT=5s
FORECAST_CACHE_TTL=4h
FORECAST_FALLBACK_CACHE_TTL=24h
CORS_ALLOWED_ORIGINS=http://localhost:4200
SERVER_PORT=8080
```

La clave de AEMET debe mantenerse fuera de Git y no debe incluirse en la imagen Docker.

## Desarrollo local

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
./mvnw.cmd spring-boot:run
```

La API estará disponible en `http://localhost:8080`.

## API principal

Buscar municipios:

```text
GET /api/v1/municipalities?name=madrid
```

Consultar previsión:

```text
GET /api/v1/forecast/28079?unit=G_CEL
```

Unidades admitidas: `G_CEL` y `G_FAH`.

La respuesta de previsión incluye `averageTemperature`, `temperatureUnit`, `precipitationProbability`, `dataStatus` y `retrievedAt`.

## Pruebas

```bash
./mvnw test
```

## Docker

Construir la imagen:

```bash
docker build -t aemet-backend .
```

Ejecutar el contenedor:

```bash
docker run --rm -p 8080:8080 --env-file .env aemet-backend
```
