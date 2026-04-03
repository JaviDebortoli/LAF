import { TestBed } from '@angular/core/testing';
import { App, type GraphEdge, type GraphNode, type GraphResponse } from './app';

interface VisualGraphResult {
  nodes: Array<{ id: string; label: string; type: string }>;
  edges: GraphEdge[];
}

interface AppVisualGraphAccess {
  buildVisualGraph(graph: GraphResponse): VisualGraphResult;
}

const createNode = (node: GraphNode): GraphNode => node;
const createEdge = (edge: GraphEdge): GraphEdge => edge;

describe('App', () => {
  const THREE_LABELS_PROGRAM = `goodArea(houseA). {0.8;high;trusted}
buy(X) :- goodArea(X). {0.85;expert;reliable}`;

  const ONE_LABEL_PROGRAM = `goodArea(houseA). {0.8}
buy(X) :- goodArea(X). {0.85}`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain(
      'Argumentation Intelligence Studio',
    );
    expect(compiled.querySelector('.page-header p')?.textContent).toContain(
      'A unified workspace for exploring and comparing argumentation formalisms.',
    );
  });

  it('should render dynamic tabs based on detected labels', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange(THREE_LABELS_PROGRAM);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.operation-tab').length).toBe(3);
    expect(compiled.querySelector('.operation-tab.active')?.textContent).toContain('label_1');
  });

  it('should switch active label tab on click', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange(THREE_LABELS_PROGRAM);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const tabs = compiled.querySelectorAll<HTMLButtonElement>('.operation-tab');
    tabs[1].click();
    fixture.detectChanges();

    expect(app.activeOperationTabIndex).toBe(1);
    expect(compiled.querySelector('.operation-tab.active')?.textContent).toContain('label_2');
  });

  it('should allow editing individual label names', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange(THREE_LABELS_PROGRAM);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const labelNameInput = compiled.querySelector<HTMLInputElement>(
      '.operation-item input[type="text"]',
    );
    expect(labelNameInput).not.toBeNull();

    if (!labelNameInput) {
      return;
    }

    labelNameInput.value = 'trustLevel';
    labelNameInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(app.operationRows[0].labelName).toBe('trustLevel');
    expect(compiled.querySelector('.operation-tab.active')?.textContent).toContain('trustLevel');
  });

  it('should clamp active tab index when label count decreases', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange(THREE_LABELS_PROGRAM);
    app.setActiveOperationTab(2);
    fixture.detectChanges();

    app.onProgramTextChange(ONE_LABEL_PROGRAM);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(app.activeOperationTabIndex).toBe(0);
    expect(compiled.querySelectorAll('.operation-tab').length).toBe(1);
    expect(compiled.querySelector('.operation-tab.active')?.textContent).toContain('label_1');
  });

  it('should require unique non-empty label names before processing', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange(THREE_LABELS_PROGRAM);
    fixture.detectChanges();

    app.operationRows[0].labelName = 'confidence';
    app.operationRows[1].labelName = 'confidence';
    app.operationRows[2].labelName = '';

    app.processProgram();

    expect(app.parseErrors()).toContain('Attribute 2: label name "confidence" is duplicated.');
    expect(app.parseErrors()).toContain('Attribute 3: label name is required.');
  });

  it('should reject comma-separated labels outside interval bounds', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    app.onProgramTextChange('basicServices(houseA). {0.5, 0.7}');
    app.processProgram();

    expect(app.parseErrors()).toContain(
      'Line 1: invalid label separators. Use semicolons between attributes and commas only inside intervals [min, max].',
    );
  });

  it('should collapse duplicate fact nodes before creating CA conflicts', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as unknown as AppVisualGraphAccess;

    const visual = app.buildVisualGraph({
      nodes: [
        createNode({
          id: 'F_BUY_1',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        }),
        createNode({
          id: 'F_BUY_2',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
        createNode({
          id: 'F_NOT_BUY',
          label: '~buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
        createNode({
          id: 'R_BUY',
          label: 'buy(X) :- goodArea(X).',
          type: 'RULE',
          attributes: ['0.85', '1'],
          deltaAttributes: ['0.85', '1'],
        }),
        createNode({
          id: 'R_NOT_BUY',
          label: '~buy(X) :- ~goodArea(X).',
          type: 'RULE',
          attributes: ['0.5', '0.8'],
          deltaAttributes: ['0.5', '0.8'],
        }),
      ],
      edges: [
        createEdge({ from: 'R_BUY', to: 'F_BUY_1', kind: 'SUPPORT' }),
        createEdge({ from: 'R_BUY', to: 'F_BUY_2', kind: 'SUPPORT' }),
        createEdge({ from: 'R_NOT_BUY', to: 'F_NOT_BUY', kind: 'SUPPORT' }),
        createEdge({ from: 'F_BUY_1', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY_1', kind: 'CONFLICT' }),
        createEdge({ from: 'F_BUY_2', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY_2', kind: 'CONFLICT' }),
      ],
    });

    const buyFactNodes = visual.nodes.filter(
      (node) => node.type === 'FACT' && node.label === 'buy(houseA)',
    );
    expect(buyFactNodes.length).toBe(1);

    const caNodes = visual.nodes.filter((node) => node.type === 'CA');
    expect(caNodes.length).toBe(1);

    const conflictEdges = visual.edges.filter((edge) => edge.kind === 'CONFLICT');
    expect(conflictEdges.length).toBe(2);
  });

  it('should keep only aggregated target and avoid duplicated buy conflict nodes', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as unknown as AppVisualGraphAccess;

    const visual = app.buildVisualGraph({
      nodes: [
        createNode({
          id: 'F_QA',
          label: 'quietArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        }),
        createNode({
          id: 'F_BS',
          label: 'basicServices(houseA)',
          type: 'FACT',
          attributes: ['0.75', '0.95'],
          deltaAttributes: ['0.75', '0.95'],
        }),
        createNode({
          id: 'R_GOOD_Q',
          label: 'goodArea(X) :- quietArea(X).',
          type: 'RULE',
          attributes: ['0.75', '0.9'],
          deltaAttributes: ['0.75', '0.9'],
        }),
        createNode({
          id: 'R_GOOD_B',
          label: 'goodArea(X) :- basicServices(X).',
          type: 'RULE',
          attributes: ['0.75', '0.95'],
          deltaAttributes: ['0.75', '0.95'],
        }),
        createNode({
          id: 'F_GOOD_A',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        }),
        createNode({
          id: 'F_GOOD_B',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        }),
        createNode({
          id: 'F_GOOD_FINAL',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
        createNode({
          id: 'R_BUY',
          label: 'buy(X) :- goodArea(X).',
          type: 'RULE',
          attributes: ['0.85', '1'],
          deltaAttributes: ['0.85', '1'],
        }),
        createNode({
          id: 'R_NOT_BUY',
          label: '~buy(X) :- ~goodArea(X).',
          type: 'RULE',
          attributes: ['0.5', '0.8'],
          deltaAttributes: ['0.5', '0.8'],
        }),
        createNode({
          id: 'F_BUY_1',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        }),
        createNode({
          id: 'F_BUY_2',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
        createNode({
          id: 'F_NOT_GOOD',
          label: '~goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
        createNode({
          id: 'F_NOT_BUY',
          label: '~buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        }),
      ],
      edges: [
        createEdge({ from: 'F_QA', to: 'F_GOOD_A', kind: 'SUPPORT' }),
        createEdge({ from: 'R_GOOD_Q', to: 'F_GOOD_A', kind: 'SUPPORT' }),
        createEdge({ from: 'F_BS', to: 'F_GOOD_B', kind: 'SUPPORT' }),
        createEdge({ from: 'R_GOOD_B', to: 'F_GOOD_B', kind: 'SUPPORT' }),
        createEdge({ from: 'F_GOOD_A', to: 'F_GOOD_FINAL', kind: 'AGGREGATION' }),
        createEdge({ from: 'F_GOOD_B', to: 'F_GOOD_FINAL', kind: 'AGGREGATION' }),
        createEdge({ from: 'F_GOOD_FINAL', to: 'F_BUY_1', kind: 'SUPPORT' }),
        createEdge({ from: 'R_BUY', to: 'F_BUY_1', kind: 'SUPPORT' }),
        createEdge({ from: 'F_GOOD_FINAL', to: 'F_BUY_2', kind: 'SUPPORT' }),
        createEdge({ from: 'R_BUY', to: 'F_BUY_2', kind: 'SUPPORT' }),
        createEdge({ from: 'F_NOT_GOOD', to: 'F_NOT_BUY', kind: 'SUPPORT' }),
        createEdge({ from: 'R_NOT_BUY', to: 'F_NOT_BUY', kind: 'SUPPORT' }),
        createEdge({ from: 'F_BUY_1', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY_1', kind: 'CONFLICT' }),
        createEdge({ from: 'F_BUY_2', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY_2', kind: 'CONFLICT' }),
      ],
    });

    const byId = new Set(visual.nodes.map((node) => node.id));
    expect(byId.has('F_GOOD_A')).toBe(false);
    expect(byId.has('F_GOOD_B')).toBe(false);

    const buyFacts = visual.nodes.filter(
      (node) => node.type === 'FACT' && node.label === 'buy(houseA)',
    );
    expect(buyFacts.length).toBe(1);

    const goodAreaFacts = visual.nodes.filter(
      (node) => node.type === 'FACT' && node.label === 'goodArea(houseA)',
    );
    expect(goodAreaFacts.length).toBe(1);

    const caNodes = visual.nodes.filter((node) => node.type === 'CA');
    expect(caNodes.length).toBe(1);

    const targetGoodAreaId = goodAreaFacts[0].id;
    const nodeTypeById = new Map(visual.nodes.map((node) => [node.id, node.type]));
    const incomingSupportToGoodArea = visual.edges.filter((edge) => {
      return (
        edge.kind === 'SUPPORT' &&
        edge.to === targetGoodAreaId &&
        nodeTypeById.get(edge.from) === 'DMP'
      );
    });
    expect(incomingSupportToGoodArea.length).toBe(2);
  });

  it('should introduce a single dMP node when one conclusion has multiple visible supports', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as unknown as AppVisualGraphAccess;

    const visual = app.buildVisualGraph({
      nodes: [
        createNode({
          id: 'F_PREMISE_1',
          label: 'basicServices(houseA)',
          type: 'FACT',
          attributes: ['0.8'],
          deltaAttributes: ['0.8'],
        }),
        createNode({
          id: 'F_PREMISE_2',
          label: 'quietArea(houseA)',
          type: 'FACT',
          attributes: ['0.9'],
          deltaAttributes: ['0.9'],
        }),
        createNode({
          id: 'R_GOOD',
          label: 'goodArea(X) :- quietArea(X).',
          type: 'RULE',
          attributes: ['0.7'],
          deltaAttributes: ['0.7'],
        }),
        createNode({
          id: 'F_GOOD',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1'],
          deltaAttributes: ['1'],
        }),
      ],
      edges: [
        createEdge({ from: 'F_PREMISE_1', to: 'F_GOOD', kind: 'SUPPORT' }),
        createEdge({ from: 'F_PREMISE_2', to: 'F_GOOD', kind: 'SUPPORT' }),
        createEdge({ from: 'R_GOOD', to: 'F_GOOD', kind: 'SUPPORT' }),
      ],
    });

    const dmpNodes = visual.nodes.filter((node) => node.type === 'DMP');
    expect(dmpNodes.length).toBe(1);

    const incomingToDmp = visual.edges.filter(
      (edge) => edge.kind === 'SUPPORT' && edge.to === dmpNodes[0].id,
    );
    expect(incomingToDmp.length).toBe(3);

    const incomingToGoodFact = visual.edges.filter(
      (edge) => edge.kind === 'SUPPORT' && edge.to === 'F_GOOD',
    );
    expect(incomingToGoodFact).toEqual([
      {
        from: dmpNodes[0].id,
        to: 'F_GOOD',
        kind: 'SUPPORT',
      },
    ]);
  });

  it('should collapse repeated conflict edges into a single CA mediator pair', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as unknown as AppVisualGraphAccess;

    const visual = app.buildVisualGraph({
      nodes: [
        createNode({
          id: 'F_BUY',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1'],
          deltaAttributes: ['1'],
        }),
        createNode({
          id: 'F_NOT_BUY',
          label: '~buy(houseA)',
          type: 'FACT',
          attributes: ['1'],
          deltaAttributes: ['0'],
        }),
      ],
      edges: [
        createEdge({ from: 'F_BUY', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_BUY', to: 'F_NOT_BUY', kind: 'CONFLICT' }),
        createEdge({ from: 'F_NOT_BUY', to: 'F_BUY', kind: 'CONFLICT' }),
      ],
    });

    const caNodes = visual.nodes.filter((node) => node.type === 'CA');
    expect(caNodes.length).toBe(1);

    const conflictEdges = visual.edges.filter((edge) => edge.kind === 'CONFLICT');
    expect(conflictEdges).toEqual([
      { from: 'F_BUY', to: caNodes[0].id, kind: 'CONFLICT' },
      { from: 'F_NOT_BUY', to: caNodes[0].id, kind: 'CONFLICT' },
    ]);
  });
});
