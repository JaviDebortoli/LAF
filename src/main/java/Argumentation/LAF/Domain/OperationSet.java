package Argumentation.LAF.Domain;

/**
 * Encapsulates the three algebraic operations associated with a label.
 * <p>
 * For each label "li" the user must define:
 * </p>
 * <ul>
 *   <li>a <b>support</b> operation, applied when a rule and its premises
 *       jointly support a conclusion,</li>
 *   <li>an <b>aggregation</b> operation, applied when multiple
 *       independent supports converge on the same fact,</li>
 *   <li>a <b>conflict</b> operation, applied when two opposite facts
 *       (e.g. p and ~p) are in conflict.</li>
 * </ul>
 *
 * <p>
 * The expressions are stored as strings because:
 * <ul>
 *   <li>They can be numeric expressions to be evaluated with exp4j, e.g.
 *       {@code "X + Y"} or {@code "max(X,Y)"}; or</li>
 *   <li>They can be special keywords handled by the engine in a
 *       symbolic way, e.g. {@code "Union"} or {@code "Intersection"}.</li>
 * </ul>
 * </p>
 *
 * @author JaviDebórtoli
 */
public class OperationSet {
    
    private final String supportExpr;       /** Expression used for the support operation of this label. */
    private final String aggregationExpr;   /** Expression used for the aggregation operation of this label. */
    private final String conflictExpr;      /** Expression used for the conflict operation of this label. */

    /**
     * Creates a new set of operations for a label.
     *
     * @param supportExpr     expression for support (e.g. "X * Y" or "Union")
     * @param aggregationExpr expression for aggregation (e.g. "X + Y")
     * @param conflictExpr    expression for conflict (e.g. "max(X,Y)" or "Intersection")
     */
    public OperationSet(String supportExpr, String aggregationExpr, String conflictExpr) {
        this.supportExpr = supportExpr;
        this.aggregationExpr = aggregationExpr;
        this.conflictExpr = conflictExpr;
    }

    /**
     * Returns the expression used for the support operation.
     * 
     * @return support operation expression.
     */
    public String getSupportExpr() { return supportExpr; }
    
    /**
     * Returns the expression used for the aggregation operation.
     * 
     * @return aggregation operation expression.
     */
    public String getAggregationExpr() { return aggregationExpr; }
    
    /**
     * Returns the expression used for the conflict operation.
     * 
     * @return conflict operation expression.
     */
    public String getConflictExpr() { return conflictExpr; }
    
}
