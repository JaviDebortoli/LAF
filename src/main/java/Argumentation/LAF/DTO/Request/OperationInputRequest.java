/*
 * Funciones ingresadas por el usuario
 */
package Argumentation.LAF.DTO.Request;

import Argumentation.LAF.DTO.LabelOperationsDTO;
import java.util.List;

/**
 * @author JaviDebórtoli
 */
public class OperationInputRequest {
    
    private List<LabelOperationsDTO> labels;

    public List<LabelOperationsDTO> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelOperationsDTO> labels) {
        this.labels = labels;
    }
    
}
