package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Response.ConflictTraceResponse;
import Argumentation.LAF.DTO.Response.DerivationTraceResponse;
import Argumentation.LAF.DTO.Response.FinalConclusionTraceResponse;
import Argumentation.LAF.DTO.Response.GraphEdgeResponse;
import Argumentation.LAF.DTO.Response.GraphNodeResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("deprecation")
public class NarrativeTraceBuilderService {
    public NarrativeTraceResponse build(GraphResponse graph) {
        List<GraphNodeResponse> nodes = graph.getNodes() == null ? List.of() : graph.getNodes();
        List<GraphEdgeResponse> edges = graph.getEdges() == null ? List.of() : graph.getEdges();

        Map<String, GraphNodeResponse> nodeById = new HashMap<>();
        for (GraphNodeResponse node : nodes) {
            nodeById.put(node.getId(), node);
        }

        Map<String, List<GraphEdgeResponse>> incomingInference = new HashMap<>();
        Set<String> factUsedInInference = new LinkedHashSet<>();

        for (GraphEdgeResponse edge : edges) {
            if (!isInferenceEdge(edge.getKind())) {
                continue;
            }

            incomingInference.computeIfAbsent(edge.getTo(), ignored -> new ArrayList<>()).add(edge);
            GraphNodeResponse source = nodeById.get(edge.getFrom());
            if (source != null && "FACT".equals(source.getType())) {
                factUsedInInference.add(source.getId());
            }
        }

        List<GraphNodeResponse> finalFacts = nodes.stream()
                .filter(node -> "FACT".equals(node.getType()))
                .filter(node -> !factUsedInInference.contains(node.getId()))
                .sorted(Comparator.comparing(GraphNodeResponse::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<GraphNodeResponse> collapsedFinalFacts = collapseFinalFactsByLiteral(finalFacts);

        List<FinalConclusionTraceResponse> finalConclusions = new ArrayList<>();
        List<DerivationTraceResponse> derivations = new ArrayList<>();
        for (GraphNodeResponse finalFact : collapsedFinalFacts) {
            finalConclusions.add(toFinalConclusion(finalFact));
            derivations.add(buildDerivation(finalFact, incomingInference, nodeById));
        }

        List<ConflictTraceResponse> conflicts = buildConflicts(edges, nodeById);

        NarrativeTraceResponse response = new NarrativeTraceResponse();
        response.setFinalConclusions(finalConclusions);
        response.setDerivations(derivations);
        response.setConflicts(conflicts);
        return response;
    }

    private boolean isInferenceEdge(String kind) {
        return "SUPPORT".equals(kind) || "AGGREGATION".equals(kind);
    }

    private List<GraphNodeResponse> collapseFinalFactsByLiteral(List<GraphNodeResponse> finalFacts) {
        Map<String, GraphNodeResponse> representativeByLiteral = new HashMap<>();

        for (GraphNodeResponse candidate : finalFacts) {
            String literal = Objects.toString(candidate.getLabel(), "");
            GraphNodeResponse currentRepresentative = representativeByLiteral.get(literal);
            if (currentRepresentative == null || isBetterRepresentative(candidate, currentRepresentative)) {
                representativeByLiteral.put(literal, candidate);
            }
        }

        return representativeByLiteral.values().stream()
                .sorted(Comparator
                        .comparing((GraphNodeResponse node) -> safeLowerCase(node.getLabel()))
                        .thenComparing(node -> safeLowerCase(node.getId()))
                        .thenComparing(node -> Objects.toString(node.getId(), "")))
                .toList();
    }

    private boolean isBetterRepresentative(GraphNodeResponse candidate, GraphNodeResponse current) {
        int deltaScoreComparison = Double.compare(
                numericDeltaSum(candidate.getDeltaAttributes()),
                numericDeltaSum(current.getDeltaAttributes()));
        if (deltaScoreComparison != 0) {
            return deltaScoreComparison > 0;
        }

        int deltaSignatureComparison = safeLowerCase(compareSignature(candidate.getDeltaAttributes()))
                .compareTo(safeLowerCase(compareSignature(current.getDeltaAttributes())));
        if (deltaSignatureComparison != 0) {
            return deltaSignatureComparison < 0;
        }

        int idComparison = safeLowerCase(candidate.getId()).compareTo(safeLowerCase(current.getId()));
        if (idComparison != 0) {
            return idComparison < 0;
        }

        return Objects.toString(candidate.getId(), "").compareTo(Objects.toString(current.getId(), "")) < 0;
    }

    private String compareSignature(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return String.join("|", values);
    }

    private String safeLowerCase(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT);
    }

    private FinalConclusionTraceResponse toFinalConclusion(GraphNodeResponse node) {
        FinalConclusionTraceResponse response = new FinalConclusionTraceResponse();
        response.setLiteral(node.getLabel());
        response.setMu(node.getAttributes());
        response.setDelta(node.getDeltaAttributes());

        Acceptability acceptability = computeAcceptability(node.getDeltaAttributes());
        response.setAcceptability(acceptability.status());
        response.setAcceptabilityReason(acceptability.reason());
        return response;
    }

    private Acceptability computeAcceptability(String[] delta) {
        if (delta == null || delta.length == 0) {
            return new Acceptability("UNDETERMINED", "No delta labels are available.");
        }

        List<Double> numeric = new ArrayList<>();
        for (String value : delta) {
            Double parsed = parseNumeric(value);
            if (parsed != null) {
                numeric.add(parsed);
            }
        }

        if (numeric.isEmpty()) {
            return new Acceptability("UNDETERMINED", "Delta labels are qualitative for this conclusion.");
        }

        boolean allZero = numeric.stream().allMatch(v -> v == 0.0);
        if (allZero) {
            return new Acceptability(
                    "DEFEATED",
                    "All numeric delta labels are 0.0, so the conclusion is defeated by conflict outcome.");
        }

        return new Acceptability(
                "ADMISSIBLE",
                "At least one numeric delta label is greater than 0.0, so the conclusion remains admissible.");
    }

    private DerivationTraceResponse buildDerivation(
            GraphNodeResponse target,
            Map<String, List<GraphEdgeResponse>> incomingInference,
            Map<String, GraphNodeResponse> nodeById) {
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        LinkedHashSet<String> edgeKinds = new LinkedHashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();

        traverseDerivation(target.getId(), incomingInference, nodeById, steps, edgeKinds, stack);

        DerivationTraceResponse response = new DerivationTraceResponse();
        response.setTargetLiteral(target.getLabel());
        response.setSteps(new ArrayList<>(steps));
        response.setEdgeKinds(new ArrayList<>(edgeKinds));
        return response;
    }

    private void traverseDerivation(
            String nodeId,
            Map<String, List<GraphEdgeResponse>> incomingInference,
            Map<String, GraphNodeResponse> nodeById,
            LinkedHashSet<String> steps,
            LinkedHashSet<String> edgeKinds,
            ArrayDeque<String> stack) {
        if (stack.contains(nodeId)) {
            return;
        }

        stack.push(nodeId);
        List<GraphEdgeResponse> incoming = incomingInference.getOrDefault(nodeId, List.of()).stream()
                .sorted(Comparator
                        .comparing((GraphEdgeResponse edge) -> labelOf(nodeById, edge.getFrom()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GraphEdgeResponse::getKind, String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (GraphEdgeResponse edge : incoming) {
            traverseDerivation(edge.getFrom(), incomingInference, nodeById, steps, edgeKinds, stack);
            edgeKinds.add(edge.getKind());
        }

        GraphNodeResponse node = nodeById.get(nodeId);
        if (node != null) {
            steps.add(node.getLabel());
        }

        stack.pop();
    }

    private String labelOf(Map<String, GraphNodeResponse> nodeById, String nodeId) {
        GraphNodeResponse node = nodeById.get(nodeId);
        return node == null || node.getLabel() == null ? "" : node.getLabel();
    }

    private List<ConflictTraceResponse> buildConflicts(
            List<GraphEdgeResponse> edges,
            Map<String, GraphNodeResponse> nodeById) {
        List<ConflictTraceResponse> conflicts = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();

        for (GraphEdgeResponse edge : edges) {
            if (!"CONFLICT".equals(edge.getKind())) {
                continue;
            }

            GraphNodeResponse left = nodeById.get(edge.getFrom());
            GraphNodeResponse right = nodeById.get(edge.getTo());
            if (left == null || right == null) {
                continue;
            }

            String pairKey = normalizePair(left.getLabel(), right.getLabel());
            if (!seenPairs.add(pairKey)) {
                continue;
            }

            ConflictDecision decision = decideWinner(left, right);

            ConflictTraceResponse response = new ConflictTraceResponse();
            response.setLeftLiteral(left.getLabel());
            response.setRightLiteral(right.getLabel());
            response.setLeftDelta(left.getDeltaAttributes());
            response.setRightDelta(right.getDeltaAttributes());
            response.setWinner(decision.winner());
            response.setWinnerReason(decision.reason());
            conflicts.add(response);
        }

        conflicts.sort(Comparator
                .comparing(ConflictTraceResponse::getLeftLiteral, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ConflictTraceResponse::getRightLiteral, String.CASE_INSENSITIVE_ORDER));
        return conflicts;
    }

    private String normalizePair(String left, String right) {
        if (left.compareToIgnoreCase(right) <= 0) {
            return left + "|" + right;
        }
        return right + "|" + left;
    }

    private ConflictDecision decideWinner(GraphNodeResponse left, GraphNodeResponse right) {
        double leftScore = numericDeltaSum(left.getDeltaAttributes());
        double rightScore = numericDeltaSum(right.getDeltaAttributes());

        int cmp = Double.compare(leftScore, rightScore);
        if (cmp > 0) {
            return new ConflictDecision("LEFT", "Left literal has a stronger numeric delta profile.");
        }
        if (cmp < 0) {
            return new ConflictDecision("RIGHT", "Right literal has a stronger numeric delta profile.");
        }

        int lex = left.getLabel().compareToIgnoreCase(right.getLabel());
        if (lex < 0) {
            return new ConflictDecision("LEFT", "Tie resolved lexicographically for deterministic output.");
        }
        if (lex > 0) {
            return new ConflictDecision("RIGHT", "Tie resolved lexicographically for deterministic output.");
        }
        return new ConflictDecision("DRAW", "Conflict remains tied under deterministic comparison rules.");
    }

    private double numericDeltaSum(String[] delta) {
        if (delta == null) {
            return 0.0;
        }

        double sum = 0.0;
        for (String value : delta) {
            Double parsed = parseNumeric(value);
            if (parsed != null) {
                sum += parsed;
            }
        }
        return sum;
    }

    private Double parseNumeric(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record Acceptability(String status, String reason) {
    }

    private record ConflictDecision(String winner, String reason) {
    }
}
