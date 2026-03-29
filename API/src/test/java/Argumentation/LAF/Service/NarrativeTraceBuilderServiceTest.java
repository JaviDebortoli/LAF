package Argumentation.LAF.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Argumentation.LAF.DTO.Response.GraphEdgeResponse;
import Argumentation.LAF.DTO.Response.GraphNodeResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import org.junit.jupiter.api.Test;

class NarrativeTraceBuilderServiceTest {
    private final NarrativeTraceBuilderService service = new NarrativeTraceBuilderService();

    @Test
    void shouldExtractFinalConclusionsAndDerivationPaths() {
        GraphResponse graph = new GraphResponse();
        graph.setNodes(java.util.List.of(
                node("F1", "basicServices(houseA)", "FACT", new String[] {"0.75", "0.95"}, new String[] {"0.75", "0.95"}),
                node("F2", "goodArea(houseA)", "FACT", new String[] {"0.80", "0.90"}, new String[] {"0.80", "0.90"}),
                node("F3", "buy(houseA)", "FACT", new String[] {"0.85", "1.0"}, new String[] {"0.70", "0.88"}),
                node("R1", "goodArea(X) :- basicServices(X).", "RULE", new String[] {"0.75", "0.95"}, new String[] {"0.75", "0.95"}),
                node("R2", "buy(X) :- goodArea(X).", "RULE", new String[] {"0.85", "1.0"}, new String[] {"0.85", "1.0"})));

        graph.setEdges(java.util.List.of(
                edge("R1", "F2", "SUPPORT"),
                edge("F1", "F2", "AGGREGATION"),
                edge("R2", "F3", "SUPPORT"),
                edge("F2", "F3", "AGGREGATION")));

        var trace = service.build(graph);

        assertEquals(1, trace.getFinalConclusions().size());
        assertEquals("buy(houseA)", trace.getFinalConclusions().get(0).getLiteral());
        assertEquals("ACCEPTED", trace.getFinalConclusions().get(0).getAcceptability());

        assertEquals(1, trace.getDerivations().size());
        var derivation = trace.getDerivations().get(0);
        assertEquals("buy(houseA)", derivation.getTargetLiteral());
        assertTrue(derivation.getSteps().contains("basicServices(houseA)"));
        assertTrue(derivation.getSteps().contains("goodArea(houseA)"));
        assertTrue(derivation.getSteps().contains("buy(houseA)"));
    }

    @Test
    void shouldNormalizeConflictsAndComputeWinner() {
        GraphResponse graph = new GraphResponse();
        graph.setNodes(java.util.List.of(
                node("F1", "goodArea(houseA)", "FACT", new String[] {"0.8", "0.9"}, new String[] {"0.7", "0.8"}),
                node("F2", "~goodArea(houseA)", "FACT", new String[] {"0.5", "0.8"}, new String[] {"0.4", "0.5"})));
        graph.setEdges(java.util.List.of(
                edge("F1", "F2", "CONFLICT"),
                edge("F2", "F1", "CONFLICT")));

        var trace = service.build(graph);

        assertEquals(1, trace.getConflicts().size());
        var conflict = trace.getConflicts().get(0);
        assertEquals("goodArea(houseA)", conflict.getLeftLiteral());
        assertEquals("~goodArea(houseA)", conflict.getRightLiteral());
        assertEquals("LEFT", conflict.getWinner());
        assertNotNull(conflict.getWinnerReason());
        assertFalse(conflict.getWinnerReason().isBlank());
    }

    private GraphNodeResponse node(String id, String label, String type, String[] mu, String[] delta) {
        GraphNodeResponse node = new GraphNodeResponse();
        node.setId(id);
        node.setLabel(label);
        node.setType(type);
        node.setAttributes(mu);
        node.setDeltaAttributes(delta);
        return node;
    }

    private GraphEdgeResponse edge(String from, String to, String kind) {
        GraphEdgeResponse edge = new GraphEdgeResponse();
        edge.setFrom(from);
        edge.setTo(to);
        edge.setKind(kind);
        return edge;
    }
}
