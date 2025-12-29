/*
 * 
 */
package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.LabelOperationsDTO;
import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.Domain.OperationSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author JaviDebórtoli
 */
@Service
public class AlgebraService {
    
    private final Map<String, OperationSet> operationsByLabel = new LinkedHashMap<>();

    public void loadOperations(OperationInputRequest request) {
        operationsByLabel.clear();
        if (request == null || request.getLabels() == null) return;

        for (LabelOperationsDTO dto : request.getLabels()) {
            OperationSet set = new OperationSet(
                    dto.getSupportFunction(),
                    dto.getAggregationFunction(),
                    dto.getConflictFunction()
            );
            operationsByLabel.put(dto.getLabelName(), set);
        }
    }

    public Map<String, OperationSet> getOperationsByLabel() {
        return operationsByLabel;
    }
    
}
