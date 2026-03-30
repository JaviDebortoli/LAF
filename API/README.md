# Backend LAF (Spring Boot)

Backend en Spring Boot (Java 25) que implementa la logica principal de inferencia del Label-Based Argumentation Framework (LAF).

## Responsabilidades

- Parsear y procesar hechos y reglas con vectores de labels.
- Aplicar operaciones de soporte, agregacion y conflicto por atributo.
- Construir y exponer por REST el grafo argumentativo inferido.
- Generar narrativa en lenguaje natural (ingles) a partir del grafo y su trazabilidad.

Endpoint principal:

- `POST /api/graph`

Endpoint de proceso completo (grafo + narrativa):

- `POST /api/graph/process`

## Requisitos

- Java 25
- Maven Wrapper (incluido)

## Ejecutar localmente

Desde este directorio (`LAF/`):

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

URL por defecto: `http://localhost:8080`.

## Compilar y testear

```bash
./mvnw test
./mvnw package
```

En Windows:

```bash
mvnw.cmd test
mvnw.cmd package
```

## Contrato de entrada/salida

- Entrada: hechos, reglas y operaciones por label.
- Salida: grafo con nodos y aristas tipadas (`nodes[]`, `edges[]`).

En `POST /api/graph/process` la salida incluye ademas:

- `narrative`: texto narrativo del experimento.
- `trace`: conclusiones finales, derivaciones, conflictos y ganador por conflicto.
- `meta`: modelo y version de prompt usada.

## Configuracion de narracion LLM

Propiedades relevantes en `application.properties`:

- `laf.narration.llm.enabled`
- `laf.narration.llm.base-url`
- `laf.narration.llm.api-key`
- `laf.narration.llm.model`
- `laf.narration.llm.prompt-version`
- `laf.narration.llm.timeout-ms`

Recomendado: usar variable de entorno para la key:

- `laf.narration.llm.api-key=${OPENAI_API_KEY:}`

Si el servicio LLM no esta disponible, el backend responde `503` con el mensaje:

- `Narrative generation service is temporarily unavailable.`

Este backend es consumido por la UI Angular en `../UI/` y se alinea con el flujo del `README.md` de la raiz.
