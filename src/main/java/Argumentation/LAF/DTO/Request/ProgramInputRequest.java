package Argumentation.LAF.DTO.Request;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import java.util.List;

/**
 * Data Transfer Object (DTO) used to encapsulate the input program sent by the
 * client to the system. This object represents the initial knowledge base
 * composed of facts and rules that will be loaded into the argumentation model.
 *
 * <p>
 * This DTO is typically received via a REST request and later transformed into
 * domain objects ({@link Argumentation.LAF.Domain.Fact} and
 * {@link Argumentation.LAF.Domain.Rule}) by the {@code ProgramLoaderService}.
 * </p>
 *
 * <p>
 * It does not perform any validation or inference logic; its sole responsibility
 * is to transport structured input data between layers.
 * </p>
 * 
 * @author JaviDebórtoli
 */
@Deprecated
public class ProgramInputRequest {
    /** List of facts provided as part of the input program. */
    private List<FactDTO> facts;
    /** List of rules provided as part of the input program. */
    private List<RuleDTO> rules;
    
    /**
     * Returns the list of fact DTOs included in the input program.
     *
     * @return list of {@link FactDTO} objects, or {@code null} if none were provided
     */
    public List<FactDTO> getFacts() {
        return facts;
    }
    
    /**
     * Returns the list of rule DTOs included in the input program.
     *
     * @return list of {@link RuleDTO} objects, or {@code null} if none were provided
     */
    @Deprecated
    public List<RuleDTO> getRules() {
        return rules;
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
     * Sets the list of rule DTOs that compose the input program.
     *
     * @param rules list of {@link RuleDTO} objects
     */
    @Deprecated
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
}