import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import cytoscape, { Core } from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { finalize } from 'rxjs';
import {
  LafProgramService,
  type AttributeKind,
  type FactInput,
  type OperationRow,
  type RuleInput,
} from './services/laf-program.service';
import {
  mapBackendRequestError,
  mapGraphRenderError,
  mapInvalidBackendPayloadError,
  type HttpLikeError,
} from './utils/ui-error.util';

cytoscape.use(dagre);

interface LabelOperationInput {
  labelName: string;
  supportFunction: string;
  aggregationFunction: string;
  conflictFunction: string;
}

interface GraphRequest {
  facts: FactInput[];
  rules: RuleInput[];
  explainabilityEnabled: boolean;
  operations: {
    labels: LabelOperationInput[];
  };
}

export type GraphNodeType = 'FACT' | 'RULE';

export type GraphEdgeKind = 'SUPPORT' | 'AGGREGATION' | 'CONFLICT';

export interface GraphNode {
  id: string;
  label: string;
  type: GraphNodeType;
  attributes: string[];
  deltaAttributes: string[];
  attributeIntervals?: (readonly [number, number] | null)[];
  sourceKey?: string;
}

export interface GraphEdge {
  from: string;
  to: string;
  kind: GraphEdgeKind;
}

type VisualNodeType = GraphNodeType | 'DMP' | 'CA';

interface VisualNode {
  id: string;
  label: string;
  type: VisualNodeType;
  attributes: string[];
  deltaAttributes: string[];
  renderLabel: string;
  renderImage: string;
  renderWidth: number;
  renderHeight: number;
  conflictLeftId?: string;
  conflictRightId?: string;
}

interface VisualEdge {
  from: string;
  to: string;
  kind: GraphEdgeKind;
}

interface NodeLabelDetailCell {
  displayValue: string;
  percentage: number | null;
}

interface NodeLabelDetailRow {
  labelName: string;
  color: string;
  attributeIndex: number;
  mu: NodeLabelDetailCell;
  delta: NodeLabelDetailCell;
  intervalBounds: readonly [number, number] | null;
  sliderValue: number | null;
}

export interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

interface VisualGraph {
  nodes: VisualNode[];
  edges: VisualEdge[];
}

interface FinalConclusionTrace {
  literal: string;
  mu: string[];
  delta: string[];
  acceptability: string;
  acceptabilityReason: string;
}

interface DerivationTrace {
  targetLiteral: string;
  steps: string[];
  edgeKinds: string[];
}

interface ConflictTrace {
  leftLiteral: string;
  rightLiteral: string;
  leftDelta: string[];
  rightDelta: string[];
  winner: string;
  winnerReason: string;
}

interface NarrativeTrace {
  finalConclusions: FinalConclusionTrace[];
  derivations: DerivationTrace[];
  conflicts: ConflictTrace[];
}

interface ProcessMeta {
  model: string;
  promptVersion: string;
  generatedAt: string;
}

interface GraphProcessResponse {
  graph: GraphResponse;
  narrative: string | null;
  trace: NarrativeTrace | null;
  meta: ProcessMeta | null;
  explainability: ExplainabilityState;
}

interface ExplainabilityState {
  enabled: boolean;
  status: 'ok' | 'disabled' | 'unavailable';
  message: string | null;
}

interface FormalismTab {
  id: string;
  name: string;
  shortName: string;
  description: string;
  enabled: boolean;
}

const EXAMPLE_PROGRAM = `basicServices(houseA). {0.75; [0.80, 0.95]}
goodNeighbors(houseA). {0.75; 0.9}
gangOperate(houseA). {0.1; [0.1, 0.3]}
buy(X) :- goodArea(X). {0.85; 1.0}
goodArea(X) :- basicServices(X). {0.75; [0.8, 0.95]}
goodArea(X) :- quietArea(X). {0.75; 0.9}
quietArea(X) :- goodNeighbors(X). {0.75; 0.9}
insecureArea(X) :- gangOperate(X). {0.2; [0.1, 0.4]}
~goodArea(X) :- insecureArea(X). {0.1; [0.2, 0.4]}
~buy(X) :- ~goodArea(X). {0.1; 0.2}`;

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements AfterViewInit, OnDestroy {
  @ViewChild('graphCanvas') graphCanvas?: ElementRef<HTMLDivElement>;
  @ViewChild('graphStage') graphStage?: ElementRef<HTMLDivElement>;

  readonly backendUrl = '/api/graph/process';
  explainabilityEnabled = true;

  programText = EXAMPLE_PROGRAM;
  operationRows: OperationRow[] = [];
  activeOperationTabIndex = 0;
  activeFormalismId = 'laf';

  readonly formalismTabs: FormalismTab[] = [
    {
      id: 'laf',
      name: 'Label-Based Argumentation Framework',
      shortName: 'LAF',
      description:
        'Evaluates argument strength with multi-attribute labels. It propagates values through support and aggregation, then weakens conflicting conclusions to produce a graded final assessment.',
      enabled: true,
    },
    {
      id: 'coming-soon-1',
      name: 'Label-Based Argumentation Framework (Extended)',
      shortName: 'LBAF',
      description: 'Reserved slot for the next formalism to be implemented.',
      enabled: false,
    },
  ];

  readonly parseErrors = signal<string[]>([]);
  readonly backendError = signal('');
  readonly isLoading = signal(false);
  readonly liveStatusMessage = signal('');
  readonly liveErrorMessage = signal('');

  readonly graphResponse = signal<GraphResponse | null>(null);
  readonly processNarrative = signal<GraphProcessResponse | null>(null);
  readonly selectedNode = signal<GraphNode | null>(null);
  readonly detailPanelPosition = signal<{ left: number; top: number } | null>(null);

  private readonly detailBarPalette = [
    '#2563eb',
    '#059669',
    '#dc2626',
    '#d97706',
    '#7c3aed',
    '#0891b2',
  ];
  private readonly intervalSelections = new Map<string, number>();
  private readonly intervalPreviewSelections = new Map<string, number>();

  private cy: Core | null = null;
  private initialGraphViewport: { zoom: number; pan: { x: number; y: number } } | null = null;
  private minGraphZoom = 0.2;
  private maxGraphZoom = 3;
  private isClampingViewport = false;
  private panelDragState: {
    offsetX: number;
    offsetY: number;
    panelWidth: number;
    panelHeight: number;
  } | null = null;
  private readonly onPanelDragMove = (event: MouseEvent) => this.handlePanelDragMove(event);
  private readonly onPanelDragEnd = () => this.endDetailPanelDrag();

  constructor(
    private readonly http: HttpClient,
    private readonly lafProgramService: LafProgramService,
  ) {
    this.resetOperationsByProgram();
  }

  ngAfterViewInit(): void {
    const response = this.graphResponse();
    if (response) {
      this.renderGraph(response);
    }
  }

  ngOnDestroy(): void {
    this.cy?.destroy();
    this.cy = null;
    this.initialGraphViewport = null;
    this.endDetailPanelDrag();
  }

  zoomInGraph(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    const nextZoom = this.clamp(cy.zoom() * 1.18, this.minGraphZoom, this.maxGraphZoom);
    cy.zoom({
      level: nextZoom,
      renderedPosition: {
        x: cy.width() / 2,
        y: cy.height() / 2,
      },
    });
    this.clampGraphPanToViewport();
  }

  zoomOutGraph(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    const nextZoom = this.clamp(cy.zoom() / 1.18, this.minGraphZoom, this.maxGraphZoom);
    cy.zoom({
      level: nextZoom,
      renderedPosition: {
        x: cy.width() / 2,
        y: cy.height() / 2,
      },
    });
    this.clampGraphPanToViewport();
  }

  fitGraphToContent(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    cy.fit(cy.elements(), 30);
    this.clampGraphPanToViewport();
  }

  resetGraphViewport(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    if (!this.initialGraphViewport) {
      this.fitGraphToContent();
      return;
    }

    cy.zoom(this.initialGraphViewport.zoom);
    cy.pan(this.initialGraphViewport.pan);
    this.clampGraphPanToViewport();
  }

  loadExample(): void {
    this.programText = EXAMPLE_PROGRAM;
    this.intervalSelections.clear();
    this.intervalPreviewSelections.clear();
    this.resetOperationsByProgram();
    this.graphResponse.set(null);
    this.processNarrative.set(null);
    this.selectedNode.set(null);
    this.backendError.set('');
  }

  onProgramTextChange(nextText: string): void {
    this.programText = nextText;
    this.intervalSelections.clear();
    this.intervalPreviewSelections.clear();
    this.parseErrors.set([]);
    this.processNarrative.set(null);
    this.synchronizeOperationsFromCurrentProgram();
  }

  setActiveOperationTab(index: number): void {
    if (index < 0 || index >= this.operationRows.length) {
      return;
    }

    this.activeOperationTabIndex = index;
  }

  selectFormalism(tabId: string): void {
    const selected = this.formalismTabs.find((tab) => tab.id === tabId);
    if (!selected || !selected.enabled) {
      return;
    }

    this.activeFormalismId = selected.id;
  }

  isFormalismActive(tabId: string): boolean {
    return this.activeFormalismId === tabId;
  }

  getActiveFormalism(): FormalismTab {
    return (
      this.formalismTabs.find((tab) => tab.id === this.activeFormalismId) ?? this.formalismTabs[0]
    );
  }

  closeSelectedNodeDetails(): void {
    this.setSelectedNode(null);
  }

  onOperationTabKeydown(event: KeyboardEvent, currentIndex: number): void {
    if (this.operationRows.length === 0) {
      return;
    }

    let nextIndex: number | null = null;
    if (event.key === 'ArrowRight') {
      nextIndex = currentIndex === this.operationRows.length - 1 ? 0 : currentIndex + 1;
    } else if (event.key === 'ArrowLeft') {
      nextIndex = currentIndex === 0 ? this.operationRows.length - 1 : currentIndex - 1;
    } else if (event.key === 'Home') {
      nextIndex = 0;
    } else if (event.key === 'End') {
      nextIndex = this.operationRows.length - 1;
    }

    if (nextIndex === null) {
      return;
    }

    event.preventDefault();
    this.setActiveOperationTab(nextIndex);
    const targetTab = document.getElementById(
      `operation-tab-${nextIndex}`,
    ) as HTMLButtonElement | null;
    targetTab?.focus();
  }

  onGraphCanvasKeydown(event: KeyboardEvent): void {
    const graph = this.graphResponse();
    if (!graph || graph.nodes.length === 0) {
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.setSelectedNode(null);
      return;
    }

    const selected = this.selectedNode();
    const selectedIndex = selected ? graph.nodes.findIndex((node) => node.id === selected.id) : -1;

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      const nextIndex = selectedIndex < 0 ? 0 : (selectedIndex + 1) % graph.nodes.length;
      this.setSelectedNode(graph.nodes[nextIndex]);
      return;
    }

    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      const nextIndex =
        selectedIndex < 0
          ? graph.nodes.length - 1
          : (selectedIndex - 1 + graph.nodes.length) % graph.nodes.length;
      this.setSelectedNode(graph.nodes[nextIndex]);
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      this.setSelectedNode(graph.nodes[0]);
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      this.setSelectedNode(graph.nodes[graph.nodes.length - 1]);
      return;
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (!selected) {
        this.setSelectedNode(graph.nodes[0]);
      }
    }
  }

  startDetailPanelDrag(event: MouseEvent, panelElement: HTMLElement): void {
    if (event.button !== 0) {
      return;
    }

    const targetElement = event.target as HTMLElement | null;
    if (targetElement?.closest('.panel-close')) {
      return;
    }

    const stageElement = this.graphStage?.nativeElement;
    if (!stageElement) {
      return;
    }

    event.preventDefault();

    const stageRect = stageElement.getBoundingClientRect();
    const panelRect = panelElement.getBoundingClientRect();

    this.panelDragState = {
      offsetX: event.clientX - panelRect.left,
      offsetY: event.clientY - panelRect.top,
      panelWidth: panelRect.width,
      panelHeight: panelRect.height,
    };

    const initialLeft = panelRect.left - stageRect.left;
    const initialTop = panelRect.top - stageRect.top;
    this.detailPanelPosition.set({ left: initialLeft, top: initialTop });

    window.addEventListener('mousemove', this.onPanelDragMove);
    window.addEventListener('mouseup', this.onPanelDragEnd);
  }

  private handlePanelDragMove(event: MouseEvent): void {
    const dragState = this.panelDragState;
    const stageElement = this.graphStage?.nativeElement;
    if (!dragState || !stageElement) {
      return;
    }

    const stageRect = stageElement.getBoundingClientRect();
    const padding = 8;

    const rawLeft = event.clientX - stageRect.left - dragState.offsetX;
    const rawTop = event.clientY - stageRect.top - dragState.offsetY;

    const maxLeft = Math.max(padding, stageRect.width - dragState.panelWidth - padding);
    const maxTop = Math.max(padding, stageRect.height - dragState.panelHeight - padding);

    const left = this.clamp(rawLeft, padding, maxLeft);
    const top = this.clamp(rawTop, padding, maxTop);
    this.detailPanelPosition.set({ left, top });
  }

  private endDetailPanelDrag(): void {
    this.panelDragState = null;
    window.removeEventListener('mousemove', this.onPanelDragMove);
    window.removeEventListener('mouseup', this.onPanelDragEnd);
  }

  processProgram(options?: { preserveSelection?: boolean }): void {
    this.parseErrors.set([]);
    this.backendError.set('');
    const previousSelected = options?.preserveSelection ? this.selectedNode() : null;
    this.setSelectedNode(null, { announce: false });
    this.announceStatus('Processing graph.');

    const parsedResult = this.lafProgramService.parseProgram(this.programText);
    this.parseErrors.set(parsedResult.errors);

    if (!parsedResult.parsed) {
      this.graphResponse.set(null);
      this.processNarrative.set(null);
      this.announceError(`Validation errors found: ${parsedResult.errors.length}.`);
      return;
    }

    const parsed = parsedResult.parsed;

    const parsedWithSelections = this.lafProgramService.applyIntervalSelections(
      parsed,
      this.intervalSelections,
    );

    this.synchronizeOperationRows(parsed.attributeCount, parsed.attributeKinds);

    const operationErrors = this.lafProgramService.validateOperations(
      this.operationRows,
      parsed.attributeCount,
    );
    this.parseErrors.set(operationErrors);
    if (operationErrors.length > 0) {
      this.graphResponse.set(null);
      this.processNarrative.set(null);
      this.announceError(`Validation errors found: ${operationErrors.length}.`);
      return;
    }

    const requestPayload: GraphRequest = {
      facts: parsedWithSelections.facts,
      rules: parsedWithSelections.rules,
      explainabilityEnabled: this.explainabilityEnabled,
      operations: {
        labels: this.operationRows.map((row) => ({
          labelName: row.labelName.trim(),
          supportFunction: row.supportFunction.trim(),
          aggregationFunction: row.aggregationFunction.trim(),
          conflictFunction: row.conflictFunction.trim(),
        })),
      },
    };

    this.isLoading.set(true);

    this.http
      .post<GraphProcessResponse>(this.backendUrl, requestPayload)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          if (
            !response ||
            !response.graph ||
            !Array.isArray(response.graph.nodes) ||
            !Array.isArray(response.graph.edges) ||
            !response.explainability ||
            typeof response.explainability.enabled !== 'boolean' ||
            !['ok', 'disabled', 'unavailable'].includes(response.explainability.status)
          ) {
            this.graphResponse.set(null);
            this.processNarrative.set(null);
            this.backendError.set(mapInvalidBackendPayloadError());
            this.announceError('Backend response is invalid.');
            return;
          }

          this.graphResponse.set(response.graph);
          this.processNarrative.set(response);

          if (previousSelected) {
            const restoredSelection = this.findMatchingNode(response.graph.nodes, previousSelected);
            if (restoredSelection) {
              this.setSelectedNode(restoredSelection, { announce: false });
            }
          }

          this.announceStatus(
            `Graph ready with ${response.graph.nodes.length} nodes and ${response.graph.edges.length} edges.`,
          );

          setTimeout(() => {
            try {
              this.renderGraph(response.graph);
            } catch (renderError) {
              this.backendError.set(mapGraphRenderError(renderError));
            }
          }, 0);
        },
        error: (error: HttpLikeError) => {
          this.graphResponse.set(null);
          this.processNarrative.set(null);
          this.cy?.destroy();
          this.cy = null;
          this.backendError.set(mapBackendRequestError(error));
          this.announceError('Backend request failed.');
        },
      });
  }

  private announceStatus(message: string): void {
    this.liveStatusMessage.set(message);
    this.liveErrorMessage.set('');
  }

  private announceError(message: string): void {
    this.liveErrorMessage.set(message);
  }

  private setSelectedNode(node: GraphNode | null, options?: { announce?: boolean }): void {
    const shouldAnnounce = options?.announce ?? true;
    this.cy?.nodes().removeClass('node-selected');

    if (!node) {
      this.selectedNode.set(null);
      this.detailPanelPosition.set(null);
      if (shouldAnnounce) {
        this.announceStatus('Node details closed.');
      }
      return;
    }

    this.cy?.getElementById(node.id).addClass('node-selected');
    this.selectedNode.set(node);
    if (shouldAnnounce) {
      this.announceStatus(`Selected node: ${node.label}.`);
    }
  }

  private resetOperationsByProgram(): void {
    this.parseErrors.set([]);
    this.synchronizeOperationsFromCurrentProgram();
  }

  private synchronizeOperationsFromCurrentProgram(): void {
    const inferred = this.lafProgramService.inferAttributeConfig(this.programText);
    if (!inferred) {
      return;
    }

    this.synchronizeOperationRows(inferred.attributeCount, inferred.attributeKinds);
  }

  private synchronizeOperationRows(attributeCount: number, kinds: AttributeKind[]): void {
    const previousRows = this.operationRows;
    this.operationRows = [];

    for (let index = 0; index < attributeCount; index += 1) {
      const existing = previousRows[index];
      if (existing) {
        this.operationRows.push({ ...existing });
        continue;
      }

      const defaults = this.defaultFunctionsForKind(kinds[index] ?? 'numeric');
      this.operationRows.push({
        labelName: `label_${index + 1}`,
        supportFunction: defaults.supportFunction,
        aggregationFunction: defaults.aggregationFunction,
        conflictFunction: defaults.conflictFunction,
      });
    }

    this.ensureValidActiveTabIndex();
  }

  private ensureValidActiveTabIndex(): void {
    if (this.operationRows.length === 0) {
      this.activeOperationTabIndex = 0;
      return;
    }

    if (this.activeOperationTabIndex < 0) {
      this.activeOperationTabIndex = 0;
      return;
    }

    if (this.activeOperationTabIndex >= this.operationRows.length) {
      this.activeOperationTabIndex = this.operationRows.length - 1;
    }
  }

  private defaultFunctionsForKind(_kind: AttributeKind): Omit<OperationRow, 'labelName'> {
    return {
      supportFunction: 'X + Y',
      aggregationFunction: 'X * Y',
      conflictFunction: 'X - Y',
    };
  }

  private normalizeNumeric(value: number): string {
    const compact = value.toString();
    return compact === '-0' ? '0' : compact;
  }

  private buildIntervalSelectionKey(sourceKey: string, attributeIndex: number): string {
    return this.lafProgramService.buildIntervalSelectionKey(sourceKey, attributeIndex);
  }

  private clamp(value: number, min: number, max: number): number {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  onNodeIntervalInput(row: NodeLabelDetailRow, rawValue: string): void {
    const node = this.selectedNode();
    if (!node || !row.intervalBounds || !node.sourceKey) {
      return;
    }

    const parsedValue = Number(rawValue);
    if (!Number.isFinite(parsedValue)) {
      return;
    }

    const clamped = this.clamp(parsedValue, row.intervalBounds[0], row.intervalBounds[1]);
    const selectionKey = this.buildIntervalSelectionKey(node.sourceKey, row.attributeIndex);
    this.intervalPreviewSelections.set(selectionKey, clamped);
  }

  onNodeIntervalCommit(row: NodeLabelDetailRow, rawValue: string): void {
    const node = this.selectedNode();
    if (!node || !row.intervalBounds || !node.sourceKey) {
      return;
    }

    const parsedValue = Number(rawValue);
    if (!Number.isFinite(parsedValue)) {
      return;
    }

    const clamped = this.clamp(parsedValue, row.intervalBounds[0], row.intervalBounds[1]);
    const selectionKey = this.buildIntervalSelectionKey(node.sourceKey, row.attributeIndex);
    this.intervalPreviewSelections.delete(selectionKey);
    this.intervalSelections.set(selectionKey, clamped);
    this.processProgram({ preserveSelection: true });
  }

  private findMatchingNode(nodes: GraphNode[], selectedNode: GraphNode): GraphNode | null {
    if (selectedNode.sourceKey) {
      const bySourceKey = nodes.find(
        (node) => node.type === selectedNode.type && node.sourceKey === selectedNode.sourceKey,
      );
      if (bySourceKey) {
        return bySourceKey;
      }
    }

    const byLabel = nodes.find(
      (node) => node.type === selectedNode.type && node.label === selectedNode.label,
    );
    return byLabel ?? null;
  }

  private renderGraph(graph: GraphResponse): void {
    const container = this.graphCanvas?.nativeElement;
    if (!container) {
      return;
    }

    this.cy?.destroy();
    this.initialGraphViewport = null;

    const visualGraph = this.buildVisualGraph(graph);

    const elements = [
      ...visualGraph.nodes.map((node) => ({
        data: {
          id: node.id,
          label: node.label,
          type: node.type,
          attributes: node.attributes,
          deltaAttributes: node.deltaAttributes,
          renderLabel: node.renderLabel,
          renderImage: node.renderImage,
          renderWidth: node.renderWidth,
          renderHeight: node.renderHeight,
          conflictLeftId: node.conflictLeftId,
          conflictRightId: node.conflictRightId,
        },
      })),
      ...visualGraph.edges.map((edge, index) => ({
        data: {
          id: `${edge.from}_${edge.to}_${edge.kind}_${index}`,
          source: edge.from,
          target: edge.to,
          kind: edge.kind,
        },
      })),
    ];

    this.cy = cytoscape({
      container,
      elements,
      minZoom: 0.05,
      maxZoom: 4,
      style: [
        {
          selector: 'node[type = "FACT"], node[type = "RULE"]',
          style: {
            label: '',
            shape: 'rectangle',
            'background-opacity': 0,
            'background-image': 'data(renderImage)',
            'background-fit': 'contain',
            'background-clip': 'none',
            width: 'data(renderWidth)',
            height: 'data(renderHeight)',
            'border-width': 0,
          },
        },
        {
          selector: 'node[type = "RULE"]',
          style: {
            shape: 'rectangle',
          },
        },
        {
          selector: 'node[type = "FACT"]',
          style: {
            shape: 'rectangle',
          },
        },
        {
          selector: 'node[type = "DMP"]',
          style: {
            label: 'data(label)',
            shape: 'ellipse',
            width: 82,
            height: 40,
            'background-color': '#f5f5f5',
            'border-width': 1,
            'border-color': '#222222',
            'font-size': '15px',
            'font-style': 'italic',
            'font-family': 'Times New Roman, serif',
            color: '#1f2937',
            'text-halign': 'center',
            'text-valign': 'center',
          },
        },
        {
          selector: 'node[type = "CA"]',
          style: {
            label: 'data(label)',
            shape: 'diamond',
            width: 64,
            height: 44,
            'background-color': '#f5f5f5',
            'border-width': 1,
            'border-color': '#222222',
            'font-size': '14px',
            'font-family': 'Times New Roman, serif',
            color: '#1f2937',
            'text-halign': 'center',
            'text-valign': 'center',
          },
        },
        {
          selector: 'node.node-selected',
          style: {
            'underlay-color': '#1e40af',
            'underlay-opacity': 0.2,
            'underlay-padding': 8,
            'overlay-color': '#1e40af',
            'overlay-opacity': 0.12,
            'overlay-padding': 3,
          },
        },
        {
          selector: 'edge',
          style: {
            width: 1.4,
            'line-color': '#111111',
            'target-arrow-color': '#111111',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
          },
        },
        {
          selector: 'edge[kind = "CONFLICT"]',
          style: {
            'line-color': '#111111',
            'target-arrow-color': '#111111',
            'source-arrow-color': '#111111',
            'source-arrow-shape': 'triangle',
            'curve-style': 'straight',
          },
        },
      ],
    });

    this.cy.one('layoutstop', () => {
      this.positionConflictMediatorsAtMidpoint();
      this.initializeGraphViewport();
    });
    this.cy.layout(this.createLayoutOptions()).run();

    this.cy.on('tap', 'node', (event) => {
      const tappedId = event.target.data('id') as string;
      const node = graph.nodes.find((item) => item.id === tappedId) ?? null;
      this.setSelectedNode(node, { announce: false });
    });

    this.cy.on('tap', (event) => {
      if (event.target !== this.cy) {
        return;
      }

      this.setSelectedNode(null, { announce: false });
    });
  }

  private positionConflictMediatorsAtMidpoint(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    const caNodes = cy.nodes('node[type = "CA"]');
    caNodes.forEach((caNode) => {
      const leftId = caNode.data('conflictLeftId') as string | undefined;
      const rightId = caNode.data('conflictRightId') as string | undefined;
      if (!leftId || !rightId) {
        return;
      }

      const leftNode = cy.getElementById(leftId);
      const rightNode = cy.getElementById(rightId);
      if (leftNode.empty() || rightNode.empty()) {
        return;
      }

      const leftPosition = leftNode.position();
      const rightPosition = rightNode.position();
      caNode.position({
        x: (leftPosition.x + rightPosition.x) / 2,
        y: (leftPosition.y + rightPosition.y) / 2,
      });
    });
  }

  private initializeGraphViewport(): void {
    const cy = this.cy;
    if (!cy) {
      return;
    }

    cy.fit(cy.elements(), 30);

    const fittedZoom = cy.zoom();
    this.minGraphZoom = Math.max(0.05, fittedZoom * 0.9);
    this.maxGraphZoom = Math.max(this.minGraphZoom + 0.1, fittedZoom * 4);
    cy.minZoom(this.minGraphZoom);
    cy.maxZoom(this.maxGraphZoom);

    this.initialGraphViewport = {
      zoom: fittedZoom,
      pan: { ...cy.pan() },
    };

    cy.on('pan zoom resize', () => {
      this.clampGraphPanToViewport();
    });

    this.clampGraphPanToViewport();
  }

  private clampGraphPanToViewport(): void {
    const cy = this.cy;
    if (!cy || this.isClampingViewport) {
      return;
    }

    const elements = cy.elements();
    if (elements.length === 0) {
      return;
    }

    const bounds = elements.boundingBox();
    const zoom = cy.zoom();
    const pan = cy.pan();
    const viewportWidth = cy.width();
    const viewportHeight = cy.height();
    const margin = 36;

    const minPanX = viewportWidth - bounds.x2 * zoom - margin;
    const maxPanX = margin - bounds.x1 * zoom;
    const minPanY = viewportHeight - bounds.y2 * zoom - margin;
    const maxPanY = margin - bounds.y1 * zoom;

    const nextPanX = this.clampPanAxis(pan.x, minPanX, maxPanX);
    const nextPanY = this.clampPanAxis(pan.y, minPanY, maxPanY);

    if (nextPanX === pan.x && nextPanY === pan.y) {
      return;
    }

    this.isClampingViewport = true;
    cy.pan({ x: nextPanX, y: nextPanY });
    this.isClampingViewport = false;
  }

  private clampPanAxis(current: number, min: number, max: number): number {
    if (min > max) {
      return (min + max) / 2;
    }

    return this.clamp(current, min, max);
  }

  private buildVisualGraph(graph: GraphResponse): VisualGraph {
    const allEdges: VisualEdge[] = graph.edges.map((edge) => ({ ...edge }));
    const supportEdges = allEdges.filter((edge) => edge.kind === 'SUPPORT');
    const aggregationEdges = allEdges.filter((edge) => edge.kind === 'AGGREGATION');
    const conflictEdges = allEdges.filter((edge) => edge.kind === 'CONFLICT');

    const supportByTarget = new Map<string, VisualEdge[]>();
    supportEdges.forEach((edge) => {
      const current = supportByTarget.get(edge.to) ?? [];
      current.push(edge);
      supportByTarget.set(edge.to, current);
    });

    const aggregationByTarget = new Map<string, VisualEdge[]>();
    aggregationEdges.forEach((edge) => {
      const current = aggregationByTarget.get(edge.to) ?? [];
      current.push(edge);
      aggregationByTarget.set(edge.to, current);
    });

    const hiddenAggregationNodes = new Set(aggregationEdges.map((edge) => edge.from));
    const nodes: VisualNode[] = graph.nodes
      .filter((node) => !hiddenAggregationNodes.has(node.id))
      .map((node) => ({
        id: node.id,
        label: node.label,
        type: node.type,
        attributes: node.attributes,
        deltaAttributes: node.deltaAttributes,
        renderLabel: this.buildNodeLabel(node),
        renderImage: this.buildNodeImage(node),
        renderWidth: this.estimateNodeWidth(node),
        renderHeight: this.estimateNodeHeight(node),
      }));

    const finalEdges: VisualEdge[] = [];
    const supportTargetsHandledByAggregation = new Set<string>();
    let dmpIndex = 1;
    let caIndex = 1;

    const createDmpNode = (): string => {
      const dmpId = `DMP_${dmpIndex++}`;
      nodes.push({
        id: dmpId,
        label: 'dMP',
        type: 'DMP',
        attributes: [],
        deltaAttributes: [],
        renderLabel: 'dMP',
        renderImage: '',
        renderWidth: 82,
        renderHeight: 40,
      });
      return dmpId;
    };

    aggregationByTarget.forEach((aggregationSources, aggregatedTargetId) => {
      aggregationSources.forEach((aggregationEdge) => {
        const intermediateNodeId = aggregationEdge.from;
        supportTargetsHandledByAggregation.add(intermediateNodeId);

        const branchSupportEdges = (supportByTarget.get(intermediateNodeId) ?? []).filter(
          (edge) => !hiddenAggregationNodes.has(edge.from),
        );

        if (branchSupportEdges.length === 0) {
          return;
        }

        const dmpId = createDmpNode();
        branchSupportEdges.forEach((edge) => {
          finalEdges.push({
            from: edge.from,
            to: dmpId,
            kind: 'SUPPORT',
          });
        });

        finalEdges.push({
          from: dmpId,
          to: aggregatedTargetId,
          kind: 'SUPPORT',
        });
      });
    });

    supportByTarget.forEach((targetSupportEdges, targetId) => {
      if (
        hiddenAggregationNodes.has(targetId) &&
        supportTargetsHandledByAggregation.has(targetId)
      ) {
        return;
      }

      const visibleSupportEdges = targetSupportEdges.filter(
        (edge) => !hiddenAggregationNodes.has(edge.from) && !hiddenAggregationNodes.has(edge.to),
      );

      if (visibleSupportEdges.length === 0) {
        return;
      }

      if (visibleSupportEdges.length >= 2) {
        const dmpId = createDmpNode();
        visibleSupportEdges.forEach((edge) => {
          finalEdges.push({
            from: edge.from,
            to: dmpId,
            kind: 'SUPPORT',
          });
        });
        finalEdges.push({
          from: dmpId,
          to: targetId,
          kind: 'SUPPORT',
        });
        return;
      }

      visibleSupportEdges.forEach((edge) => finalEdges.push(edge));
    });

    const visibleConflictEdges = conflictEdges.filter(
      (edge) => !hiddenAggregationNodes.has(edge.from) && !hiddenAggregationNodes.has(edge.to),
    );
    const collapsedVisual = this.collapseDuplicateFactVisualNodes(
      nodes,
      finalEdges,
      visibleConflictEdges,
    );
    const collapsedNodes = collapsedVisual.nodes;
    const collapsedSupportEdges = [...collapsedVisual.supportEdges];
    const collapsedConflictEdges = collapsedVisual.conflictEdges;

    const conflictPairs = new Map<string, { firstId: string; secondId: string }>();
    collapsedConflictEdges.forEach((edge) => {
      if (edge.from === edge.to) {
        return;
      }

      const firstId = edge.from < edge.to ? edge.from : edge.to;
      const secondId = edge.from < edge.to ? edge.to : edge.from;
      const pairKey = `${firstId}::${secondId}`;
      if (!conflictPairs.has(pairKey)) {
        conflictPairs.set(pairKey, { firstId, secondId });
      }
    });

    conflictPairs.forEach(({ firstId, secondId }) => {
      const caId = `CA_${caIndex++}_${firstId}_${secondId}`;
      collapsedNodes.push({
        id: caId,
        label: 'CA',
        type: 'CA',
        attributes: [],
        deltaAttributes: [],
        renderLabel: 'CA',
        renderImage: '',
        renderWidth: 64,
        renderHeight: 44,
        conflictLeftId: firstId,
        conflictRightId: secondId,
      });

      collapsedSupportEdges.push(
        { from: firstId, to: caId, kind: 'CONFLICT' },
        { from: secondId, to: caId, kind: 'CONFLICT' },
      );
    });

    return {
      nodes: collapsedNodes,
      edges: collapsedSupportEdges,
    };
  }

  private collapseDuplicateFactVisualNodes(
    nodes: VisualNode[],
    supportEdges: VisualEdge[],
    conflictEdges: VisualEdge[],
  ): { nodes: VisualNode[]; supportEdges: VisualEdge[]; conflictEdges: VisualEdge[] } {
    const factNodes = nodes.filter((node) => node.type === 'FACT');
    const groupsByLabel = new Map<string, VisualNode[]>();

    factNodes.forEach((node) => {
      const current = groupsByLabel.get(node.label) ?? [];
      current.push(node);
      groupsByLabel.set(node.label, current);
    });

    const supportIncoming = new Map<string, number>();
    const totalDegree = new Map<string, number>();

    const trackDegree = (edge: VisualEdge): void => {
      totalDegree.set(edge.from, (totalDegree.get(edge.from) ?? 0) + 1);
      totalDegree.set(edge.to, (totalDegree.get(edge.to) ?? 0) + 1);
    };

    supportEdges.forEach((edge) => {
      supportIncoming.set(edge.to, (supportIncoming.get(edge.to) ?? 0) + 1);
      trackDegree(edge);
    });
    conflictEdges.forEach(trackDegree);

    const replacementByNodeId = new Map<string, string>();

    groupsByLabel.forEach((groupNodes) => {
      if (groupNodes.length <= 1) {
        return;
      }

      const sortedCandidates = [...groupNodes].sort((left, right) => {
        const leftSupportIn = supportIncoming.get(left.id) ?? 0;
        const rightSupportIn = supportIncoming.get(right.id) ?? 0;
        if (leftSupportIn !== rightSupportIn) {
          return rightSupportIn - leftSupportIn;
        }

        const leftDegree = totalDegree.get(left.id) ?? 0;
        const rightDegree = totalDegree.get(right.id) ?? 0;
        if (leftDegree !== rightDegree) {
          return rightDegree - leftDegree;
        }

        return left.id.localeCompare(right.id);
      });

      const canonicalId = sortedCandidates[0].id;
      sortedCandidates.slice(1).forEach((node) => {
        replacementByNodeId.set(node.id, canonicalId);
      });
    });

    if (replacementByNodeId.size === 0) {
      return { nodes, supportEdges, conflictEdges };
    }

    const collapsedNodes = nodes.filter((node) => !replacementByNodeId.has(node.id));
    const validNodeIds = new Set(collapsedNodes.map((node) => node.id));

    const remapAndDeduplicateEdges = (edges: VisualEdge[]): VisualEdge[] => {
      const unique = new Set<string>();
      const result: VisualEdge[] = [];

      edges.forEach((edge) => {
        const from = replacementByNodeId.get(edge.from) ?? edge.from;
        const to = replacementByNodeId.get(edge.to) ?? edge.to;

        if (from === to || !validNodeIds.has(from) || !validNodeIds.has(to)) {
          return;
        }

        const key = `${from}|${to}|${edge.kind}`;
        if (unique.has(key)) {
          return;
        }

        unique.add(key);
        result.push({ from, to, kind: edge.kind });
      });

      return result;
    };

    return {
      nodes: collapsedNodes,
      supportEdges: remapAndDeduplicateEdges(supportEdges),
      conflictEdges: remapAndDeduplicateEdges(conflictEdges),
    };
  }

  private createLayoutOptions() {
    return {
      name: 'dagre',
      rankDir: 'TB',
      ranker: 'network-simplex',
      acyclicer: 'greedy',
      nodeSep: 80,
      rankSep: 110,
      edgeSep: 45,
      animate: false,
      fit: true,
      padding: 26,
    };
  }

  private buildNodeLabel(node: GraphNode): string {
    const mu = this.formatAttributeLine(node.attributes);
    const delta = this.formatAttributeLine(node.deltaAttributes);
    return `${node.label}\n----------------\nmu: ${mu}\ndelta: ${delta}`;
  }

  private buildNodeImage(node: GraphNode): string {
    const labelLines = this.buildLabelLines(node.label);
    const columnCount = Math.max(
      node.attributes?.length ?? 0,
      node.deltaAttributes?.length ?? 0,
      1,
    );
    const width = this.estimateNodeWidth(node);
    const topHeight = 22 + labelLines.length * 20;
    const attrHeight = 52;
    const totalHeight = topHeight + attrHeight;
    const colWidth = width / columnCount;

    const verticalDividers: string[] = [];
    for (let index = 1; index < columnCount; index += 1) {
      const x = Math.round(colWidth * index);
      verticalDividers.push(
        `<line x1="${x}" y1="${topHeight}" x2="${x}" y2="${totalHeight}" stroke="#111" stroke-width="1" stroke-dasharray="2,2" />`,
      );
    }

    const labelLineElements = labelLines
      .map((line, index) => {
        const y = 22 + index * 20;
        return `<text x="${width / 2}" y="${y}" text-anchor="middle" font-family="Courier New, monospace" font-size="14">${this.escapeXml(line)}</text>`;
      })
      .join('');

    const muValues = this.normalizeAttributeValues(node.attributes, columnCount, 2);
    const deltaValues = this.normalizeAttributeValues(node.deltaAttributes, columnCount, 2);
    const muLineElements = muValues
      .map((value, index) => {
        const x = colWidth * index + colWidth / 2;
        const y = topHeight + 18;
        return `<text x="${x}" y="${y}" text-anchor="middle" font-family="Courier New, monospace" font-size="13" font-weight="600">${this.escapeXml(value)}</text>`;
      })
      .join('');

    const deltaLineElements = deltaValues
      .map((value, index) => {
        const x = colWidth * index + colWidth / 2;
        const y = topHeight + 41;
        return `<text x="${x}" y="${y}" text-anchor="middle" font-family="Courier New, monospace" font-size="13" font-weight="600">${this.escapeXml(value)}</text>`;
      })
      .join('');

    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${totalHeight}" viewBox="0 0 ${width} ${totalHeight}">
      <rect x="0.5" y="0.5" width="${width - 1}" height="${topHeight - 1}" fill="#fff" stroke="#111" stroke-width="1" />
      <rect x="0.5" y="${topHeight + 0.5}" width="${width - 1}" height="${attrHeight - 1}" fill="#fff" stroke="#111" stroke-width="1" stroke-dasharray="2,2" />
      <line x1="0" y1="${topHeight + attrHeight / 2}" x2="${width}" y2="${topHeight + attrHeight / 2}" stroke="#111" stroke-width="1" stroke-dasharray="2,2" />
      ${verticalDividers.join('')}
      ${labelLineElements}
      ${muLineElements}
      ${deltaLineElements}
    </svg>`;

    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  }

  private estimateNodeWidth(node: GraphNode): number {
    const labelLines = this.buildLabelLines(node.label);
    const longestLabel = labelLines.reduce((max, line) => Math.max(max, line.length), 0);
    const labelWidth = Math.max(120, longestLabel * 8 + 28);
    const columnCount = Math.max(
      node.attributes?.length ?? 0,
      node.deltaAttributes?.length ?? 0,
      1,
    );
    const attrsWidth = Math.max(120, columnCount * 52);
    return Math.max(labelWidth, attrsWidth);
  }

  private estimateNodeHeight(node: GraphNode): number {
    const labelLines = this.buildLabelLines(node.label);
    return 22 + labelLines.length * 20 + 52;
  }

  private buildLabelLines(rawLabel: string): string[] {
    const normalized = rawLabel.replace(/\s+/g, ' ').trim();
    if (!normalized) {
      return ['-'];
    }

    const preferredBreak =
      normalized.includes('(') && !normalized.includes(':-')
        ? normalized.replace('(', '\n(')
        : normalized;
    const roughLines = preferredBreak.split('\n');
    const wrapped: string[] = [];

    roughLines.forEach((line) => {
      wrapped.push(...this.wrapLine(line, 28));
    });

    return wrapped.slice(0, 3);
  }

  private wrapLine(line: string, maxChars: number): string[] {
    const trimmed = line.trim();
    if (trimmed.length <= maxChars) {
      return [trimmed];
    }

    const words = trimmed.split(' ');
    const output: string[] = [];
    let current = '';

    words.forEach((word) => {
      const candidate = current ? `${current} ${word}` : word;
      if (candidate.length <= maxChars) {
        current = candidate;
        return;
      }

      if (current) {
        output.push(current);
      }
      current = word;
    });

    if (current) {
      output.push(current);
    }

    return output;
  }

  private normalizeAttributeValues(
    values: string[] | undefined,
    targetLength: number,
    maxDecimals: number,
  ): string[] {
    const normalized = values
      ? values.map((value) => this.formatValueWithPrecision(value, maxDecimals))
      : [];
    while (normalized.length < targetLength) {
      normalized.push('-');
    }
    return normalized.slice(0, targetLength);
  }

  getSelectedNodeDetailRows(): NodeLabelDetailRow[] {
    const node = this.selectedNode();
    if (!node) {
      return [];
    }

    const size = Math.max(node.attributes?.length ?? 0, node.deltaAttributes?.length ?? 0);
    if (size === 0) {
      return [];
    }

    const rows: NodeLabelDetailRow[] = [];
    for (let index = 0; index < size; index += 1) {
      const labelName = this.operationRows[index]?.labelName?.trim() || `label_${index + 1}`;
      const intervalBounds = this.readIntervalBounds(node, index);
      const sliderValue = this.readSliderValue(node, index, intervalBounds);
      const muValue =
        sliderValue !== null ? this.normalizeNumeric(sliderValue) : node.attributes?.[index];
      rows.push({
        labelName,
        color: this.detailBarPalette[index % this.detailBarPalette.length],
        attributeIndex: index,
        mu: this.buildDetailCell(muValue),
        delta: this.buildDetailCell(node.deltaAttributes?.[index]),
        intervalBounds,
        sliderValue,
      });
    }

    return rows;
  }

  private readIntervalBounds(node: GraphNode, index: number): readonly [number, number] | null {
    const candidate = node.attributeIntervals?.[index];
    if (!candidate || candidate.length < 2) {
      return null;
    }

    const min = Number(candidate[0]);
    const max = Number(candidate[1]);
    if (!Number.isFinite(min) || !Number.isFinite(max)) {
      return null;
    }

    return min <= max ? [min, max] : [max, min];
  }

  private readSliderValue(
    node: GraphNode,
    index: number,
    intervalBounds: readonly [number, number] | null,
  ): number | null {
    if (!intervalBounds) {
      return null;
    }

    const selectionKey = node.sourceKey
      ? this.buildIntervalSelectionKey(node.sourceKey, index)
      : null;

    if (selectionKey) {
      const preview = this.intervalPreviewSelections.get(selectionKey);
      if (preview !== undefined) {
        return this.clamp(preview, intervalBounds[0], intervalBounds[1]);
      }

      const selected = this.intervalSelections.get(selectionKey);
      if (selected !== undefined) {
        return this.clamp(selected, intervalBounds[0], intervalBounds[1]);
      }
    }

    const current = Number(node.attributes?.[index]);
    if (!Number.isFinite(current)) {
      return intervalBounds[0];
    }

    return this.clamp(current, intervalBounds[0], intervalBounds[1]);
  }

  getEditableZoneStartPercent(bounds: readonly [number, number] | null): number {
    if (!bounds) {
      return 0;
    }

    return this.clamp(bounds[0], 0, 1) * 100;
  }

  getEditableZoneWidthPercent(bounds: readonly [number, number] | null): number {
    if (!bounds) {
      return 100;
    }

    const min = this.clamp(bounds[0], 0, 1);
    const max = this.clamp(bounds[1], 0, 1);
    if (max <= min) {
      return 0;
    }

    return (max - min) * 100;
  }

  getEditableZoneEndPercent(bounds: readonly [number, number] | null): number {
    if (!bounds) {
      return 100;
    }

    return this.clamp(bounds[1], 0, 1) * 100;
  }

  getEditableFillWidthPercent(
    bounds: readonly [number, number] | null,
    sliderValue: number | null,
  ): number {
    if (!bounds || sliderValue === null) {
      return 0;
    }

    const start = this.getEditableZoneStartPercent(bounds);
    const sliderPercent = this.clamp(sliderValue, 0, 1) * 100;
    if (sliderPercent <= start) {
      return 0;
    }

    return sliderPercent - start;
  }

  getIntervalSliderStep(bounds: readonly [number, number] | null): string {
    if (!bounds) {
      return '0.01';
    }

    const range = Math.abs(bounds[1] - bounds[0]);
    if (range <= 0.1) {
      return '0.001';
    }

    if (range <= 1) {
      return '0.01';
    }

    return '0.1';
  }

  private formatValueWithPrecision(rawValue: string, maxDecimals: number): string {
    const value = rawValue.trim();
    if (!value) {
      return '-';
    }

    const asNumber = Number(value);
    if (!Number.isFinite(asNumber)) {
      return value;
    }

    const compact = asNumber.toFixed(maxDecimals).replace(/\.?0+$/, '');
    return compact === '-0' ? '0' : compact;
  }

  private buildDetailCell(rawValue: string | undefined): NodeLabelDetailCell {
    const value = rawValue?.trim() ?? '';
    if (!value) {
      return {
        displayValue: '-',
        percentage: null,
      };
    }

    const asNumber = Number(value);
    if (!Number.isFinite(asNumber)) {
      return {
        displayValue: value,
        percentage: null,
      };
    }

    const displayValue = this.formatValueWithPrecision(value, 4);
    if (asNumber < 0 || asNumber > 1) {
      return {
        displayValue,
        percentage: null,
      };
    }

    return {
      displayValue,
      percentage: asNumber * 100,
    };
  }

  private escapeXml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&apos;');
  }

  private formatAttributeLine(values: string[]): string {
    if (!values || values.length === 0) {
      return '-';
    }

    return values.map((value) => this.formatValueWithPrecision(value, 2)).join('    ');
  }
}
