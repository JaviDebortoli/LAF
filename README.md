# Labeled Argumentation Frameworks (LAF)

Monorepo with two projects:

- `LAF/`: Spring Boot backend (Java 25) implementing LAF inference logic.
- `UI/`: Angular frontend to load programs, configure label operations, run inference, and visualize the resulting graph.

## Repository structure

- `LAF/` -> REST API (`/api/graph`) and inference engine.
- `UI/` -> Web interface for end-to-end experimentation.
- `docs/` -> Supporting documentation (ignored in this environment).

## Requirements

### Backend
- Java 25
- Maven Wrapper (included in `LAF/`)

### Frontend
- Node.js 20+
- npm 10+

## Local setup

### 1) Start backend

From `LAF/`:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Backend URL: `http://localhost:8080`.

### 2) Start frontend

From `UI/`:

```bash
npm install
npm run start
```

Frontend URL: `http://localhost:4200`.

In development, UI proxy routes `/api` to the backend.

## Typical workflow

1. Enter or load a program in **Knowledge Program**.
2. Configure operations in **Label Operations**.
3. Click **Process**.
4. Analyze graph structure and selected node details.

## Main API

- `POST /api/graph`
  - Input: facts, rules, and label operations.
  - Output: inferred argumentative graph nodes and edges.

## Useful commands

### UI
- `npm run start`
- `npm run test`
- `npm run build`

### Backend
- `./mvnw test`
- `./mvnw package`
- `./mvnw spring-boot:run`

## Notes

- The frontend is focused on academic experimentation and graph analysis.
- The backend and UI are designed to keep the `/api/graph` contract stable.
