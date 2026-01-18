package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.LabelOperationsDTO;
import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.Domain.OperationSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Stateless service responsible for mapping algebraic operation definitions
 * received from the client into domain-level {@link OperationSet} instances.
 *
 * <p>
 * This service performs pure transformations and does not maintain any
 * internal state, making it thread-safe and suitable for request-driven
 * execution.
 * </p>
 * 
 * @author JaviDebórtoli
 */
@Service
public class AlgebraMapperService {
    /**
     * Maps the algebraic operation definitions contained in the given request
     * into a map of {@link OperationSet} objects indexed by label name.
     *
     * <p>
     * Each {@link LabelOperationsDTO} defines the support, aggregation and
     * conflict functions associated with a specific label.
     * </p>
     *
     * @param request the input request containing label operation definitions
     * @return a map associating label names with their corresponding
     * {@link OperationSet} instances (empty if input is null)
     */
    public Map<String, OperationSet> mapOperations(OperationInputRequest request) {
        Map<String, OperationSet> operationsByLabel = new HashMap<>();

        if (request == null) {
            return operationsByLabel;
        }

        List<LabelOperationsDTO> labels = request.getLabels();
        if (labels == null) {
            return operationsByLabel;
        }

        for (LabelOperationsDTO dto : labels) {
            OperationSet operationSet = new OperationSet(
                    dto.getSupportFunction(),
                    dto.getAggregationFunction(),
                    dto.getConflictFunction()
            );

            operationsByLabel.put(dto.getLabelName(), operationSet);
        }

        return operationsByLabel;
    }
}
