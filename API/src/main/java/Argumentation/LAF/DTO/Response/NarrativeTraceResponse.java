package Argumentation.LAF.DTO.Response;

import java.util.List;

public class NarrativeTraceResponse {
    private List<FinalConclusionTraceResponse> finalConclusions;
    private List<DerivationTraceResponse> derivations;
    private List<ConflictTraceResponse> conflicts;

    public List<FinalConclusionTraceResponse> getFinalConclusions() {
        return finalConclusions;
    }

    public List<DerivationTraceResponse> getDerivations() {
        return derivations;
    }

    public List<ConflictTraceResponse> getConflicts() {
        return conflicts;
    }

    public void setFinalConclusions(List<FinalConclusionTraceResponse> finalConclusions) {
        this.finalConclusions = finalConclusions;
    }

    public void setDerivations(List<DerivationTraceResponse> derivations) {
        this.derivations = derivations;
    }

    public void setConflicts(List<ConflictTraceResponse> conflicts) {
        this.conflicts = conflicts;
    }
}
