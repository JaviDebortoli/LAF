import { Injectable } from '@angular/core';

export interface FactInput {
  name: string;
  argument: string;
  attributes: string[];
  attributeIntervals: (readonly [number, number] | null)[];
  sourceKey: string;
}

export interface RuleInput {
  headName: string;
  bodyLiterals: string[];
  attributes: string[];
  attributeIntervals: (readonly [number, number] | null)[];
  sourceKey: string;
}

export type AttributeKind = 'numeric' | 'qualitative';

export interface ParsedProgram {
  facts: FactInput[];
  rules: RuleInput[];
  attributeCount: number;
  attributeKinds: AttributeKind[];
}

export interface OperationRow {
  labelName: string;
  supportFunction: string;
  aggregationFunction: string;
  conflictFunction: string;
}

interface ParsedAttributeToken {
  value: string;
  interval: readonly [number, number] | null;
}

export interface ParseProgramResult {
  parsed: ParsedProgram | null;
  errors: string[];
}

@Injectable({ providedIn: 'root' })
export class LafProgramService {
  inferAttributeConfig(
    text: string,
  ): Pick<ParsedProgram, 'attributeCount' | 'attributeKinds'> | null {
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

      const tokens = this.splitAttributesFromLabelBlock(labelMatch[1]);
      if (!tokens) {
        return;
      }

      const attributes = tokens
        .map((value) => this.parseAttributeToken(value))
        .filter((token): token is ParsedAttributeToken => token !== null)
        .map((token) => token.value);

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

    const hasInconsistentArity = attributesByLine.some(
      (attributes) => attributes.length !== attributeCount,
    );
    if (hasInconsistentArity) {
      return null;
    }

    const attributeKinds: AttributeKind[] = [];

    for (let attrIndex = 0; attrIndex < attributeCount; attrIndex += 1) {
      let sawNumeric = false;
      let sawText = false;

      attributesByLine.forEach((attributes) => {
        const value = attributes[attrIndex];
        if (this.isNumericLike(value)) {
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

  parseProgram(text: string): ParseProgramResult {
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
        const parsedAttributes = this.parseAttributes(factMatch[3], index + 1, errors);
        if (!parsedAttributes) {
          return;
        }

        const attributes = parsedAttributes.map((item) => item.value);
        const attributeIntervals = parsedAttributes.map((item) => item.interval);

        facts.push({
          name: factMatch[1],
          argument: factMatch[2].trim(),
          attributes,
          attributeIntervals,
          sourceKey: this.buildFactSourceKey(factMatch[1], factMatch[2].trim()),
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

        const parsedAttributes = this.parseAttributes(ruleMatch[3], index + 1, errors);
        if (!parsedAttributes || bodyLiterals.length === 0) {
          return;
        }

        const attributes = parsedAttributes.map((item) => item.value);
        const attributeIntervals = parsedAttributes.map((item) => item.interval);

        rules.push({
          headName: ruleMatch[1],
          bodyLiterals,
          attributes,
          attributeIntervals,
          sourceKey: this.buildRuleSourceKey(index + 1),
        });
        return;
      }

      errors.push(`Line ${index + 1}: does not match fact or rule format.`);
    });

    const allAttributes = [
      ...facts.map((item) => item.attributes),
      ...rules.map((item) => item.attributes),
    ];

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

    const kinds: (AttributeKind | 'mixed')[] = Array.from(
      { length: attributeCount },
      () => 'numeric',
    );

    for (let attrIndex = 0; attrIndex < attributeCount; attrIndex += 1) {
      let sawNumeric = false;
      let sawText = false;

      allAttributes.forEach((attributes) => {
        const value = attributes[attrIndex];
        if (this.isNumericLike(value)) {
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
      return {
        parsed: null,
        errors,
      };
    }

    return {
      parsed: {
        facts,
        rules,
        attributeCount,
        attributeKinds: kinds.map((kind) => (kind === 'mixed' ? 'numeric' : kind)),
      },
      errors: [],
    };
  }

  validateOperations(operationRows: OperationRow[], attributeCount: number): string[] {
    const errors: string[] = [];

    if (operationRows.length !== attributeCount) {
      errors.push('The number of operation sets does not match the attribute arity.');
      return errors;
    }

    const seenLabelNames = new Set<string>();

    operationRows.forEach((row, index) => {
      const normalizedLabelName = row.labelName.trim();
      if (!normalizedLabelName) {
        errors.push(`Attribute ${index + 1}: label name is required.`);
      } else {
        const duplicateKey = normalizedLabelName.toLowerCase();
        if (seenLabelNames.has(duplicateKey)) {
          errors.push(`Attribute ${index + 1}: label name "${normalizedLabelName}" is duplicated.`);
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

    return errors;
  }

  applyIntervalSelections(
    parsed: ParsedProgram,
    intervalSelections: ReadonlyMap<string, number>,
  ): ParsedProgram {
    const applySelections = <T extends FactInput | RuleInput>(items: T[]): T[] => {
      return items.map((item) => {
        const nextAttributes = [...item.attributes];

        item.attributeIntervals.forEach((bounds, index) => {
          if (!bounds) {
            return;
          }

          const selectionKey = this.buildIntervalSelectionKey(item.sourceKey, index);
          const selected = intervalSelections.get(selectionKey);
          if (selected === undefined) {
            return;
          }

          const clamped = this.clamp(selected, bounds[0], bounds[1]);
          nextAttributes[index] = this.normalizeNumeric(clamped);
        });

        return {
          ...item,
          attributes: nextAttributes,
        };
      });
    };

    return {
      ...parsed,
      facts: applySelections(parsed.facts),
      rules: applySelections(parsed.rules),
    };
  }

  buildIntervalSelectionKey(sourceKey: string, attributeIndex: number): string {
    return `${sourceKey}|${attributeIndex}`;
  }

  private parseAttributes(
    rawAttributes: string,
    lineNumber: number,
    errors: string[],
  ): ParsedAttributeToken[] | null {
    const rawParts = this.splitAttributesFromLabelBlock(rawAttributes);
    if (!rawParts) {
      errors.push(
        `Line ${lineNumber}: invalid label separators. Use semicolons between attributes and commas only inside intervals [min, max].`,
      );
      return null;
    }

    const tokens = rawParts
      .map((part) => this.parseAttributeToken(part))
      .filter((token): token is ParsedAttributeToken => token !== null);

    if (tokens.length === 0) {
      errors.push(`Line ${lineNumber}: label block cannot be empty.`);
      return null;
    }

    if (tokens.length !== rawParts.length) {
      errors.push(`Line ${lineNumber}: malformed attribute in label block.`);
      return null;
    }

    return tokens;
  }

  private splitAttributesFromLabelBlock(rawAttributes: string): string[] | null {
    const parts: string[] = [];
    let current = '';
    let bracketDepth = 0;

    for (let index = 0; index < rawAttributes.length; index += 1) {
      const char = rawAttributes[index];

      if (char === '[') {
        bracketDepth += 1;
        current += char;
        continue;
      }

      if (char === ']') {
        if (bracketDepth === 0) {
          return null;
        }

        bracketDepth -= 1;
        current += char;
        continue;
      }

      if (char === ';' && bracketDepth === 0) {
        const trimmed = current.trim();
        if (!trimmed) {
          return null;
        }

        parts.push(trimmed);
        current = '';
        continue;
      }

      if (char === ',' && bracketDepth === 0) {
        return null;
      }

      current += char;
    }

    if (bracketDepth !== 0) {
      return null;
    }

    const tail = current.trim();
    if (!tail) {
      return parts.length > 0 ? null : [];
    }

    parts.push(tail);
    return parts;
  }

  private parseAttributeToken(rawToken: string): ParsedAttributeToken | null {
    const token = rawToken.trim();
    if (!token) {
      return null;
    }

    const intervalMatch = token.match(
      /^\[\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))\s*,\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))\s*\]$/,
    );
    if (intervalMatch) {
      const first = Number(intervalMatch[1]);
      const second = Number(intervalMatch[2]);
      if (!Number.isFinite(first) || !Number.isFinite(second)) {
        return null;
      }

      const min = Math.min(first, second);
      const max = Math.max(first, second);
      return {
        value: this.normalizeNumeric(min),
        interval: [min, max],
      };
    }

    return {
      value: token,
      interval: null,
    };
  }

  private normalizeNumeric(value: number): string {
    const compact = value.toString();
    return compact === '-0' ? '0' : compact;
  }

  private isNumericLike(value: string): boolean {
    const asNumber = Number(value);
    return Number.isFinite(asNumber);
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

  private buildFactSourceKey(name: string, argument: string): string {
    return `FACT|${name}|${argument}`;
  }

  private buildRuleSourceKey(lineNumber: number): string {
    return `RULE|line:${lineNumber}`;
  }
}
