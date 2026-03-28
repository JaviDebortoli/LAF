# Backend LAF (Spring Boot)

Backend en Spring Boot (Java 25) que implementa la logica principal de inferencia del Label-Based Argumentation Framework (LAF).

## Responsabilidades

- Parsear y procesar hechos y reglas con vectores de labels.
- Aplicar operaciones de soporte, agregacion y conflicto por atributo.
- Construir y exponer por REST el grafo argumentativo inferido.

Endpoint principal:

- `POST /api/graph`

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

Este backend es consumido por la UI Angular en `../UI/` y se alinea con el flujo del `README.md` de la raiz.
