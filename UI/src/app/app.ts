import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import cytoscape, { Core } from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { finalize } from 'rxjs';

cytoscape.use(dagre);

interface FactInput {
  name: string;
  argument: string;
  attributes: string[];
}

interface RuleInput {
  headName: string;
  bodyLiterals: string[];
  attributes: string[];
}

interface LabelOperationInput {
  labelName: string;
  supportFunction: string;
  aggregationFunction: string;
  conflictFunction: string;
}

interface GraphRequest {
  facts: FactInput[];
  rules: RuleInput[];
  operations: {
    labels: LabelOperationInput[];
  };
}

interface GraphNode {
  id: string;
  label: string;
  type: string;
  attributes: string[];
  deltaAttributes: string[];
}

interface GraphEdge {
  from: string;
  to: string;
  kind: string;
}

interface VisualNode {
  id: string;
  label: string;
  type: string;
  attributes: string[];
  deltaAttributes: string[];
  renderLabel: string;
  renderImage: string;
  renderWidth: number;
  renderHeight: number;
}

interface VisualEdge {
  from: string;
  to: string;
  kind: string;
}

interface NodeLabelDetailCell {
  displayValue: string;
  percentage: number | null;
}

interface NodeLabelDetailRow {
  labelName: string;
  color: string;
  mu: NodeLabelDetailCell;
  delta: NodeLabelDetailCell;
}

interface GraphResponse {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

interface ParsedProgram {
  facts: FactInput[];
  rules: RuleInput[];
  attributeCount: number;
  attributeKinds: AttributeKind[];
}

type AttributeKind = 'numeric' | 'qualitative';

interface OperationRow {
  labelName: string;
  supportFunction: string;
  aggregationFunction: string;
  conflictFunction: string;
}

const EXAMPLE_PROGRAM = `reliableSource(reportA). {0.9, 0.8}
supportedEvidence(reportA). {0.85, 0.7}
~supportedEvidence(reportA). {0.4, 0.3}
acceptClaim(X) :- reliableSource(X), supportedEvidence(X). {0.88, 0.75}`;

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements AfterViewInit, OnDestroy {
  @ViewChild('graphCanvas') graphCanvas?: ElementRef<HTMLDivElement>;

  readonly backendUrl = '/api/graph';

  programText = EXAMPLE_PROGRAM;
  operationRows: OperationRow[] = [];
  activeOperationTabIndex = 0;

  readonly parseErrors = signal<string[]>([]);
  readonly backendError = signal('');
  readonly isLoading = signal(false);

  readonly graphResponse = signal<GraphResponse | null>(null);
  readonly selectedNode = signal<GraphNode | null>(null);

  private readonly detailBarPalette = ['#2563eb', '#059669', '#dc2626', '#d97706', '#7c3aed', '#0891b2'];

  private cy: Core | null = null;

  constructor(private readonly http: HttpClient) {
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
  }

  loadExample(): void {
    this.programText = EXAMPLE_PROGRAM;
    this.resetOperationsByProgram();
    this.graphResponse.set(null);
    this.selectedNode.set(null);
    this.backendError.set('');
  }

  onProgramTextChange(nextText: string): void {
    this.programText = nextText;
    this.parseErrors.set([]);
    this.synchronizeOperationsFromCurrentProgram();
  }

  setActiveOperationTab(index: number): void {
    if (index < 0 || index >= this.operationRows.length) {
      return;
    }

    this.activeOperationTabIndex = index;
  }

  processProgram(): void {
    this.parseErrors.set([]);
    this.backendError.set('');
    this.selectedNode.set(null);

    const parsed = this.parseProgram(this.programText);
    if (!parsed) {
      this.graphResponse.set(null);
      return;
    }

    this.synchronizeOperationRows(parsed.attributeCount, parsed.attributeKinds);

    if (!this.validateOperations(parsed.attributeCount)) {
      this.graphResponse.set(null);
      return;
    }

    const requestPayload: GraphRequest = {
      facts: parsed.facts,
      rules: parsed.rules,
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
      .post<GraphResponse>(this.backendUrl, requestPayload)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          if (!Array.isArray(response.nodes) || !Array.isArray(response.edges)) {
            this.graphResponse.set(null);
            this.backendError.set('Backend returned invalid JSON: arrays `nodes` and `edges` were expected.');
            return;
          }

          this.graphResponse.set(response);

          setTimeout(() => {
            try {
              this.renderGraph(response);
            } catch (renderError) {
              this.backendError.set(
                `Failed to render graph: ${renderError instanceof Error ? renderError.message : String(renderError)}`,
              );
            }
          }, 0);
        },
        error: (error: { error?: unknown; message?: string }) => {
          this.graphResponse.set(null);
          this.cy?.destroy();
          this.cy = null;
          this.backendError.set(this.buildBackendErrorMessage(error));
        },
      });
  }

  private resetOperationsByProgram(): void {
    this.parseErrors.set([]);
    this.synchronizeOperationsFromCurrentProgram();
  }

  private synchronizeOperationsFromCurrentProgram(): void {
    const inferred = this.inferAttributeConfig(this.programText);
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

  private inferAttributeConfig(text: string): Pick<ParsedProgram, 'attributeCount' | 'attributeKinds'> | null {
    const attributesByLine: string[][] = [];
    const lines = text.split(/\r?\n/);

    lines.forEach((rawLine) => {
      const line = rawLine.trim();
      if (!line || line.startsWith('#') || line.startsWith('//') || line.startsWith('%')) {
        return;
      }

      const labelMatch = line.match(/\{\s*([^}]*)\s*\}\s*$/);
      if (!labelMatch) {
        return;
      }

      const attributes = labelMatch[1]
        .split(',')
        .map((value) => value.trim())
        .filter((value) => value.length > 0);

      if (attributes.length === 0) {
        return;
      }

      attributesByLine.push(attributes);
    });

    if (attributesByLine.length === 0) {
      return null;
    }

    const attributeCount = attributesByLine[0].length;
    if (attributeCount === 0) {
      return null;
    }

    const hasInconsistentArity = attributesByLine.some((attributes) => attributes.length !== attributeCount);
    if (hasInconsistentArity) {
      return null;
    }

    const attributeKinds: AttributeKind[] = [];

    for (let attrIndex = 0; attrIndex < attributeCount; attrIndex += 1) {
      let sawNumeric = false;
      let sawText = false;

      attributesByLine.forEach((attributes) => {
        const value = attributes[attrIndex];
        if (this.isNumeric(value)) {
          sawNumeric = true;
          return;
        }

        sawText = true;
      });

      if (sawNumeric && sawText) {
        return null;
      }

      attributeKinds.push(sawText ? 'qualitative' : 'numeric');
    }

    return {
      attributeCount,
      attributeKinds,
    };
  }

  private defaultFunctionsForKind(_kind: AttributeKind): Omit<OperationRow, 'labelName'> {
    return {
      supportFunction: 'X + Y',
      aggregationFunction: 'X * Y',
      conflictFunction: 'X - Y',
    };
  }

  private validateOperations(attributeCount: number): boolean {
    const errors: string[] = [];

    if (this.operationRows.length !== attributeCount) {
      errors.push('The number of operation sets does not match the attribute arity.');
      this.parseErrors.set(errors);
      return false;
    }

    const seenLabelNames = new Set<string>();

    this.operationRows.forEach((row, index) => {
      const normalizedLabelName = row.labelName.trim();
      if (!normalizedLabelName) {
        errors.push(`Attribute ${index + 1}: label name is required.`);
      } else {
        const duplicateKey = normalizedLabelName.toLowerCase();
        if (seenLabelNames.has(duplicateKey)) {
          errors.push(`Attribute ${index + 1}: label name \"${normalizedLabelName}\" is duplicated.`);
        }
        seenLabelNames.add(duplicateKey);
      }

      if (!row.supportFunction.trim()) {
        errors.push(`Attribute ${index + 1}: support function is required.`);
      }
      if (!row.aggregationFunction.trim()) {
        errors.push(`Attribute ${index + 1}: aggregation function is required.`);
      }
      if (!row.conflictFunction.trim()) {
        errors.push(`Attribute ${index + 1}: conflict function is required.`);
      }
    });

    this.parseErrors.set(errors);
    return errors.length === 0;
  }

  private parseProgram(text: string): ParsedProgram | null {
    const facts: FactInput[] = [];
    const rules: RuleInput[] = [];
    const errors: string[] = [];
    const lines = text.split(/\r?\n/);

    const factPattern =
      /^\s*([~]?[A-Za-z][A-Za-z0-9_]*)\s*\(\s*([^)]+?)\s*\)\s*\.\s*\{\s*([^}]*)\s*\}\s*$/;
    const rulePattern =
      /^\s*([~]?[A-Za-z][A-Za-z0-9_]*)\s*\(\s*X\s*\)\s*:-\s*(.+)\.\s*\{\s*([^}]*)\s*\}\s*$/;
    const ruleLiteralPattern = /^\s*([~]?[A-Za-z][A-Za-z0-9_]*)\s*\(\s*X\s*\)\s*$/;

    lines.forEach((rawLine, index) => {
      const line = rawLine.trim();
      if (!line || line.startsWith('#') || line.startsWith('//') || line.startsWith('%')) {
        return;
      }

      const factMatch = line.match(factPattern);
      if (factMatch) {
        const attributes = this.parseAttributes(factMatch[3], index + 1, errors);
        if (!attributes) {
          return;
        }

        facts.push({
          name: factMatch[1],
          argument: factMatch[2].trim(),
          attributes,
        });
        return;
      }

      const ruleMatch = line.match(rulePattern);
      if (ruleMatch) {
        const bodyPieces = ruleMatch[2]
          .split(',')
          .map((piece) => piece.trim())
          .filter((piece) => piece.length > 0);

        const bodyLiterals: string[] = [];
        bodyPieces.forEach((piece) => {
          const literalMatch = piece.match(ruleLiteralPattern);
          if (literalMatch) {
            bodyLiterals.push(literalMatch[1]);
            return;
          }

          errors.push(`Line ${index + 1}: invalid literal in rule body -> ${piece}`);
        });

        const attributes = this.parseAttributes(ruleMatch[3], index + 1, errors);
        if (!attributes || bodyLiterals.length === 0) {
          return;
        }

        rules.push({
          headName: ruleMatch[1],
          bodyLiterals,
          attributes,
        });
        return;
      }

      errors.push(`Line ${index + 1}: does not match fact or rule format.`);
    });

    const allAttributes = [...facts.map((item) => item.attributes), ...rules.map((item) => item.attributes)];

    if (allAttributes.length === 0) {
      errors.push('You must provide at least one fact or rule with labels.');
    }

    const attributeCount = allAttributes.length > 0 ? allAttributes[0].length : 0;

    allAttributes.forEach((attributes, index) => {
      if (attributes.length !== attributeCount) {
        errors.push(
          `Inconsistent arity at element ${index + 1}: expected ${attributeCount} attributes but found ${attributes.length}.`,
        );
      }
    });

    const kinds: (AttributeKind | 'mixed')[] = Array.from({ length: attributeCount }, () => 'numeric');

    for (let attrIndex = 0; attrIndex < attributeCount; attrIndex += 1) {
      let sawNumeric = false;
      let sawText = false;

      allAttributes.forEach((attributes) => {
        const value = attributes[attrIndex];
        if (this.isNumeric(value)) {
          sawNumeric = true;
        } else {
          sawText = true;
        }
      });

      if (sawNumeric && sawText) {
        kinds[attrIndex] = 'mixed';
        errors.push(
          `Attribute ${attrIndex + 1}: mixes numeric and qualitative values. Use one type per attribute.`,
        );
      } else if (sawText) {
        kinds[attrIndex] = 'qualitative';
      }
    }

    if (errors.length > 0) {
      this.parseErrors.set(errors);
      return null;
    }

    this.parseErrors.set([]);
    return {
      facts,
      rules,
      attributeCount,
      attributeKinds: kinds.map((kind) => (kind === 'mixed' ? 'numeric' : kind)),
    };
  }

  private parseAttributes(rawAttributes: string, lineNumber: number, errors: string[]): string[] | null {
    const attributes = rawAttributes
      .split(',')
      .map((item) => item.trim())
      .filter((item) => item.length > 0);

    if (attributes.length === 0) {
      errors.push(`Line ${lineNumber}: label block cannot be empty.`);
      return null;
    }

    return attributes;
  }

  private isNumeric(value: string): boolean {
    const asNumber = Number(value);
    return Number.isFinite(asNumber);
  }

  private buildBackendErrorMessage(error: { error?: unknown; message?: string }): string {
    if (typeof error.error === 'string') {
      return error.error;
    }

    if (error.error && typeof error.error === 'object') {
      return JSON.stringify(error.error);
    }

    return error.message ?? 'Unexpected error while calling backend.';
  }

  private renderGraph(graph: GraphResponse): void {
    const container = this.graphCanvas?.nativeElement;
    if (!container) {
      return;
    }

    this.cy?.destroy();

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
      layout: {
        ...this.createLayoutOptions(),
      },
    });

    this.cy.on('tap', 'node', (event) => {
      const tappedId = event.target.data('id') as string;
      const node = graph.nodes.find((item) => item.id === tappedId) ?? null;
      this.cy?.nodes().removeClass('node-selected');
      if (node) {
        event.target.addClass('node-selected');
      }
      this.selectedNode.set(node);
    });

    this.cy.on('tap', (event) => {
      if (event.target !== this.cy) {
        return;
      }

      this.cy?.nodes().removeClass('node-selected');
      this.selectedNode.set(null);
    });
  }

  private buildVisualGraph(graph: GraphResponse): { nodes: VisualNode[]; edges: VisualEdge[] } {
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
      if (hiddenAggregationNodes.has(targetId) && supportTargetsHandledByAggregation.has(targetId)) {
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
    const collapsedVisual = this.collapseDuplicateFactVisualNodes(nodes, finalEdges, visibleConflictEdges);
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
    const columnCount = Math.max(node.attributes?.length ?? 0, node.deltaAttributes?.length ?? 0, 1);
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
    const columnCount = Math.max(node.attributes?.length ?? 0, node.deltaAttributes?.length ?? 0, 1);
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

    const preferredBreak = normalized.includes('(') && !normalized.includes(':-') ? normalized.replace('(', '\n(') : normalized;
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

  private normalizeAttributeValues(values: string[] | undefined, targetLength: number, maxDecimals: number): string[] {
    const normalized = values ? values.map((value) => this.formatValueWithPrecision(value, maxDecimals)) : [];
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
      rows.push({
        labelName,
        color: this.detailBarPalette[index % this.detailBarPalette.length],
        mu: this.buildDetailCell(node.attributes?.[index]),
        delta: this.buildDetailCell(node.deltaAttributes?.[index]),
      });
    }

    return rows;
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
