/*
 * 
 */
package Argumentation.LAF.DTO.Response;

/**
 * @author JaviDebórtoli
 */
public class GraphEdgeResponse {
    
    private String from;
    private String to;
    private String kind; // "SUPPORT", "AGGREGATION", "CONFLICT"

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
    
}
