package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.Rule;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Stateless service responsible for mapping input DTOs into
 * domain-level objects used by the inference engine.
 *
 * <p>
 * This service performs pure transformations and does not keep
 * any internal state, making it thread-safe and suitable for
 * request-driven execution.
 * </p>
 * 
 * @author JaviDebórtoli
 */
@Service
public class ProgramMapperService {
    private static final Pattern INTERVAL_PATTERN = Pattern.compile(
            "^\\[\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*,\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*\\]$");

    /**
     * Maps a list of {@link FactDTO} objects into a list of domain {@link Fact}s.
     *
     * @param factDtos the list of fact DTOs received from the client
     * @return a list of mapped {@link Fact} instances (empty if input is null)
     */
    public List<Fact> mapFacts(List<FactDTO> factDtos) {
        List<Fact> facts = new ArrayList<>();

        if (factDtos == null || factDtos.isEmpty()) {
            return facts;
        }

        for (FactDTO dto : factDtos) {
            ParsedAttributes parsedAttributes = normalizeAttributes(dto.getAttributes(), dto.getAttributeIntervals());
            Fact fact = new Fact(
                    dto.getName(),
                    dto.getArgument(),
                    parsedAttributes.values(),
                    parsedAttributes.intervals(),
                    dto.getSourceKey()
            );
            facts.add(fact);
        }

        return facts;
    }
    /**
     * Maps a list of {@link RuleDTO} objects into a list of domain {@link Rule}s.
     *
     * @param ruleDtos the list of rule DTOs received from the client
     * @return a list of mapped {@link Rule} instances (empty if input is null)
     */
    public List<Rule> mapRules(List<RuleDTO> ruleDtos) {
        List<Rule> rules = new ArrayList<>();

        if (ruleDtos == null || ruleDtos.isEmpty()) {
            return rules;
        }

        for (RuleDTO dto : ruleDtos) {
            ParsedAttributes parsedAttributes = normalizeAttributes(dto.getAttributes(), dto.getAttributeIntervals());
            Rule rule = new Rule(
                    dto.getHeadName(),
                    dto.getBodyLiterals(),
                    parsedAttributes.values(),
                    parsedAttributes.intervals(),
                    dto.getSourceKey()
            );
            rules.add(rule);
        }

        return rules;
    }

    private ParsedAttributes normalizeAttributes(String[] attributes, Double[][] providedIntervals) {
        if (attributes == null) {
            return new ParsedAttributes(null, null);
        }

        String[] normalizedValues = new String[attributes.length];
        Double[][] intervals = new Double[attributes.length][];

        for (int index = 0; index < attributes.length; index++) {
            String rawValue = attributes[index] == null ? "" : attributes[index].trim();
            double[] parsedInlineInterval = parseInlineInterval(rawValue);
            Double[] providedInterval = readProvidedInterval(providedIntervals, index);

            if (providedInterval != null) {
                intervals[index] = new Double[] { providedInterval[0], providedInterval[1] };
                if (isNumeric(rawValue)) {
                    normalizedValues[index] = rawValue;
                } else {
                    normalizedValues[index] = toNumericString(providedInterval[0]);
                }
                continue;
            }

            if (parsedInlineInterval != null) {
                intervals[index] = new Double[] { parsedInlineInterval[0], parsedInlineInterval[1] };
                normalizedValues[index] = toNumericString(parsedInlineInterval[0]);
                continue;
            }

            normalizedValues[index] = rawValue;
        }

        return new ParsedAttributes(normalizedValues, hasAnyInterval(intervals) ? intervals : null);
    }

    private Double[] readProvidedInterval(Double[][] providedIntervals, int index) {
        if (providedIntervals == null || index >= providedIntervals.length) {
            return null;
        }

        Double[] candidate = providedIntervals[index];
        if (candidate == null || candidate.length < 2 || candidate[0] == null || candidate[1] == null) {
            return null;
        }

        double min = candidate[0];
        double max = candidate[1];
        if (max < min) {
            double temp = min;
            min = max;
            max = temp;
        }

        return new Double[] { min, max };
    }

    private double[] parseInlineInterval(String value) {
        Matcher matcher = INTERVAL_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }

        double first = Double.parseDouble(matcher.group(1));
        double second = Double.parseDouble(matcher.group(2));
        double min = Math.min(first, second);
        double max = Math.max(first, second);
        return new double[] { min, max };
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String toNumericString(double value) {
        if (Math.floor(value) == value) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }

        return Double.toString(value);
    }

    private boolean hasAnyInterval(Double[][] intervals) {
        for (Double[] interval : intervals) {
            if (interval != null) {
                return true;
            }
        }
        return false;
    }

    private record ParsedAttributes(String[] values, Double[][] intervals) {
    }
}
