import { TestBed } from '@angular/core/testing';
import { App } from './app';

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
      'Labeled Argumentation Frameworks (LAF)',
    );
    expect(compiled.querySelector('.page-header p')?.textContent).toContain(
      'A formalism that models arguments',
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

  it('should parse semicolon-separated attributes and intervals using lower bounds', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;

    const parsed = app.parseProgram(
      `basicServices(houseA). {0.5; [0.6, 1.0]}\nquietArea(X) :- basicServices(X). {0.7; [0.8, 0.9]}`,
    );

    expect(parsed).not.toBeNull();
    expect(parsed.facts[0].attributes).toEqual(['0.5', '0.6']);
    expect(parsed.facts[0].attributeIntervals).toEqual([null, [0.6, 1]]);
    expect(parsed.rules[0].attributes).toEqual(['0.7', '0.8']);
    expect(parsed.rules[0].attributeIntervals).toEqual([null, [0.8, 0.9]]);
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
    const app = fixture.componentInstance as any;

    const visual: {
      nodes: Array<{ id: string; label: string; type: string }>;
      edges: Array<{ from: string; to: string; kind: string }>;
    } = app.buildVisualGraph({
      nodes: [
        {
          id: 'F_BUY_1',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        },
        {
          id: 'F_BUY_2',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
        {
          id: 'F_NOT_BUY',
          label: '~buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
        {
          id: 'R_BUY',
          label: 'buy(X) :- goodArea(X).',
          type: 'RULE',
          attributes: ['0.85', '1'],
          deltaAttributes: ['0.85', '1'],
        },
        {
          id: 'R_NOT_BUY',
          label: '~buy(X) :- ~goodArea(X).',
          type: 'RULE',
          attributes: ['0.5', '0.8'],
          deltaAttributes: ['0.5', '0.8'],
        },
      ],
      edges: [
        { from: 'R_BUY', to: 'F_BUY_1', kind: 'SUPPORT' },
        { from: 'R_BUY', to: 'F_BUY_2', kind: 'SUPPORT' },
        { from: 'R_NOT_BUY', to: 'F_NOT_BUY', kind: 'SUPPORT' },
        { from: 'F_BUY_1', to: 'F_NOT_BUY', kind: 'CONFLICT' },
        { from: 'F_NOT_BUY', to: 'F_BUY_1', kind: 'CONFLICT' },
        { from: 'F_BUY_2', to: 'F_NOT_BUY', kind: 'CONFLICT' },
        { from: 'F_NOT_BUY', to: 'F_BUY_2', kind: 'CONFLICT' },
      ],
    });

    const buyFactNodes = visual.nodes.filter(
      (node: { id: string; label: string; type: string }) => {
        return node.type === 'FACT' && node.label === 'buy(houseA)';
      },
    );
    expect(buyFactNodes.length).toBe(1);

    const caNodes = visual.nodes.filter(
      (node: { id: string; label: string; type: string }) => node.type === 'CA',
    );
    expect(caNodes.length).toBe(1);

    const conflictEdges = visual.edges.filter(
      (edge: { from: string; to: string; kind: string }) => {
        return edge.kind === 'CONFLICT';
      },
    );
    expect(conflictEdges.length).toBe(2);
  });

  it('should keep only aggregated target and avoid duplicated buy conflict nodes', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;

    const visual: {
      nodes: Array<{ id: string; label: string; type: string }>;
      edges: Array<{ from: string; to: string; kind: string }>;
    } = app.buildVisualGraph({
      nodes: [
        {
          id: 'F_QA',
          label: 'quietArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        },
        {
          id: 'F_BS',
          label: 'basicServices(houseA)',
          type: 'FACT',
          attributes: ['0.75', '0.95'],
          deltaAttributes: ['0.75', '0.95'],
        },
        {
          id: 'R_GOOD_Q',
          label: 'goodArea(X) :- quietArea(X).',
          type: 'RULE',
          attributes: ['0.75', '0.9'],
          deltaAttributes: ['0.75', '0.9'],
        },
        {
          id: 'R_GOOD_B',
          label: 'goodArea(X) :- basicServices(X).',
          type: 'RULE',
          attributes: ['0.75', '0.95'],
          deltaAttributes: ['0.75', '0.95'],
        },
        {
          id: 'F_GOOD_A',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        },
        {
          id: 'F_GOOD_B',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        },
        {
          id: 'F_GOOD_FINAL',
          label: 'goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
        {
          id: 'R_BUY',
          label: 'buy(X) :- goodArea(X).',
          type: 'RULE',
          attributes: ['0.85', '1'],
          deltaAttributes: ['0.85', '1'],
        },
        {
          id: 'R_NOT_BUY',
          label: '~buy(X) :- ~goodArea(X).',
          type: 'RULE',
          attributes: ['0.5', '0.8'],
          deltaAttributes: ['0.5', '0.8'],
        },
        {
          id: 'F_BUY_1',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['1', '1'],
        },
        {
          id: 'F_BUY_2',
          label: 'buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
        {
          id: 'F_NOT_GOOD',
          label: '~goodArea(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
        {
          id: 'F_NOT_BUY',
          label: '~buy(houseA)',
          type: 'FACT',
          attributes: ['1', '1'],
          deltaAttributes: ['0', '0'],
        },
      ],
      edges: [
        { from: 'F_QA', to: 'F_GOOD_A', kind: 'SUPPORT' },
        { from: 'R_GOOD_Q', to: 'F_GOOD_A', kind: 'SUPPORT' },
        { from: 'F_BS', to: 'F_GOOD_B', kind: 'SUPPORT' },
        { from: 'R_GOOD_B', to: 'F_GOOD_B', kind: 'SUPPORT' },
        { from: 'F_GOOD_A', to: 'F_GOOD_FINAL', kind: 'AGGREGATION' },
        { from: 'F_GOOD_B', to: 'F_GOOD_FINAL', kind: 'AGGREGATION' },
        { from: 'F_GOOD_FINAL', to: 'F_BUY_1', kind: 'SUPPORT' },
        { from: 'R_BUY', to: 'F_BUY_1', kind: 'SUPPORT' },
        { from: 'F_GOOD_FINAL', to: 'F_BUY_2', kind: 'SUPPORT' },
        { from: 'R_BUY', to: 'F_BUY_2', kind: 'SUPPORT' },
        { from: 'F_NOT_GOOD', to: 'F_NOT_BUY', kind: 'SUPPORT' },
        { from: 'R_NOT_BUY', to: 'F_NOT_BUY', kind: 'SUPPORT' },
        { from: 'F_BUY_1', to: 'F_NOT_BUY', kind: 'CONFLICT' },
        { from: 'F_NOT_BUY', to: 'F_BUY_1', kind: 'CONFLICT' },
        { from: 'F_BUY_2', to: 'F_NOT_BUY', kind: 'CONFLICT' },
        { from: 'F_NOT_BUY', to: 'F_BUY_2', kind: 'CONFLICT' },
      ],
    });

    const byId = new Set(visual.nodes.map((node: { id: string }) => node.id));
    expect(byId.has('F_GOOD_A')).toBe(false);
    expect(byId.has('F_GOOD_B')).toBe(false);

    const buyFacts = visual.nodes.filter((node: { id: string; label: string; type: string }) => {
      return node.type === 'FACT' && node.label === 'buy(houseA)';
    });
    expect(buyFacts.length).toBe(1);

    const goodAreaFacts = visual.nodes.filter(
      (node: { id: string; label: string; type: string }) => {
        return node.type === 'FACT' && node.label === 'goodArea(houseA)';
      },
    );
    expect(goodAreaFacts.length).toBe(1);

    const caNodes = visual.nodes.filter(
      (node: { id: string; label: string; type: string }) => node.type === 'CA',
    );
    expect(caNodes.length).toBe(1);

    const targetGoodAreaId = goodAreaFacts[0].id;
    const nodeTypeById = new Map(
      visual.nodes.map((node: { id: string; type: string }) => [node.id, node.type]),
    );
    const incomingSupportToGoodArea = visual.edges.filter(
      (edge: { from: string; to: string; kind: string }) => {
        return (
          edge.kind === 'SUPPORT' &&
          edge.to === targetGoodAreaId &&
          nodeTypeById.get(edge.from) === 'DMP'
        );
      },
    );
    expect(incomingSupportToGoodArea.length).toBe(2);
  });
});
