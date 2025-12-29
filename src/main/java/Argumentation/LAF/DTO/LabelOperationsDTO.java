/*
 * 
 */
package Argumentation.LAF.DTO;

/**
 * @author JaviDebórtoli
 */
public class LabelOperationsDTO {
    
    private String labelName;
    private String supportFunction;
    private String aggregationFunction;
    private String conflictFunction;

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getSupportFunction() {
        return supportFunction;
    }

    public void setSupportFunction(String supportFunction) {
        this.supportFunction = supportFunction;
    }

    public String getAggregationFunction() {
        return aggregationFunction;
    }

    public void setAggregationFunction(String aggregationFunction) {
        this.aggregationFunction = aggregationFunction;
    }

    public String getConflictFunction() {
        return conflictFunction;
    }

    public void setConflictFunction(String conflictFunction) {
        this.conflictFunction = conflictFunction;
    }
    
}
