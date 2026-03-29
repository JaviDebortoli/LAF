package Argumentation.LAF.DTO.Response;

import java.util.List;

public class DerivationTraceResponse {
    private String targetLiteral;
    private List<String> steps;
    private List<String> edgeKinds;

    public String getTargetLiteral() {
        return targetLiteral;
    }

    public List<String> getSteps() {
        return steps;
    }

    public List<String> getEdgeKinds() {
        return edgeKinds;
    }

    public void setTargetLiteral(String targetLiteral) {
        this.targetLiteral = targetLiteral;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public void setEdgeKinds(List<String> edgeKinds) {
        this.edgeKinds = edgeKinds;
    }
}
