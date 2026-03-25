package Argumentation.LAF.DTO;

/**
 * Data Transfer Object (DTO) that represents the algebraic operations
 * associated with a specific label in the Label-Based Argumentation Framework.
 *
 * <p>
 * Each label is defined by three operations:
 * </p>
 * <ul>
 *     <li><b>Support</b>: used to evaluate individual arguments.</li>
 *     <li><b>Aggregation</b>: used to combine multiple supporting arguments.</li>
 *     <li><b>Conflict</b>: used to resolve attacks between arguments.</li>
 * </ul>
 *
 * <p>
 * The operations are expressed as strings and later parsed and stored
 * in an {@link Argumentation.LAF.Domain.OperationSet}.
 * </p>
 * 
 * @author JaviDebórtoli
 */
public class LabelOperationsDTO {
    /** Name of the label to which the operations apply. */
    private String labelName;
    /** Expression defining the support operation. */
    private String supportFunction;
    /** Expression defining the aggregation operation. */
    private String aggregationFunction;
    /** Expression defining the conflict operation. */
    private String conflictFunction;

    /**
     * Returns the label name.
     *
     * @return the label name
     */
    public String getLabelName() {
        return labelName;
    }
    
    /**
     * Returns the support operation expression.
     *
     * @return the support function
     */
    public String getSupportFunction() {
        return supportFunction;
    }

    /**
     * Returns the aggregation operation expression.
     *
     * @return the aggregation function
     */
    public String getAggregationFunction() {
        return aggregationFunction;
    }
    
    /**
     * Returns the conflict operation expression.
     *
     * @return the conflict function
     */
    public String getConflictFunction() {
        return conflictFunction;
    }
    
    /**
     * Sets the label name.
     *
     * @param labelName the label name
     */
    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    /**
     * Sets the support operation expression.
     *
     * @param supportFunction the support function
     */
    public void setSupportFunction(String supportFunction) {
        this.supportFunction = supportFunction;
    }

    /**
     * Sets the aggregation operation expression.
     *
     * @param aggregationFunction the aggregation function
     */
    public void setAggregationFunction(String aggregationFunction) {
        this.aggregationFunction = aggregationFunction;
    }
    
    /**
     * Sets the conflict operation expression.
     *
     * @param conflictFunction the conflict function
     */
    public void setConflictFunction(String conflictFunction) {
        this.conflictFunction = conflictFunction;
    }
}