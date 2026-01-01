/*
 * 
 */
package Argumentation.LAF.Service;

import Argumentation.LAF.Domain.ArgumentativeGraph;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.InferenceEngine;
import Argumentation.LAF.Domain.OperationSet;
import Argumentation.LAF.Domain.Rule;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author JaviDebórtoli
 */
@Service
public class InferenceService {
    
    public ArgumentativeGraph buildGraph(List<Fact> facts, List<Rule> rules, Map<String, OperationSet> operations) {
        
        if (operations == null || operations.isEmpty()) {
            throw new IllegalStateException("Missing functions");
        }
        
        String[][] functions = new String[operations.size()][3];
        int i = 0;
        for (OperationSet set : operations.values()) {
            functions[i][0] = set.getSupportExpr();
            functions[i][1] = set.getAggregationExpr();
            functions[i][2] = set.getConflictExpr();
            i++;
        }

        InferenceEngine engine = new InferenceEngine(facts, rules, functions);
        return engine.buildTree();
    }
    
}
