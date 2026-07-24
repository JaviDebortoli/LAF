package Argumentation.LAF.Domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import Argumentation.LAF.Service.InferenceService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InferenceEngineSymbolicOperatorsTest {

    // ---- Soporte ----

    @Test
    void unionInSupportStillMergesSingleTokenLabels() {
        // Regresion: esta combinacion ya funcionaba antes del refactor.
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "red" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "blue" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Union", "Union", "Intersection"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(basicServices)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, "red blue");
    }

    @Test
    void intersectionInSupportKeepsCommonLabels() {
        // Combinacion nueva: antes solo Union estaba disponible en Soporte.
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "red blue" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "blue green" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Intersection", "Union", "Intersection"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(basicServices)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, "blue");
    }

    @Test
    void differenceInSupportRemovesLabels() {
        // Combinacion nueva: Difference nunca existio en ningun lado.
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "red blue" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "blue" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Difference", "Union", "Intersection"));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(basicServices)), List.of(goodAreaRule), operations);

        assertGoodAreaAttributeEquals(graph, "red");
    }

    // ---- Agregacion ----

    @Test
    void unionInAggregationStillMergesLabels() {
        // Regresion: esta combinacion ya funcionaba antes del refactor.
        assertAggregatedGoodAreaEquals("Union", "blue green red");
    }

    @Test
    void intersectionInAggregationKeepsCommonLabels() {
        // Combinacion nueva: antes solo Union estaba disponible en Agregacion.
        assertAggregatedGoodAreaEquals("Intersection", "blue");
    }

    @Test
    void differenceInAggregationRemovesLabels() {
        // Combinacion nueva.
        assertAggregatedGoodAreaEquals("Difference", "green");
    }

    // ---- Conflicto ----

    @Test
    void intersectionInConflictStillKeepsCommonLabels() {
        // Regresion: esta combinacion ya funcionaba antes del refactor.
        assertConflictDeltas("Intersection", "blue", "blue");
    }

    @Test
    void unionInConflictMergesLabels() {
        // Combinacion nueva: antes solo Intersection estaba disponible en Conflicto.
        assertConflictDeltas("Union", "blue green red", "red blue green");
    }

    @Test
    void differenceInConflictRemovesLabels() {
        // Combinacion nueva.
        assertConflictDeltas("Difference", "green", "red");
    }

    // ---- Error handling ----

    @Test
    void unrecognizedSymbolicOperatorThrows() {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "red" });
        Rule goodAreaRule = new Rule("goodArea", List.of("basicServices"), new String[] { "blue" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Xor", "Union", "Intersection"));

        List<Fact> facts = new ArrayList<>(List.of(basicServices));
        List<Rule> rules = List.of(goodAreaRule);

        assertThrows(IllegalArgumentException.class,
                () -> inferenceService.buildGraph(facts, rules, operations));
    }

    // ---- Helpers ----

    private void assertAggregatedGoodAreaEquals(String aggregationExpr, String expected) {
        InferenceService inferenceService = new InferenceService();

        Fact basicServices = new Fact("basicServices", "houseA", new String[] { "red blue" });
        Fact goodNeighbors = new Fact("goodNeighbors", "houseA", new String[] { "blue green" });
        Rule fromBasicServices = new Rule("goodArea", List.of("basicServices"), new String[] { "red blue" });
        Rule fromGoodNeighbors = new Rule("goodArea", List.of("goodNeighbors"), new String[] { "blue green" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Union", aggregationExpr, "Union"));

        List<Fact> facts = new ArrayList<>(List.of(basicServices, goodNeighbors));
        List<Rule> rules = List.of(fromBasicServices, fromGoodNeighbors);

        ArgumentativeGraph graph = inferenceService.buildGraph(facts, rules, operations);

        assertGoodAreaAttributeEquals(graph, expected);
    }

    private void assertConflictDeltas(String conflictExpr, String expectedNegativeDelta, String expectedPositiveDelta) {
        InferenceService inferenceService = new InferenceService();

        Fact positive = new Fact("p", "a", new String[] { "red blue" });
        Fact negative = new Fact("~p", "a", new String[] { "blue green" });

        Map<String, OperationSet> operations = Map.of(
                "label_1", new OperationSet("Union", "Union", conflictExpr));

        ArgumentativeGraph graph = inferenceService.buildGraph(
                new ArrayList<>(List.of(positive, negative)), List.of(), operations);

        assertEquals(1, graph.conflictiveNodes().size());
        PairInConflict pair = graph.conflictiveNodes().get(0);

        boolean firstIsNegative = "~p".equals(pair.first().getName());
        Fact negativeResult = firstIsNegative ? pair.first() : pair.second();
        Fact positiveResult = firstIsNegative ? pair.second() : pair.first();

        assertEquals(expectedNegativeDelta, negativeResult.getDeltaAttributes()[0]);
        assertEquals(expectedPositiveDelta, positiveResult.getDeltaAttributes()[0]);
    }

    private void assertGoodAreaAttributeEquals(ArgumentativeGraph graph, String expected) {
        // Tras una agregacion, el grafo interno puede contener objetos Fact intermedios
        // (superados) ademas del definitivo, todos con el mismo nombre/argumento pero
        // ubicados como VALUES en distintas entradas de edges(). El hecho definitivo es
        // el unico que nunca aparece tambien como KEY (nada lo supera mas adelante).
        Set<KnowledgePiece> supersededKeys = new HashSet<>(graph.edges().keySet());

        String actual = graph.edges().values().stream()
                .flatMap(List::stream)
                .filter(fact -> "goodArea".equals(fact.getName()) && "houseA".equals(fact.getArgument()))
                .filter(fact -> !supersededKeys.contains(fact))
                .findFirst()
                .map(fact -> fact.getAttributes()[0])
                .orElseThrow(() -> new AssertionError("goodArea(houseA) was not derived"));

        assertEquals(expected, actual);
    }
}
