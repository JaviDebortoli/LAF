package Argumentation.LAF.Domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Executes the inferential process over a knowledge program composed of
 * user-defined facts and rules, applying the label algebra (support,
 * aggregation and conflict) defined for each label.
 * <p>
 * This class is the core of the reasoning engine: it builds new facts
 * whenever a rule is activated, propagating labels accordingly.
 * </p>
 *
 * <p>
 * The inference flow executed by {@link #run(Map, List, List)} is:
 * </p>
 * <ol>
 *   <li>Load initial facts</li>
 *   <li>Iteratively activate rules when their body predicates are present</li>
 *   <li>Apply <b>support</b> to combine rule and parent facts</li>
 *   <li>Apply <b>aggregation</b> when multiple facts derive the same new fact</li>
 *   <li>Detect <b>conflict</b> when a fact and its negation appear</li>
 *   <li>Build an {@link ArgumentativeGraph} with all edge relations</li>
 * </ol>
 *
 * <h3>Label Interpretation</h3>
 * <ul>
 *   <li>If attributes parse as doubles -> numeric algebra via exp4j</li>
 *   <li>If not -> interpreted as sets of strings (support = Union,
 *       aggregation = Union, conflict = Intersection)</li>
 * </ul>
 *
 * <h3>Returned Structure</h3>
 * The engine returns an {@link ArgumentativeGraph} containing:
 * <ul>
 *   <li>A map {@code parent -> list of derived facts}</li>
 *   <li>A list of conflict pairs</li>
 * </ul>
 * 
 * @JaviDebórtoli
 */
public class InferenceEngine {
    private final Map<KnowledgePiece, List<Fact>> edges;    /** Stores argumentation edges used when constructing the graph. */
    private final List<Fact> facts;                         /** Active list of facts during the inference cycle. */
    private final List<Rule> rules;                         /** Rule set composing the program being evaluated. */
    private final String[][] functions;                     /** Matrix storing the label algebra functions. */
    private final List<KnowledgePiece> removableEdges;      /** Temporary list of edges that need to be removed after conflict resolution. */
    private final List<PairInConflict> conflictiveNodes;    /** Pairs of contradictory facts detected during the conflict phase. */
    
    /**
     * Creates a new inference engine from lists of facts, rules,
     * and the label algebra function table.
     * 
     * @param facts      The initial list of known facts. These form the base
     *                   knowledge from which the inference process begins.
     * @param rules      The set of rules that may produce new facts during
     *                   evaluation. These rules are not modified by the engine.
     * @param functions  A 2D array representing the label algebra
     */
    public InferenceEngine(List<Fact> facts, List<Rule> rules, String[][] functions) {
        this.edges = new HashMap<>();               
        this.facts = facts;                         
        this.rules = rules;                         
        this.functions = functions;                 
        this.removableEdges = new ArrayList<>();    
        this.conflictiveNodes = new ArrayList<>();
    }
    
    /**
     * Executes the complete inference cycle and constructs the argumentative structure.
     * <p>
     * This method drives the full process:
     * <ol>
     *   <li>Iterates through all rules and attempts inference for every argument</li>
     *   <li>Derives new facts when rule activation is successful</li>
     *   <li>Checks for aggregation when duplicate facts appear</li>
     *   <li>Marks edges for removal when a fact must be replaced by its aggregated version</li>
     *   <li>Detects contradictions (p(X) vs ~p(X)) and computes weakened labels</li>
     * </ol>
     *
     * When no new facts are generated and all aggregations/conflicts have been processed,
     * the system is considered stable and returns the internal graph representation.
     *
     * @return an {@link ArgumentativeGraph} that connects parents (Facts/Rules)
     * to derived Facts, plus conflict pairs for visualization.
     */
    public ArgumentativeGraph buildTree() {
        List<Fact> potentialFacts = new ArrayList<>();
        boolean anyNewFact;
        Fact newFact = null;
        int bodyPartsVerified;
        List<String> arguments = new ArrayList<>();
        // All arguments different from the facts are obtained.
        for (Fact fact : facts) {
            if (!arguments.contains(fact.getArgument())) {
                arguments.add(fact.getArgument());
            }
        }
        
        do {
            anyNewFact = false; // Indicates whether the graph has been modified and the cycle must be repeated.

            for (String argument : arguments) { // Arguments cycle
                for (Rule rule : rules) { // Rules cycle

                    potentialFacts.clear();
                    bodyPartsVerified = 0;

                    for (String bodypart : rule.getBody()) { // Body cycle for each rule
                        for (Fact fact : facts) { // Facts cycle                    
                            
                            if ( bodypart.equals(fact.getName()) && fact.getArgument().equals(argument)) {    
                                newFact = new Fact(rule.getHead(), fact.getArgument(), null); // New fact created
                                potentialFacts.add(fact); // The fact is added to the fulfilled predicates of the rule.
                                bodyPartsVerified++;
                            }
                            
                        }
                    }

                    if ( bodyPartsVerified == rule.getBody().size() 
                            && !alreadyExists(newFact, rule) 
                            && !anyAggregation(newFact) ) {

                        addFact(potentialFacts, newFact, rule); // New fact added 
                        anyNewFact = true; // Indicates that the major cycle must be repeated
                    } else if ( bodyPartsVerified == rule.getBody().size() 
                            && !alreadyExists(newFact, rule) 
                            && anyAggregation(newFact) ){

                        doAggregation(potentialFacts, newFact, rule); // New aggregated fact added
                        anyNewFact = true; // Indicates that the major cycle must be repeated
                    }
                }
            }
        } while (anyNewFact);
        
        conflict(); // Conflicts between facts are resolved
        
        return new ArgumentativeGraph(edges, conflictiveNodes);
    }
    
    /**
     * Registers a newly inferred fact into the inference state and updates the graph structure.
     * <p>
     * This method is called when a rule has been successfully activated and a conclusion
     * (new fact) can be generated from a set of premises.
     * </p>
     *
     * <h3>Operational steps performed:</h3>
     * <ol>
     *   <li>Computes the labels of the new fact by applying the SUPPORT operation:
     *       <pre>support(premises, rule)</pre>
     *   </li>
     *   <li>Adds the new fact to the global list of known facts</li>
     *   <li>Creates a SUPPORT edge from the rule to the new fact</li>
     *   <li>Creates AGGREGATION-candidate edges from all premises to the new fact</li>
     * </ol>
     *
     * @param potentialFacts The list of existing facts that matched the rule body
     * and enabled the inference. They become parents in the graph.
     * @param newFact The fact generated as a consequence of firing the rule.
     * @param rule The rule used to derive the new fact; origin point of the SUPPORT edge.
     * 
     * This method does not check for duplicates or conflicts; that is handled afterward
     * by aggregation and conflict-management routines.
     */
    private void addFact (List<Fact> potentialFacts, Fact newFact, Rule rule) {
        // Calculate label values 
        newFact.setAttributes( calculateSupport (potentialFacts, rule) );
        // Add new fact to facts list
        facts.add(newFact); 
        // Add edge between the activated rule and the new fact.
        if (!edges.containsKey(rule)) {
            edges.put(rule, new ArrayList<>());
        }
        edges.get(rule).add(newFact);
        // Add edge between the ancestors facts and the new fact.
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new ArrayList<>());
            }
            edges.get(potentialFact).add(newFact);
        }
    }
    
    /**
     * Computes the SUPPORT operation for a newly inferred fact according to the
     * label algebra defined in {@code functions[i][0]}.
     * <p>
     * This operation combines the labels of the premises (facts that activated the rule)
     * and the label of the rule itself. The computation mechanism depends on the type of
     * labels of the program:
     *
     * <h3>Numeric evaluation (default attempt)</h3>
     * If the labels can be parsed as {@code double}, the method:
     * <ul>
     *   <li>Evaluates the expression in {@code functions[i][0]} using exp4j</li>
     *   <li>Sequentially combines: accumulator -> premises -> rule</li>
     *   <li>Clamps the numeric result to the range {@code [0.0, 1.0]}</li>
     * </ul>
     *
     * <h3>Symbolic / Set-based fallback</h3>
     * If numeric evaluation fails (IllegalArgumentException), the method performs
     * a symbolic operation. Currently:
     * <ul>
     *   <li>{@code "Union"} → merges all textual labels without repetition</li>
     * </ul>
     *
     * @param potentialFacts    The list of facts that satisfied the body of the rule
     *                          and enable the derivation of a new fact.
     * @param rule              The rule being activated; its label participates last in the
     *                          calculation and completes the support propagation.
     * 
     * @return  A {@code String[]} vector of labels representing the result of applying
     *          the SUPPORT algebra to the given premises and rule.
     */
    private String[] calculateSupport (List<Fact> potentialFacts, Rule rule) {
        String[] atributtes = new String[ potentialFacts.getFirst().getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            atributtes[i] = "0.0";
            // Reemplazar los valores de X y Y, y evaluar la funcion para cada uno de los antecedentes
            try {
                for (Fact fact : potentialFacts) {
                    expression = new ExpressionBuilder( functions[i][0] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(atributtes[i]))
                        .setVariable("Y", Double.parseDouble(fact.getAttributes()[i]));

                    atributtes[i] = String.valueOf(expression.evaluate());
                }
                // Reemplazar los valores de X y Y, y evaluar la funcion para la regla
                expression = new ExpressionBuilder( functions[i][0] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(atributtes[i]))
                        .setVariable("Y", Double.parseDouble(rule.getAttributes()[i]));

                atributtes[i] = String.valueOf(expression.evaluate());
                // Ubicar los valores en el intervalo [0, 1]
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                
                try {
                    union.clear();
                    
                    switch (functions[i][0]) {
                        case "Union" -> {
                            for (Fact fact : potentialFacts) {
                                union.add(fact.getAttributes()[i]);
                            }
                            union.add(rule.getAttributes()[i]);
                            atributtes[i] = String.join(" ", union);
                            break;
                        }
                        
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }
    
    /**
     * Determines whether two facts represent the same logical statement in the
     * knowledge base, ignoring their attribute values.
     * <p>
     * Two facts are considered equal if and only if:
     * <ul>
     *   <li>They share the same predicate name</li>
     *   <li>They refer to the same argument</li>
     * </ul>
     *
     * Attribute vectors (original or delta) are <b>not</b> compared here. This is
     * intentional: equality is defined at the logical level (predicate + argument),
     * while label differences are handled later during aggregation or conflict.
     *
     * @param firstFact  The first fact to compare
     * @param secondFact The second fact to compare
     * @return {@code true} if both facts describe the same logical statement,
     *         {@code false} otherwise
     */
    private boolean equalFacts (Fact firstFact, Fact secondFact) {
        return firstFact.getName().equals(secondFact.getName()) 
                && firstFact.getArgument().equals(secondFact.getArgument());
    } 
    
    /**
     * Checks whether a fact that would be derived by a given rule
     * has already been produced by that same rule in the past.
     *
     * <p>
     * This method does <b>not</b> check global fact duplication. Instead, it verifies
     * a more specific condition:
     * <br><br>
     * <b>"Has this rule previously generated a fact with the same
     * predicate and argument?"</b>
     * </p>
     *
     * <h3>Equality Criterion</h3>
     * The comparison is logical (semantic), not syntactic:
     * <ul>
     *   <li>Same predicate name</li>
     *   <li>Same argument</li>
     * </ul>
     * Labels are ignored; if label differences arise, the aggregation procedure
     * will handle them later.
     *
     * @param newFact The fact that may potentially be derived again
     * @param rule    The rule that would originate the derivation
     * @return {@code true} if the rule has already produced a logically identical fact, {@code false} otherwise.
     */
    private boolean alreadyExists (Fact newFact, Rule rule) {
        boolean existsRule = false;

        for ( Map.Entry<KnowledgePiece, List<Fact>> edge : edges.entrySet() ) {
            for (Fact fact : edge.getValue()) {
                if ( equalFacts(fact, newFact) && edge.getKey() == rule ) {
                    existsRule = true;
                }
            }
        }
        
        return existsRule;
    }
    
    /**
     * Determines whether a newly inferred fact requires aggregation with an
     * existing one already present in the argumentative graph.
     *
     * <p>
     * A fact needs aggregation when there is already another fact that:
     * <ul>
     *   <li>Has the same predicate (name)</li>
     *   <li>Refers to the same argument</li>
     * </ul>
     * regardless of their attribute values.
     * </p>
     *
     * <p>
     * The search is performed through the current graph structure stored in
     * {@code edges}. A match triggers the aggregation process which will merge
     * labels, update edges, and unify the fact representation.
     * </p>
     * 
     * @param newFact The newly generated fact whose presence in the graph must be checked
     * @return  {@code true} if a fact with the same name and argument already exists
     *           in the graph, indicating that an aggregation step is required;
     *           {@code false} otherwise
     */
    private boolean anyAggregation(Fact newFact) {
        boolean aggregation = false;

        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            KnowledgePiece key = entry.getKey();

            if (key instanceof Fact && equalFacts((Fact) key, newFact)) {
                aggregation = true;
                break;
            }

            for (Fact fact : entry.getValue()) {
                if (equalFacts(fact, newFact)) {
                    aggregation = true;
                    break;
                }
            }

            if (aggregation) {
                break;
            }
        }

        return aggregation;
    }

    /**
     * Performs aggregation when a newly inferred fact is logically identical to an
     * existing one (same predicate and argument), preventing duplication in the graph.
     *
     * <p>
     * Aggregation is triggered when {@code anyAggregation(newFact)} is true.
     * Instead of creating a second node for the same fact, the system:
     * </p>
     *
     * <ol>
     *   <li>Locates the existing matching fact (same name + argument)</li>
     *   <li>Removes the outdated version from the knowledge base</li>
     *   <li>Recomputes the new fact's attributes via SUPPORT</li>
     *   <li>Creates edges from the rule and its premises to the new fact</li>
     *   <li>Merges labels with the previous version using {@code calculateAggregation(...)}</li>
     *   <li>Inserts the aggregated fact as the unified representation</li>
     *   <li>Triggers a reorganization of the graph to preserve coherence</li>
     * </ol>
     *
     * <h3>Aggregation rationale</h3>
     * If two derivations lead to the same fact:
     * <pre>
     *    factA(X). {red}        from Rule R1
     *    factA(X). {blue}       from Rule R2
     *
     * -> aggregation ->  factA(X). {red blue}
     * </pre>
     *
     * <h3>Label algebra behavior</h3>
     * <ul>
     *   <li>Numeric: applies the operator defined in {@code functions[i][1]}</li>
     *   <li>Symbolic: falls back to set union (no duplicates)</li>
     * </ul>
     *
     * <h3>Graph effects</h3>
     * <ul>
     *   <li>Removes the outdated fact instance</li>
     *   <li>Adds new edges from premises and rule to the unified fact</li>
     *   <li>Rebuilds dependency paths to avoid dangling references</li>
     * </ul>
     *
     * @param potentialFacts The list of existing facts that activated the rule
     * @param newFact        The newly derived fact that caused the aggregation trigger
     * @param rule           The rule responsible for producing {@code newFact}
     */
    private void doAggregation(List<Fact> potentialFacts, Fact newFact, Rule rule) {
        Fact auxFact = null;

        for (Fact fact : facts) {
            if (equalFacts(fact, newFact)) {
                auxFact = fact; // The same fact is found
                break;
            }
        }

        if (auxFact != null) {
            facts.remove(auxFact); // The same fact is removed
        } else {
            auxFact = combineFacts(newFact);
        }

        newFact.setAttributes(calculateSupport(potentialFacts, rule)); // Calculate the attributes values
        // Add the edge from the activated rule to the new fact
        if (!edges.containsKey(rule)) {
            edges.put(rule, new ArrayList<>());
        }
        edges.get(rule).add(newFact);
        // Add edges between ancestors and the new fact
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new ArrayList<>());
            }
            edges.get(potentialFact).add(newFact);
        }
        // Calculate the attributes values
        Fact aggregatedFact = new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(newFact, auxFact));
        // Add new fact to facts list
        facts.add(aggregatedFact);
        // Re-build argumentative graph with the new aggregation
        reBuilTree(aggregatedFact);
    }
    
    /**
     * Builds a new unified fact by aggregating all existing occurrences of a fact
     * with the same predicate and argument as {@code newFact}.
     *
     * <p>
     * This method is used as a fallback when an aggregation is required but the
     * matching fact is not found directly in the main fact list (e.g., because it
     * only appears as part of graph edges). Rather than assuming no duplicates
     * exist, the engine performs a search across the graph to collect all fact
     * instances related to the same logical statement.
     * </p>
     *
     * <h3>Operational steps</h3>
     * <ol>
     *   <li>Searches keys and values of {@code edges} (graph structure)</li>
     *   <li>Identifies every fact that matches:
     *      <pre>same predicate AND same argument</pre>
     *   </li>
     *   <li>Accumulates these duplicates in {@code aggregatedFacts}</li>
     *   <li>Delegates label aggregation to {@code calculateAggregation(...)}</li>
     *   <li>Creates and returns a new fact containing the unified label result</li>
     * </ol>
     * 
     * <h3>Label semantics</h3>
     * Numeric labels -> aggregated numerically according to {@code functions[i][1]}
     * Text labels -> merged as a set (union, no duplicates)
     * 
     * @param newFact   The fact that triggered the aggregation process; its predicate
     *                  and argument determine the search target for combination.
     * @return          A new {@link Fact} instance with merged labels representing the
     *                  unified version of all matching facts found in the graph.
     */
    private Fact combineFacts (Fact newFact) {
        List<Fact> aggregatedFacts = new ArrayList<>();
        // Looking for matching facts in the graph  
        for (Map.Entry<KnowledgePiece, List<Fact>> edge : edges.entrySet()) { 
            if (edge.getKey() instanceof Fact && equalFacts( (Fact) edge.getKey(), newFact) ) {
                aggregatedFacts.add((Fact) edge.getKey());
            }
            
            for (Fact piece : edge.getValue()) {
                if ( equalFacts(piece, newFact) ) {
                    aggregatedFacts.add( piece );
                }
            }
        }
        // The matching facts are combined
        return new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(aggregatedFacts) );
    }
        
    /**
     * Rebuilds the argumentative graph after an aggregation occurs, ensuring that
     * outdated references to previous fact versions are removed and replaced by
     * correct edges pointing to the newly aggregated fact.
     *
     * <p>
     * This method is invoked after aggregation has produced a unified fact. Its role
     * is to refactor the graph to reflect the updated structure:
     * </p>
     *
     * <h3>Reorganization steps</h3>
     * <ol>
     *   <li>Clears the temporary list of removable edges</li>
     *   <li>Searches the graph for references to old versions of the fact</li>
     *   <li>Marks outdated parents (old fact instances) for removal</li>
     *   <li>Detaches edges that still point to outdated aggregated nodes</li>
     *   <li>Registers the valid parents that must now point toward the new fact</li>
     *   <li>Removes outdated entries from the edge map</li>
     *   <li>Adds fresh edges so dependencies now reference {@code newFact}</li>
     * </ol>
     * 
     * <h3>Graph effects</h3>
     * <ul>
     *   <li>Old nodes representing the same fact cease to function as parents</li>
     *   <li>The graph no longer contains split origins for one fact</li>
     *   <li>Edge cycles caused by older duplicates are eliminated</li>
     * </ul>
     * 
     * @param newFact   The aggregated fact that must become the canonical version of
     *                  the fact in the graph. All references will be redirected to it.
     */
    private void reBuilTree (Fact newFact) {
        removableEdges.clear(); // Cleam removable edges list
        Set<KnowledgePiece> newEdges = new HashSet<>(); // Source facts of the new edges towards the aggregate fact
        // Looking for matching facts in the graph
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            // We are looking for edges that originate in the aggregated nodes.
            if ( entry.getKey() instanceof Fact && equalFacts( (Fact) entry.getKey(), newFact ) ) {
                newEdges.add(entry.getKey());
                removableEdges.add( (Fact) entry.getKey() );
                // Remove the upper nodes from the new aggregation
                eraseUpperNodes( entry.getValue() );
            } else {
                for (Fact fact : entry.getValue()) {
                    if ( equalFacts(fact, newFact) ) {
                        newEdges.add(fact);
                    }
                }
            }
        }
        // Remove edges
        for (KnowledgePiece removableEdge : removableEdges) {
            edges.remove(removableEdge);
        }
        // Add new aggregation edges
        for (KnowledgePiece edge : newEdges) {
            if (!edges.containsKey(edge)) {
                edges.put(edge, new ArrayList<>());
            }
            edges.get(edge).add(newFact);
        }
    }
    
    /**
     * Recursively marks nodes and their parent dependencies for removal when an
     * aggregated fact replaces previous instances in the graph.
     *
     * <p>
     * This method is invoked during the reorganization process triggered by
     * {@code reBuilTree(...)}. Its purpose is to ensure that outdated parent nodes
     * (facts that represented older versions of the same knowledge) are removed
     * from the argumentative graph structure.
     * </p>
     * 
     * <h3>Operational behavior</h3>
     * For each fact in {@code values}:
     * <ol>
     *   <li>If the fact appears as a key in the graph, its children are recursively
     *       processed to propagate removal upward through the derivation chain.</li>
     *   <li>The fact itself is marked for deletion in {@code removableEdges}.</li>
     *   <li>All parents that link to this fact (i.e. those for whom it appears as
     *       a destination in an edge) are also marked for removal.</li>
     * </ol>
     *
     * <h3>Conceptual purpose</h3>
     * When aggregation consolidates multiple fact instances into one canonical fact,
     * this method ensures that:
     * <ul>
     *   <li>Outdated derivation paths are cleaned</li>
     *   <li>No dangling references remain in the graph</li>
     *   <li>Invalid parent nodes are detached before reconstruction</li>
     * </ul>
     * 
     * @param values    The list of outdated fact nodes that must be recursively traced
     *                  and scheduled for removal from the graph structure.
     */
    public void eraseUpperNodes (List<Fact> values) {        
        for (Fact value : values) {
            // Remove upper nodes recursively
            if (edges.containsKey(value)) {
                eraseUpperNodes(edges.get(value));
            }
            // Remove node
            removableEdges.add(value); 
            // Remove edges that have the removed node as their destination
            for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
                for (Fact fact : entry.getValue()) {
                    if ( fact == value ) {
                        removableEdges.add(entry.getKey()); 
                    }
                }
            }
        } 
    }
    
    /**
     * Aggregates the label vectors of two logically equivalent facts to produce
     * a unified attribute representation.
     *
     * <p>
     * This method is used when an aggregation event occurs due to the derivation
     * of a new fact that matches a pre-existing fact (same predicate and argument).
     * Instead of keeping both versions, the system merges their labels using the
     * aggregation operator defined in the program's algebra.
     * </p>
     * 
     * <h3>Numeric mode (primary attempt)</h3>
     * If both labels can be parsed as numeric values:
     * <ul>
     *   <li>The operator {@code functions[i][1]} is interpreted by exp4j</li>
     *   <li>X -> label of {@code newFact}</li>
     *   <li>Y -> label of {@code removableFact}</li>
     *   <li>The result is clamped to the interval [0.0, 1.0]</li>
     * </ul>
     * 
     * <h3>Symbolic mode (fallback)</h3>
     * If numeric parsing fails at any index:
     * <ul>
     *   <li>The method performs a set-based merge of symbolic labels</li>
     *   <li>Only the {@code "Union"} operator is currently supported</li>
     * </ul>
     * 
     * <h3>Operator reference</h3>
     * The algebra index for aggregation is:
     * <pre>
     * functions[i][1]
     * </pre>
     * where {@code i} matches the label dimension.
     * 
     * @param newFact       The newly inferred fact whose labels need to be merged.
     * @param removableFact The pre-existing fact with equivalent predicate and argument.
     * @return              A {@code String[]} representing the aggregated label vector, combining
     *                      both numeric values (clamped to [0,1]) or symbolic sets (Union).
     */
    private String[] calculateAggregation(Fact newFact, Fact removableFact) {
        String[] atributtes = new String[ newFact.getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            try {
                atributtes[i] = "0.0";
                // Replace X and Y in the expresion
                expression = new ExpressionBuilder( functions[i][1] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(newFact.getAttributes()[i]))
                        .setVariable("Y", Double.parseDouble(removableFact.getAttributes()[i]));
                // Evaluate the expression with the current parameters
                atributtes[i] = String.valueOf(expression.evaluate());
                // Normalize values
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                try {
                    union.clear();
                    
                    switch (functions[i][1]) {
                        case "Union" -> {
                            union.addAll(Arrays.asList(newFact.getAttributes()[i].split(" ")));
                            union.addAll(Arrays.asList(removableFact.getAttributes()[i].split(" ")));
                            
                            atributtes[i] = String.join(" ", union);
                        }
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }

    /**
     * Aggregates the label vectors of multiple logically equivalent facts in order
     * to produce a single unified attribute representation.
     *
     * <p>
     * This method is typically used when several facts with the same predicate
     * and argument (duplicates) have been discovered in the graph and need to be
     * unified into one canonical fact instance. The aggregation algebra combines
     * all matching labels dimension by dimension.
     * </p>
     * 
     * <h3>Numeric mode (primary attempt)</h3>
     * If every value in the current label index can be parsed as a number:
     * <ul>
     *   <li>The aggregation operator at {@code functions[i][1]} is applied iteratively</li>
     *   <li>First fact initializes the accumulator</li>
     *   <li>Each subsequent fact updates it using exp4j</li>
     *   <li>The result is clamped to {@code [0.0, 1.0]}</li>
     * </ul>
     * 
     * <h3>Symbolic / Set-based fallback</h3>
     * If numeric parsing fails for a dimension:
     * <ul>
     *   <li>The dimension falls back to symbolic aggregation</li>
     *   <li>Currently only the {@code "Union"} operator is supported</li>
     *   <li>Duplicates are removed via {@link LinkedHashSet}</li>
     * </ul>
     * 
     * <h3>Algebra reference</h3>
     * Aggregation operator for label index {@code i}:
     * <pre>
     * functions[i][1]
     * </pre>
     * 
     * @param aggregatedFacts   The list of facts that share the same predicate and
     *                          argument, whose label values must be unified.
     * @return                  A {@code String[]} label vector representing the fully merged
     *                          attributes, either numerically aggregated or symbolically unioned.
     */
    private String[] calculateAggregation(List<Fact> aggregatedFacts) {
        String[] atributtes = new String[ aggregatedFacts.getFirst().getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            try {
                atributtes[i] = null;
            
                for (Fact fact : aggregatedFacts) {
                    if(atributtes[i] == null){
                        atributtes[i] = fact.getAttributes()[i];
                    } else {
                        // Replace X and Y in the expresion
                        expression = new ExpressionBuilder(functions[i][1])
                                .variables("X", "Y")
                                .build()
                                .setVariable("X", Double.parseDouble(atributtes[i]))
                                .setVariable("Y", Double.parseDouble(fact.getAttributes()[i]));
                        // Evaluate the expression with the current parameters
                        atributtes[i] = String.valueOf(expression.evaluate());
                    }
                }
                // Normalize values
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                try {
                    union.clear();
                    
                    switch (functions[i][1]) {
                        case "Union" -> {
                            for (Fact fact : aggregatedFacts) {
                                union.addAll(Arrays.asList(fact.getAttributes()[i].split(" ")));
                            }
                            
                            atributtes[i] = String.join(" ", union);
                        }
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }
    
    /**
     * Detects and processes logical conflicts between facts representing opposite
     * statements (i.e., affirmation vs. explicit negation) and applies the conflict
     * operator defined in the label algebra to weaken their attributes.
     *
     * <p>
     * A conflict is triggered when both of the following conditions hold:
     * <ul>
     *   <li>One fact has a predicate of the form {@code ~p}</li>
     *   <li>Another fact exists with predicate {@code p}</li>
     *   <li>Both facts share the same argument</li>
     * </ul>
     * 
     * <h3>Operational steps</h3>
     * <ol>
     *   <li>Collect all negated facts (predicates containing "~")</li>
     *   <li>For each negated fact, search for its positive counterpart</li>
     *   <li>Apply conflict algebra twice:
     *      <ul>
     *        <li>{@code nf = conflict(nf, pos)}</li>
     *        <li>{@code pos = conflict(pos, nf)}</li>
     *      </ul>
     *   </li>
     *   <li>Update {@code deltaAttributes} for both involved facts</li>
     *   <li>Register the conflict pair in {@code conflictiveNodes}
     *       so an attack edge can be built later</li>
     * </ol>
     * 
     * <h3>Label semantics</h3>
     * The operator used for conflict is: 
     * <pre>
     * functions[i][2]
     * </pre>
     *
     * Two evaluation modes exist:
     * <ul>
     *   <li><b>Numeric:</b> evaluate algebra via exp4j</li>
     *   <li><b>Symbolic:</b> fallback to set intersection</li>
     * </ul>
     * 
     * @implNote
     * This method mutates the state of the engine by modifying the
     * {@code deltaAttributes} of conflicting facts. The original values remain in
     * {@code attributes}, allowing comparison between original and weakened labels.
     *
     * @see #calculateAttack(Fact, Fact)
     */
    private void conflict() {
        List<Fact> negativeFacts = new ArrayList<>();
        String[] Attributte1;
        String[] Attributte2;
        // Capture all denied facts
        for (Fact fact : facts) {
            if (fact.getName().contains("~")) {
                negativeFacts.add(fact);
            }
        }
        
        for (Fact nf : negativeFacts) {
            for (Fact fact : facts) { // Find contradictions
                if (nf.getName().replace("~", "").equals(fact.getName()) &&
                    nf.getArgument().equals(fact.getArgument())) {
                    
                    Attributte1 = calculateAttack(nf, fact);
                    Attributte2 = calculateAttack(fact, nf);
                    
                    nf.setDeltaAttributes(Attributte1);
                    fact.setDeltaAttributes(Attributte2);
                    
                    conflictiveNodes.add(new PairInConflict(nf, fact)); // Calculate delta attributes
                }
            }
        }
    }
    
    /**
     * Applies the conflict (attack) operator to two contradictory facts in order to
     * weaken their label vectors according to the program's conflict algebra.
     *
     * <p>
     * This method is invoked when a pair of facts representing opposite statements
     * (e.g., {@code p(X)} vs {@code ~p(X)}) has been detected. The conflict operator
     * defined at {@code functions[i][2]} is then applied dimension by dimension.
     * </p>
     *
     * <h3>Numeric attack (primary mode)</h3>
     * If both label values can be parsed as numbers:
     * <ul>
     *   <li>The operator in {@code functions[i][2]} is evaluated using exp4j</li>
     *   <li>{@code X ←} value from {@code f1}</li>
     *   <li>{@code Y ←} value from {@code f2}</li>
     *   <li>The result is clamped to the interval {@code [0.0, 1.0]}</li>
     * </ul>
     * 
     * <h3>Symbolic attack (fallback)</h3>
     * If numeric parsing fails:
     * <ul>
     *   <li>Only {@code "Intersection"} is currently supported</li>
     *   <li>Each dimension is processed by set intersection</li>
     *   <li>Duplicates are removed and order is preserved</li>
     * </ul>
     * 
     * <h3>Algebra index reference</h3>
     * The conflict operator is selected via:
     * <pre>
     * functions[i][2]
     * </pre>
     * 
     * @param f1    The first fact involved in the contradiction.
     * @param f2    The opposing fact (negated counterpart or symmetrical target).
     * @return      A {@code String[]} label vector representing the weakened form of
     *              {@code f1} after conflict resolution with {@code f2}.
     */
    private String[] calculateAttack (Fact f1, Fact f2) {
        String[] attributtes = new String[f1.getAttributes().length];
        Expression expression;
        LinkedHashSet<String> intersection1 = new LinkedHashSet<>();
        LinkedHashSet<String> intersection2 = new LinkedHashSet<>();
        
        for (int i = 0; i < attributtes.length; i++) { 
            try {
                // Replace X and Y in the expresion
                expression = new ExpressionBuilder( functions[i][2] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(f1.getAttributes()[i]))
                        .setVariable("Y", Double.parseDouble(f2.getAttributes()[i]));

                // Evaluate the expression with the current parameters
                attributtes[i] = String.valueOf(expression.evaluate());
                // Normalize values
                if (Double.parseDouble(attributtes[i])>1) {
                    attributtes[i] = "1.0";
                } else if (Double.parseDouble(attributtes[i])<0) {
                    attributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                /*
                *
                * ONLY INTERSECTION SUPPORTED!!!!!!!!!!
                *
                */
                try {
                    intersection1.clear();
                    intersection2.clear();
                    
                    switch (functions[i][2]) {
                        case "Intersection" -> {
                            intersection1.addAll(Arrays.asList(f1.getAttributes()[i].split(" ")));
                            intersection2.addAll(Arrays.asList(f2.getAttributes()[i].split(" ")));
                            
                            intersection1.retainAll(intersection2);
                            
                            attributtes[i] = String.join(" ", intersection1);
                        }
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return attributtes;
    }
}