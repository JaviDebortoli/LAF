# LAF UI (Angular)

Angular frontend for end-to-end interaction with the LAF backend.

It allows users to:

- Enter a LAF knowledge program (facts and rules with labels).
- Configure operations per label (support, aggregation, conflict).
- Send inference requests to `POST /api/graph`.
- Visualize and inspect the resulting argumentative graph.

## Requirements

- Node.js 20+
- npm 10+
- Backend running at `http://localhost:8080`

## Run locally

### 1) Start backend

From `../LAF/`:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### 2) Start frontend

From this directory (`UI/`):

```bash
npm install
npm run start
```

Frontend URL: `http://localhost:4200`.

In development, `proxy.conf.json` forwards `/api` to `http://localhost:8080`.

## Typical workflow

1. Open `http://localhost:4200`.
2. Load or enter a program in **Knowledge Program**.
3. Configure **Label Operations**.
4. Click **Process**.
5. Inspect graph structure and selected node details.

## Scripts

- `npm run start` - start Angular dev server.
- `npm run test` - run unit tests.
- `npm run build` - create production build.

## Notes

- The UI uses Cytoscape for graph rendering.
- The visual model includes inferential (`dMP`) and conflict (`CA`) aesthetic nodes.
- This project is aligned with the root `README.md` and backend contract.
