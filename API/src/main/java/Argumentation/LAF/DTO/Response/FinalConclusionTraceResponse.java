package Argumentation.LAF.DTO.Response;

public class FinalConclusionTraceResponse {
    private String literal;
    private String[] mu;
    private String[] delta;
    private String acceptability;
    private String acceptabilityReason;

    public String getLiteral() {
        return literal;
    }

    public String[] getMu() {
        return mu;
    }

    public String[] getDelta() {
        return delta;
    }

    public String getAcceptability() {
        return acceptability;
    }

    public String getAcceptabilityReason() {
        return acceptabilityReason;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }

    public void setMu(String[] mu) {
        this.mu = mu;
    }

    public void setDelta(String[] delta) {
        this.delta = delta;
    }

    public void setAcceptability(String acceptability) {
        this.acceptability = acceptability;
    }

    public void setAcceptabilityReason(String acceptabilityReason) {
        this.acceptabilityReason = acceptabilityReason;
    }
}
