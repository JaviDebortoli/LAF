# LAF Backend (Spring Boot)

Spring Boot backend (Java 25) implementing the core inference logic of the Label-Based Argumentation Framework (LAF).

## Responsibilities

- Parse and process facts and rules with label vectors.
- Apply support, aggregation, and conflict operations per attribute.
- Build and expose the inferred argumentative graph via REST.

Main endpoint:

- `POST /api/graph`

## Requirements

- Java 25
- Maven Wrapper (included)

## Run locally

From this directory (`LAF/`):

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Default URL: `http://localhost:8080`.

## Build and test

```bash
./mvnw test
./mvnw package
```

On Windows:

```bash
mvnw.cmd test
mvnw.cmd package
```

## Input/Output contract

- Input: facts, rules, and operations per label.
- Output: graph with typed nodes and edges (`nodes[]`, `edges[]`).

This backend is consumed by the Angular UI in `../UI/` and is aligned with the root `README.md` workflow.
