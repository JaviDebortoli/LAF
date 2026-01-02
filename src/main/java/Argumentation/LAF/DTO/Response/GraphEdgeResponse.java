package Argumentation.LAF.DTO.Response;

/**
 * Data Transfer Object (DTO) that represents a directed edge in the
 * argumentation graph returned by the LAF backend.
 *
 * <p>
 * Each edge connects two graph nodes and is annotated with a semantic
 * type that describes the role of the relation between them, such as
 * support, aggregation or conflict.
 * </p>
 *
 * <p>
 * This DTO is intended exclusively for response serialization and
 * visualization purposes and does not contain inference logic.
 * </p>
 *
 * @author JaviDebórtoli
 */
public class GraphEdgeResponse {
    /** Identifier of the source node of the edge. */
    private String from;
    /** Identifier of the target node of the edge. */
    private String to;
    /**
     * Semantic type of the edge.
     * Possible values include:
     * <ul>
     *     <li>{@code SUPPORT}</li>
     *     <li>{@code AGGREGATION}</li>
     *     <li>{@code CONFLICT}</li>
     * </ul>
     */
    private String kind;

    /**
     * Returns the identifier of the source node.
     *
     * @return the source node identifier
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the identifier of the target node.
     *
     * @return the target node identifier
     */
    public String getTo() {
        return to;
    }

    /**
     * Returns the semantic type of the edge.
     *
     * @return the edge type
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the identifier of the source node.
     *
     * @param from the source node identifier
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Sets the identifier of the target node.
     *
     * @param to the target node identifier
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Sets the semantic type of the edge.
     *
     * @param kind the edge type (e.g. SUPPORT, AGGREGATION, CONFLICT)
     */
    public void setKind(String kind) {
        this.kind = kind;
    }
}