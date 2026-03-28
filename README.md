# LAF - Implementacion del formalismo

Este README documenta la implementacion del formalismo **Label-Based Argumentation Framework (LAF)** en este monorepo.

No reemplaza los README de `API/` y `UI/`: esos documentos cubren ejecucion local, scripts y entorno. Este archivo se enfoca en la **semantica del modelo** y en como se implementa en backend y frontend.

## 1) Modelo conceptual implementado

El sistema trabaja sobre programas formados por:

- **Hechos**: `predicado(termino). {label_1; label_2; ...}`
- **Reglas**: `cabeza(X) :- cuerpo1(X), cuerpo2(X), ... {label_1; label_2; ...}`

Cada hecho o regla porta un vector de labels. Cada posicion del vector representa un atributo independiente dentro de la algebra de etiquetas.

## 2) Sintaxis de labels

La sintaxis actual separa labels con **punto y coma**:

- Correcto: `{0.75; 0.95}`
- Correcto: `{0.5; [0.6, 1.0]}`
- Incorrecto: `{0.75, 0.95}` (la coma no separa labels)

La coma se reserva para extremos de intervalos numericos dentro de corchetes.

## 3) Labels numericos y cualitativos

### 3.1 Numericos

- Se interpretan como valores en `[0, 1]`.
- Las operaciones de soporte, agregacion y conflicto se aplican por atributo.

### 3.2 Cualitativos

- Se interpretan como conjuntos de simbolos.
- En la implementacion actual del backend:
  - soporte -> `Union`
  - agregacion -> `Union`
  - conflicto -> `Intersection`

## 4) Intervalos en labels numericos

Un atributo numerico puede declararse como intervalo:

- Ejemplo: `basicServices(houseA). {0.5; [0.6, 1.0]}`

Semantica implementada:

1. El intervalo se normaliza como `[min, max]` (si viene invertido, se corrige).
2. Para la inferencia inicial se usa el **extremo menor** (`min`).
3. Se conserva metadata del intervalo para la interaccion en UI.

Esto aplica tanto a hechos como a reglas.

## 5) Flujo de inferencia LAF en el backend

El motor sigue este orden conceptual:

1. Carga hechos iniciales.
2. Activa reglas cuando el cuerpo queda satisfecho.
3. Calcula **soporte** para hechos derivados.
4. Ejecuta **agregacion** cuando hay derivaciones equivalentes.
5. Detecta contradicciones `p(X)` vs `~p(X)` y aplica **conflicto**.
6. Devuelve el grafo argumentativo con nodos y aristas tipadas.

## 6) Representacion del grafo y visualizacion

La salida conserva la estructura argumentativa para visualizacion:

- Nodos de hechos y reglas
- Relaciones de soporte/agregacion/conflicto
- Valores `mu` y `delta` por atributo

Para nodos originados en el programa, la respuesta incluye metadata de intervalos y claves de origen estables para habilitar control interactivo en la UI.

## 7) Interaccion de intervalos en la UI

La interfaz permite ajustar labels intervalares desde el detalle del nodo:

- El detalle aparece en una **ventana flotante** sobre el grafo.
- Las barras de `mu` mantienen referencia global `0..1`.
- El control deslizante solo se mueve dentro del tramo editable del intervalo.
- Al confirmar el cambio, se reprocesa el programa y se redibuja el grafo con los nuevos valores seleccionados.

## 8) Correspondencia teoria <-> implementacion

La teoria formal completa esta en:

- `docs/theory/laf-formalism.md`

La implementacion en este repositorio materializa ese formalismo mediante:

- Algebra por atributo (soporte/agregacion/conflicto)
- Propagacion de labels en el grafo argumentativo
- Resolucion de conflicto por contradiccion explicita
- Evaluacion gradual de conclusiones segun labels

## 9) Alcance de este documento

Este README define el marco de implementacion del formalismo LAF en el monorepo.

Para ejecutar backend/frontend, scripts y entorno, ver:

- `API/README.md`
- `UI/README.md`
