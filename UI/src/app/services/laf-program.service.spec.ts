import { LafProgramService, OperationRow } from './laf-program.service';

describe('LafProgramService', () => {
  let service: LafProgramService;

  beforeEach(() => {
    service = new LafProgramService();
  });

  it('should parse semicolon-separated attributes and intervals using lower bounds', () => {
    const result = service.parseProgram(
      `basicServices(houseA). {0.5; [0.6, 1.0]}\nquietArea(X) :- basicServices(X). {0.7; [0.8, 0.9]}`,
    );

    expect(result.errors).toEqual([]);
    expect(result.parsed).not.toBeNull();

    const parsed = result.parsed;
    if (!parsed) {
      return;
    }

    expect(parsed.facts[0].attributes).toEqual(['0.5', '0.6']);
    expect(parsed.facts[0].attributeIntervals).toEqual([null, [0.6, 1]]);
    expect(parsed.rules[0].attributes).toEqual(['0.7', '0.8']);
    expect(parsed.rules[0].attributeIntervals).toEqual([null, [0.8, 0.9]]);
  });

  it('should reject comma-separated labels outside interval bounds', () => {
    const result = service.parseProgram('basicServices(houseA). {0.5, 0.7}');

    expect(result.parsed).toBeNull();
    expect(result.errors).toContain(
      'Line 1: invalid label separators. Use semicolons between attributes and commas only inside intervals [min, max].',
    );
  });

  it('should infer qualitative and numeric attribute kinds consistently', () => {
    const inferred = service.inferAttributeConfig(
      `goodArea(houseA). {0.8;high;trusted}\nbuy(X) :- goodArea(X). {0.85;expert;reliable}`,
    );

    expect(inferred).not.toBeNull();
    expect(inferred).toEqual({
      attributeCount: 3,
      attributeKinds: ['numeric', 'qualitative', 'qualitative'],
    });
  });

  it('should require unique and non-empty operation label names', () => {
    const rows: OperationRow[] = [
      {
        labelName: 'confidence',
        supportFunction: 'X+Y',
        aggregationFunction: 'X*Y',
        conflictFunction: 'X-Y',
      },
      {
        labelName: 'confidence',
        supportFunction: 'X+Y',
        aggregationFunction: 'X*Y',
        conflictFunction: 'X-Y',
      },
      {
        labelName: '',
        supportFunction: 'X+Y',
        aggregationFunction: 'X*Y',
        conflictFunction: 'X-Y',
      },
    ];

    const errors = service.validateOperations(rows, 3);

    expect(errors).toContain('Attribute 2: label name "confidence" is duplicated.');
    expect(errors).toContain('Attribute 3: label name is required.');
  });
});
