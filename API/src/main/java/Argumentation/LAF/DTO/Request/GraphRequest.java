package Argumentation.LAF.DTO.Request;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import java.util.List;

/**
 * Data Transfer Object (DTO) that encapsulates all the information required
 * to generate an argumentation graph in a single request.
 *
 * <p>
 * This request follows a stateless and request-driven approach: the complete
 * knowledge base (facts and rules) together with the algebraic operations
 * associated with labels must be provided by the client each time the
 * graph generation endpoint is invoked.
 * </p>
 *
 * <p>
 * By centralizing all required inputs in one request, the backend remains
 * thread-safe, scalable and free of shared mutable state.
 * </p>
 * 
 * @author JaviDebórtoli
 */
public class GraphRequest {
    /**
     * List of facts that compose the factual component of the knowledge base.
     */
    private List<FactDTO> facts;
    /**
     * List of rules that compose the inferential component of the knowledge base.
     */
    private List<RuleDTO> rules;
    /**
     * Algebraic operation definitions associated with argument labels.
     * These operations define how support, aggregation and conflict are computed
     * during the inference process.
     */
    private OperationInputRequest operations;
    
    /**
     * Returns the list of fact DTOs provided in the request.
     *
     * @return list of {@link FactDTO} objects, or {@code null} if none were provided
     */
    public List<FactDTO> getFacts() {
        return facts;
    }
    /**
     * Sets the list of fact DTOs that compose the input program.
     *
     * @param facts list of {@link FactDTO} objects
     */
    public void setFacts(List<FactDTO> facts) {
        this.facts = facts;
    }
    /**
     * Returns the list of rule DTOs provided in the request.
     *
     * @return list of {@link RuleDTO} objects, or {@code null} if none were provided
     */
    public List<RuleDTO> getRules() {
        return rules;
    }
    /**
     * Sets the list of rule DTOs that compose the input program.
     *
     * @param rules list of {@link RuleDTO} objects
     */
    @Deprecated
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
    /**
     * Returns the algebraic operation definitions associated with labels.
     *
     * @return the {@link OperationInputRequest} defining label operations
     */
    public OperationInputRequest getOperations() {
        return operations;
    }
    /**
     * Sets the algebraic operation definitions associated with labels.
     *
     * @param operations the {@link OperationInputRequest} defining label operations
     */
    @Deprecated
    public void setOperations(OperationInputRequest operations) {
        this.operations = operations;
    }
}
