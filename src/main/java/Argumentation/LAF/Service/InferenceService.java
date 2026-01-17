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
 * Service responsible for building an {@link ArgumentativeGraph} from a set of
 * facts, rules and algebraic operation definitions.
 *
 * <p>
 * This service acts as an application-level entry point for the inference
 * process. It coordinates the transformation of the provided domain elements
 * into an {@link ArgumentativeGraph} structure by delegating the core inference
 * logic to the underlying inference mechanisms.
 * </p>
 *
 * <p>
 * The resulting graph represents the inferential relationships between facts
 * and rules, enriched with the algebraic operations defined by the associated
 * {@link OperationSet}s, in accordance with the Label-Based Argumentation
 * Framework (LAF).
 * </p>
 *
 * @see ArgumentativeGraph
 * @see Fact
 * @see Rule
 * @see OperationSet
 *
 * @author JaviDebórtoli
 */
@Service
public class InferenceService {
    /**
     * Builds an {@link ArgumentativeGraph} from the given facts, rules and
     * operation sets.
     *
     * <p>
     * This method takes the initial knowledge base composed of {@link Fact}s
     * and {@link Rule}s, together with a collection of {@link OperationSet}s
     * that define the algebraic operations to be applied during inference,
     * and constructs the corresponding argumentative graph.
     * </p>
     *
     * <p>
     * Each {@link OperationSet} specifies the expressions used for support,
     * aggregation and conflict handling, which are incorporated into the
     * resulting graph structure to enable subsequent evaluation and labeling.
     * </p>
     *
     * @param facts the list of initial facts that constitute the base
     *              knowledge of the argumentation system
     * @param rules the list of inference rules used to derive new conclusions
     *              from the given facts
     * @param operations a mapping between operation identifiers and their
     *                   corresponding {@link OperationSet} definitions
     * @return an {@link ArgumentativeGraph} representing the inferential
     *         structure derived from the provided facts, rules and operations
     */
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