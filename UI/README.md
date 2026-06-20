# UI LAF (Angular)

Frontend en Angular para la interaccion end-to-end con el backend LAF.

Permite a los usuarios:

- Ingresar un programa de conocimiento LAF (hechos y reglas con labels).
- Configurar operaciones por label (soporte, agregacion, conflicto).
- Procesar experimento via `POST /api/graph/process`.
- Activar o desactivar explicabilidad (LLM) antes de procesar.
- Visualizar e inspeccionar el grafo argumentativo resultante.
- Leer una narrativa en ingles con los hallazgos del experimento cuando la explicabilidad esta activa.

## Requisitos

- Node.js 20+
- npm 10+
- Backend corriendo en `http://localhost:8080`

## Ejecutar localmente

### 1) Iniciar backend

Desde `../LAF/`:

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

### 2) Iniciar frontend

Desde este directorio (`UI/`):

```bash
npm install
npm run start
```

URL del frontend: `http://localhost:4200`.

En desarrollo, `proxy.conf.json` redirige `/api` hacia `http://localhost:8080`.

## Flujo tipico

1. Abrir `http://localhost:4200`.
2. Cargar o ingresar un programa en **Knowledge Program**.
3. Configurar **Label Operations**.
4. Hacer click en **Process**.
5. Inspeccionar la estructura del grafo y el detalle de nodos seleccionados.
6. Si la explicabilidad esta activa y disponible, revisar la narrativa debajo del grafo.

## Scripts

- `npm run start` - inicia el servidor de desarrollo de Angular.
- `npm run test` - ejecuta los tests unitarios.
- `npm run build` - genera el build de produccion.

## Notas

- La UI usa Cytoscape para el renderizado del grafo.
- El modelo visual incluye nodos esteticos inferenciales (`dMP`) y de conflicto (`CA`).
- Si el servicio de narrativa no esta disponible, el grafo igual se muestra y la seccion de explicabilidad informa el error.
- Este proyecto se alinea con el `README.md` raiz y con el contrato del backend.
