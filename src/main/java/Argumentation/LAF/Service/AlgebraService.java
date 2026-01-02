package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.LabelOperationsDTO;
import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.Domain.OperationSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing and providing access to the algebraic
 * operations used in the Label-Based Argumentation Framework (LAF).
 *
 * <p>
 * This service maintains a mapping between label identifiers and their
 * corresponding {@link OperationSet}, which defines the algebraic functions
 * used to combine, aggregate and resolve conflicts between argument labels.
 * </p>
 *
 * <p>
 * The operations are dynamically loaded from user input and can be queried
 * by label name during the inference and graph construction processes.
 * </p>
 *
 * @see OperationSet
 * @see OperationInputRequest
 * 
 * @author JaviDebórtoli
 */
@Service
public class AlgebraService {
    /** Map that associates each label name with its corresponding set of algebraic operations. */
    private final Map<String, OperationSet> operationsByLabel = new LinkedHashMap<>();
    
    /**
     * Loads the algebraic operations provided by the user into the service.
     *
     * <p>
     * This method clears any previously stored operations and populates the
     * internal mapping with the {@link OperationSet}s defined in the given
     * {@link OperationInputRequest}. Each label is associated with three
     * algebraic functions: support, aggregation and conflict.
     * </p>
     *
     * <p>
     * If the request or its label list is {@code null}, the method returns
     * without modifying the internal state.
     * </p>
     * 
     * @param request An {@link OperationInputRequest} containing the definition
     *                of algebraic operations grouped by label
     */
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

    /**
     * Returns the map of algebraic operations indexed by label name.
     *
     * <p>
     * The returned map associates each label with its corresponding
     * {@link OperationSet}, which defines the support, aggregation and
     * conflict functions to be used during inference.
     * </p>
     *
     * @return A map containing the operation sets associated with each label
     */
    public Map<String, OperationSet> getOperationsByLabel() {
        return operationsByLabel;
    }
}