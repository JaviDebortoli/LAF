/*
 * 
 */
package Argumentation.LAF.Domain;

/**
 * @author JaviDebórtoli
 */
public class OperationSet {
    
    private final String supportExpr;
    private final String aggregationExpr;
    private final String conflictExpr;

    public OperationSet(String supportExpr, String aggregationExpr, String conflictExpr) {
        this.supportExpr = supportExpr;
        this.aggregationExpr = aggregationExpr;
        this.conflictExpr = conflictExpr;
    }

    public String getSupportExpr() { return supportExpr; }
    public String getAggregationExpr() { return aggregationExpr; }
    public String getConflictExpr() { return conflictExpr; }
    
}
