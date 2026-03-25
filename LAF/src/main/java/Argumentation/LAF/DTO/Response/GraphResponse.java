package Argumentation.LAF.DTO.Response;

import java.util.List;

/**
 * Data Transfer Object (DTO) that represents the complete argumentation graph
 * returned by the system as a response to client requests.
 *
 * <p>
 * This object groups together the set of nodes and edges that compose the
 * argumentation graph generated according to the Label-Based Argumentation
 * Framework (LAF). It is intended to be serialized and consumed by client
 * applications, such as visualization components or analysis tools.
 * </p>
 *
 * @author JaviDebórtoli
 */
public class GraphResponse {
    /** Collection of nodes that belong to the argumentation graph. */
    private List<GraphNodeResponse> nodes;
    /** Collection of edges that define the relationships between nodes in the argumentation graph. */
    private List<GraphEdgeResponse> edges;

    /**
     * Returns the list of nodes contained in the argumentation graph.
     *
     * @return the list of {@link GraphNodeResponse} elements
     */
    @Deprecated
    public List<GraphNodeResponse> getNodes() {
        return nodes;
    }

    /**
     * Returns the list of edges contained in the argumentation graph.
     *
     * @return the list of {@link GraphEdgeResponse} elements
     */
    @Deprecated
    public List<GraphEdgeResponse> getEdges() {
        return edges;
    }

    /**
     * Sets the list of nodes that compose the argumentation graph.
     *
     * @param nodes the list of {@link GraphNodeResponse} elements
     */
    public void setNodes(List<GraphNodeResponse> nodes) {
        this.nodes = nodes;
    }

    /**
     * Sets the list of edges that define the relationships between nodes
     * in the argumentation graph.
     *
     * @param edges the list of {@link GraphEdgeResponse} elements
     */
    public void setEdges(List<GraphEdgeResponse> edges) {
        this.edges = edges;
    }
}