package Argumentation.LAF.Domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Argumentation.LAF.Service.InferenceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InferenceEngineExpressionFunctionsTest {

    @Test
    void maxUsedInSupportPicksLargerValue() {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "0.3" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "0.9" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("max(X,Y)", "X + Y", "X - Y"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(basicServices)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, 0.9);
    }

    @Test
    void minUsedInSupportPicksSmallerValue() {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "0.3" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "0.9" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("min(X,Y)", "X + Y", "X - Y"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(basicServices)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, 0.3);
    }

    @Test
    void multiPremiseAdditionSupportUnaffectedByFoldSeedChange() {
        InferenceService inferenceService = new InferenceService();

        Fact factOne = new Fact("factOne", "houseA", new String[] { "0.2" });
        Fact factTwo = new Fact("factTwo", "houseA", new String[] { "0.3" });
        Rule goodAreaRule = new Rule("goodArea", List.of("factOne", "factTwo"), new String[] { "0.1" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("X + Y", "X + Y", "X - Y"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(factOne, factTwo)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, 0.6);
    }

    @Test
    void minUsedInAggregationPicksSmallerValue() {
        assertAggregatedGoodAreaEquals("min(X,Y)", 0.3);
    }

    @Test
    void minUsedInAggregationOfThreeFactsPicksSmallestValue() {
        InferenceService inferenceService = new InferenceService();

        Fact factA = new Fact("factA", "houseA", new String[] { "0.9" });
        Fact factB = new Fact("factB", "houseA", new String[] { "0.3" });
        Fact factC = new Fact("factC", "houseA", new String[] { "0.6" });
        Rule ruleFromA = new Rule("goodArea", List.of("factA"), new String[] { "0.0" });
        Rule ruleFromB = new Rule("goodArea", List.of("factB"), new String[] { "0.0" });
        Rule ruleFromC = new Rule("goodArea", List.of("factC"), new String[] { "0.0" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("X + Y", "min(X,Y)", "X - Y"));

        List<Fact> facts = new ArrayList<>(List.of(factA, factB, factC));
        List<Rule> rules = List.of(ruleFromA, ruleFromB, ruleFromC);

        ArgumentativeGraph graph = inferenceService.buildGraph(facts, rules, operations);

        assertGoodAreaAttributeEquals(graph, 0.3);
    }

    @Test
    void maxUsedInAggregationPicksLargerValue() {
        assertAggregatedGoodAreaEquals("max(X,Y)", 0.9);
    }

    @Test
    void minMaxUsedInConflictExpression() {
        InferenceService inferenceService = new InferenceService();

        Fact positive = new Fact("p", "a", new String[] { "0.7" });
        Fact negative = new Fact("~p", "a", new String[] { "0.2" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("X + Y", "X + Y", "max(X-Y,0)"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(positive, negative)), List.of(), operations);

        assertEquals(1, graph.conflictiveNodes().size());
        PairInConflict pair = graph.conflictiveNodes().get(0);

        boolean firstIsNegative = "~p".equals(pair.first().getName());
        Fact negativeResult = firstIsNegative ? pair.first() : pair.second();
        Fact positiveResult = firstIsNegative ? pair.second() : pair.first();

        assertEquals(0.0, Double.parseDouble(negativeResult.getDeltaAttributes()[0]), 0.000001);
        assertEquals(0.5, Double.parseDouble(positiveResult.getDeltaAttributes()[0]), 0.000001);
    }

    @Test
    void unknownFunctionInExpressionThrowsInsteadOfSilentlyDefaulting() {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "0.3" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "0.9" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("bogusFn(X,Y)", "X + Y", "X - Y"));

        List<Fact> facts = new ArrayList<>(List.of(basicServices));
        List<Rule> rules = List.of(goodAreaRule);

        assertThrows(IllegalArgumentException.class,
                () -> inferenceService.buildGraph(facts, rules, operations));
    }

    @Test
    void minWithWrongArityThrows() {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "0.3" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "0.9" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("min(X)", "X + Y", "X - Y"));

        List<Fact> facts = new ArrayList<>(List.of(basicServices));
        List<Rule> rules = List.of(goodAreaRule);

        assertThrows(IllegalArgumentException.class,
                () -> inferenceService.buildGraph(facts, rules, operations));
    }

    private void assertAggregatedGoodAreaEquals(String aggregationExpr, double expected) {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "0.3" });
        Fact goodNeighbors = new Fact("goodNeighbors", "houseA", new String[] { "0.9" });
        Rule fromBasicServices = new Rule("goodArea", List.of("basicServices"), new String[] { "0.0" });
        Rule fromGoodNeighbors = new Rule("goodArea", List.of("goodNeighbors"), new String[] { "0.0" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("X + Y", aggregationExpr, "X - Y"));

        List<Fact> facts = new ArrayList<>(List.of(basicServices, goodNeighbors));
        List<Rule> rules = List.of(fromBasicServices, fromGoodNeighbors);

        ArgumentativeGraph graph = inferenceService.buildGraph(facts, rules, operations);

        assertGoodAreaAttributeEquals(graph, expected);
    }

    private void assertGoodAreaAttributeEquals(ArgumentativeGraph graph, double expected) {
        boolean found = graph.edges().values().stream()
                .flatMap(List::stream)
                .anyMatch(fact -> "goodArea".equals(fact.getName())
                        && "houseA".equals(fact.getArgument())
                        && Math.abs(Double.parseDouble(fact.getAttributes()[0]) - expected) < 0.000001);

        assertTrue(found, "Expected goodArea(houseA) to equal " + expected);
    }
}
