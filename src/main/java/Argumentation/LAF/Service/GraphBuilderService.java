package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Response.GraphEdgeResponse;
import Argumentation.LAF.DTO.Response.GraphNodeResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.Domain.ArgumentativeGraph;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.KnowledgePiece;
import Argumentation.LAF.Domain.PairInConflict;
import Argumentation.LAF.Domain.Rule;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Service responsible for constructing the argumentation graph used in the
 * Label-Based Argumentation Framework (LAF). This component takes as input the
 * pieces of knowledge provided by the user (facts, rules and conflict relations)
 * and generates a structured graph with typed nodes and edges that reflects the
 * inferential structure of the argumentative process.
 *
 * <p>
 * The generated graph includes:
 * </p>
 * <ul>
 *     <li><strong>I-nodes (Information Nodes)</strong>: Representing base facts and premises.</li>
 *     <li><strong>RA-nodes (Rule Application Nodes)</strong>: Representing the application of
 *     a rule when its premises are satisfied, linking antecedents with their conclusion.</li>
 *     <li><strong>CA-nodes (Conflict Application Nodes)</strong>: Representing the presence
 *     of inconsistencies, disagreements or contradictory conclusions between arguments.</li>
 * </ul>
 *
 * <p>
 * The resulting graph structure is returned as a {@link GraphResponse}, which
 * contains abstracted DTO representations of nodes and edges for REST exposure.
 * Each node receives a unique identifier and each edge is labeled according to
 * its semantic role in the argumentative structure (AGGREGATION, SUPPORT, CONFLICT).
 * </p>
 *
 * <h3>Main responsibilities</h3>
 * <ul>
 *     <li>Instantiate graph nodes for facts, rules and conflicts.</li>
 *     <li>Create directional links based on inferential relations.</li>
 *     <li>Detect and register bidirectional conflict relations between conclusions.</li>
 *     <li>Export a REST-ready version of the argumentation graph.</li>
 * </ul>
 *
 * <h3>Input and Output</h3>
 * <p><b>Input:</b> {@link ArgumentativeGraph} instance already populated with
 * facts, rules and detected conflicts.</p>
 * <p><b>Output:</b> {@link GraphResponse} containing node and edge DTOs
 * suitable for visualization or further inference steps.</p>
 *
 * @see GraphResponse
 * @see GraphNodeResponse
 * @see GraphEdgeResponse
 * @see ArgumentativeGraph
 *
 * @author JaviDebórtoli
 */
@Service
public class GraphBuilderService {
    
    int factCounter; /** Internal counter used to generate unique identifiers for I-nodes. */
    int ruleCounter; /** Internal counter used to generate unique identifiers for RA-nodes */
    
    /**
     * Translates the internal model representation of the argumentation graph
     * into a serializable structure ({@link GraphResponse}) suitable for REST
     * communication. This transformation converts each instantiated node and edge
     * from the logical graph into its corresponding DTO representation.
     *
     * <p>
     * The resulting {@code GraphResponse} aggregates:
     * </p>
     * <ul>
     *     <li>A collection of {@link GraphNodeResponse} elements representing I-nodes,
     *     RA-nodes and CA-nodes.</li>
     *     <li>A collection of {@link GraphEdgeResponse} elements capturing support,
     *     aggregation and conflict relations between nodes.</li>
     * </ul>
     *
     * <p>
     * This method ensures that the exported graph maintains the semantic integrity
     * of the LAF structure, preserving the inferential and conflict relationships
     * described in the underlying argumentation model.
     * </p> 
     * 
     * @param graph the already constructed graph instance containing
     *              facts, rule applications and conflicts.
     * @return      a {@link GraphResponse} DTO representing the argumentation graph in a
     *              format consumable by clients or visualization components.
     */
    public GraphResponse toGraphResponse(ArgumentativeGraph graph) {
                
        GraphResponse response = new GraphResponse();
        List<GraphNodeResponse> nodeDtos = new ArrayList<>();
        List<GraphEdgeResponse> edgeDtos = new ArrayList<>();

        factCounter = 1;
        ruleCounter = 1;
        
        Map<KnowledgePiece, String> idMap = new HashMap<>();

        // Recolectar todos los nodos que participan en el grafo
        Set<KnowledgePiece> allNodes = new LinkedHashSet<>();

        // Claves del mapa (pueden ser Rules o Facts)
        allNodes.addAll(graph.edges().keySet());
        
        // Valores del mapa (siempre Facts)
        for (List<Fact> children : graph.edges().values()) {
            allNodes.addAll(children);
        }
        
        // Nodos en conflicto
        for (PairInConflict pair : graph.conflictiveNodes()) {
            allNodes.add(pair.first());
            allNodes.add(pair.second());
        }

        // Crear DTOs de nodos
        for (KnowledgePiece kp : allNodes) {
            GraphNodeResponse nodeDto = new GraphNodeResponse();

            if (kp instanceof Fact fact) {
                String id = idMap.computeIfAbsent(kp, k -> "F" + (factCounter++));
                nodeDto.setId(id);
                nodeDto.setLabel(fact.getName() + "(" + fact.getArgument() + ")");
                nodeDto.setType("FACT");
                nodeDto.setAttributes(fact.getAttributes());
                nodeDto.setDeltaAttributes(fact.getDeltaAttributes());

            } else if (kp instanceof Rule rule) {
                String id = idMap.computeIfAbsent(kp, k -> "R" + (ruleCounter++));
                nodeDto.setId(id);
                nodeDto.setLabel(rule.toString());
                nodeDto.setType("RULE");
                nodeDto.setAttributes(rule.getAttributes());
                nodeDto.setDeltaAttributes(rule.getDeltaAttributes());
            }

            nodeDtos.add(nodeDto);
        }

        
        // Mapa hijo -> lista de padres (KnowledgePiece)
        Map<Fact, List<KnowledgePiece>> parentsMap = new HashMap<>();

        for (Map.Entry<KnowledgePiece, List<Fact>> entry : graph.edges().entrySet()) {
            KnowledgePiece parent = entry.getKey();
            for (Fact child : entry.getValue()) {
                parentsMap
                    .computeIfAbsent(child, k -> new ArrayList<>())
                    .add(parent);
            }
        }
        
        // Crear aristas de soporte/derivación a partir de edges()
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : graph.edges().entrySet()) {
            KnowledgePiece fromKp = entry.getKey();
            String fromId = idMap.get(fromKp);
            if (fromId == null) {
                continue;
            }

            for (Fact toFact : entry.getValue()) {
                String toId = idMap.get(toFact);
                if (toId == null) {
                    continue;
                }

                GraphEdgeResponse edgeDto = new GraphEdgeResponse();
                edgeDto.setFrom(fromId);
                edgeDto.setTo(toId);

                // ==== NUEVO: decidir tipo de arista según TODOS los padres del hijo ====
                List<KnowledgePiece> parents = parentsMap.getOrDefault(toFact, List.of());
                boolean hasRuleParent = parents.stream().anyMatch(p -> p instanceof Rule);
                boolean allParentsFacts = !parents.isEmpty()
                        && parents.stream().allMatch(p -> p instanceof Fact);

                if (hasRuleParent) {
                    // Si hay al menos una regla padre, TODAS las aristas padre->hijo son SUPPORT
                    edgeDto.setKind("SUPPORT");
                } else if (allParentsFacts) {
                    // Sólo cuando todos los padres son hechos es una AGGREGATION
                    edgeDto.setKind("AGGREGATION");
                } else {
                    // Caso raro / defensivo: por defecto SUPPORT
                    edgeDto.setKind("SUPPORT");
                }
                // =====================================================

                edgeDtos.add(edgeDto);
            }
        }
        
        // Crear aristas de conflicto a partir de conflictiveNodes()
        for (PairInConflict pair : graph.conflictiveNodes()) {
            Fact f1 = pair.first();
            Fact f2 = pair.second();
            String id1 = idMap.get(f1);
            String id2 = idMap.get(f2);
            if (id1 == null || id2 == null) continue;

            GraphEdgeResponse e1 = new GraphEdgeResponse();
            e1.setFrom(id1);
            e1.setTo(id2);
            e1.setKind("CONFLICT");
            edgeDtos.add(e1);

            GraphEdgeResponse e2 = new GraphEdgeResponse();
            e2.setFrom(id2);
            e2.setTo(id1);
            e2.setKind("CONFLICT");
            edgeDtos.add(e2);
        }

        response.setNodes(nodeDtos);
        response.setEdges(edgeDtos);
        return response;
    }
}