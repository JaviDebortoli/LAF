package Argumentation.LAF.DTO.Request;

import Argumentation.LAF.DTO.LabelOperationsDTO;
import java.util.List;

/**
 * Data Transfer Object (DTO) used to receive the definition of algebraic
 * operations associated with labels in the Label-Based Argumentation Framework (LAF).
 *
 * <p>
 * This request encapsulates a collection of {@link LabelOperationsDTO} objects,
 * each one defining the support, aggregation and conflict functions associated
 * with a specific label.
 * </p>
 *
 * <p>
 * It is typically sent by the client to configure or update the algebra of
 * argumentation labels before executing the inference process.
 * </p>
 * 
 * @author JaviDebórtoli
 */
public class OperationInputRequest {
    /** List of label operation definitions. Each element specifies the algebraic 
     * functions (support, aggregation and conflict) associated with a label.*/
    private List<LabelOperationsDTO> labels;

    /**
     * Returns the list of label operation definitions.
     *
     * @return a list of {@link LabelOperationsDTO} instances
     */
    public List<LabelOperationsDTO> getLabels() {
        return labels;
    }
    
    /**
     * Sets the list of label operation definitions.
     *
     * @param labels a list of {@link LabelOperationsDTO} instances defining
     *               the algebraic operations for each label
     */
    public void setLabels(List<LabelOperationsDTO> labels) {
        this.labels = labels;
    }
}