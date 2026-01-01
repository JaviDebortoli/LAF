package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.Response.GraphEdgeResponse;
import Argumentation.LAF.DTO.Response.GraphNodeResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.Domain.ArgumentativeGraph;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.KnowledgePiece;
import Argumentation.LAF.Domain.Pair;
import Argumentation.LAF.Domain.Rule;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GraphBuilderService {
    
    int factCounter;
    int ruleCounter;
    
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
        for (Pair pair : graph.conflictiveNodes()) {
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
        for (Pair pair : graph.conflictiveNodes()) {
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