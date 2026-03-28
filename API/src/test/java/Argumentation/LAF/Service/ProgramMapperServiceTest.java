package Argumentation.LAF.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.OperationSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProgramMapperServiceTest {

    @Test
    void mapFactsParsesInlineIntervalsUsingLowerBound() {
        ProgramMapperService mapper = new ProgramMapperService();

        FactDTO dto = new FactDTO();
        dto.setName("basicServices");
        dto.setArgument("houseA");
        dto.setAttributes(new String[] { "[0.5, 1.0]", "high" });
        dto.setSourceKey("FACT|basicServices|houseA");

        List<Fact> mappedFacts = mapper.mapFacts(List.of(dto));

        assertEquals(1, mappedFacts.size());
        Fact mappedFact = mappedFacts.getFirst();
        assertEquals("0.5", mappedFact.getAttributes()[0]);
        assertEquals("high", mappedFact.getAttributes()[1]);
        assertNotNull(mappedFact.getAttributeIntervals());
        assertEquals(0.5, mappedFact.getAttributeIntervals()[0][0], 0.000001);
        assertEquals(1.0, mappedFact.getAttributeIntervals()[0][1], 0.000001);
        assertEquals("FACT|basicServices|houseA", mappedFact.getSourceKey());
    }

    @Test
    void inferenceUsesLowerBoundFromIntervalAsInitialValue() {
        ProgramMapperService mapper = new ProgramMapperService();
        InferenceService inferenceService = new InferenceService();

        FactDTO factDto = new FactDTO();
        factDto.setName("basicServices");
        factDto.setArgument("houseA");
        factDto.setAttributes(new String[] { "[0.5, 1.0]" });

        RuleDTO ruleDto = new RuleDTO();
        ruleDto.setHeadName("goodArea");
        ruleDto.setBodyLiterals(List.of("basicServices"));
        ruleDto.setAttributes(new String[] { "0.4" });

        List<Fact> facts = mapper.mapFacts(List.of(factDto));
        var rules = mapper.mapRules(List.of(ruleDto));

        Map<String, OperationSet> operations = Map.of(
                "label_1",
                new OperationSet("X + Y", "X + Y", "X - Y"));

        var graph = inferenceService.buildGraph(facts, rules, operations);

        boolean foundDerivedFact = graph.edges().values().stream()
                .flatMap(List::stream)
                .anyMatch(fact -> "goodArea".equals(fact.getName())
                        && "houseA".equals(fact.getArgument())
                        && Math.abs(Double.parseDouble(fact.getAttributes()[0]) - 0.9) < 0.000001);

        assertTrue(foundDerivedFact);
    }
}
