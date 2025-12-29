/*
 * 
 */
package Argumentation.LAF.DTO.Response;

import java.util.List;

/**
 * @author JaviDebórtoli
 */
public class GraphResponse {
    
    private List<GraphNodeResponse> nodes;
    private List<GraphEdgeResponse> edges;

    public List<GraphNodeResponse> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNodeResponse> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdgeResponse> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdgeResponse> edges) {
        this.edges = edges;
    }
    
}
