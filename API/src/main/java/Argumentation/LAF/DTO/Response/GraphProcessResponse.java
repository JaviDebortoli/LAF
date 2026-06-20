package Argumentation.LAF.DTO.Response;

public class GraphProcessResponse {
    private GraphResponse graph;
    private String narrative;
    private NarrativeTraceResponse trace;
    private NarrationMetaResponse meta;
    private ExplainabilityResponse explainability;

    public GraphResponse getGraph() {
        return graph;
    }

    public String getNarrative() {
        return narrative;
    }

    public NarrativeTraceResponse getTrace() {
        return trace;
    }

    public NarrationMetaResponse getMeta() {
        return meta;
    }

    public ExplainabilityResponse getExplainability() {
        return explainability;
    }

    public void setGraph(GraphResponse graph) {
        this.graph = graph;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public void setTrace(NarrativeTraceResponse trace) {
        this.trace = trace;
    }

    public void setMeta(NarrationMetaResponse meta) {
        this.meta = meta;
    }

    public void setExplainability(ExplainabilityResponse explainability) {
        this.explainability = explainability;
    }
}
