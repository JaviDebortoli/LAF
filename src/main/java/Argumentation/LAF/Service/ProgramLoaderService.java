package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.Request.ProgramInputRequest;
import Argumentation.LAF.DTO.RuleDTO;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.Rule;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service responsible for loading and mapping an argumentative program
 * provided by the client into its internal domain representation.
 *
 * <p>
 * This service acts as an intermediate layer between the REST input
 * ({@link ProgramInputRequest}) and the domain model ({@link Fact}, {@link Rule}).
 * It converts DTO-based representations of facts and rules into domain objects
 * that can be later used for graph construction and inference.
 * </p>
 *
 * <p>
 * The service maintains an internal state with the currently loaded facts
 * and rules, which represent the active argumentative program.
 * </p>
 *
 * @see ProgramInputRequest
 * @see Fact
 * @see Rule
 * 
 * @author JaviDebórtoli
 */
@Service
public class ProgramLoaderService {
    /** Internal list containing the facts currently loaded in the program. */
    private List<Fact> currentFacts = new ArrayList<>();
    /** Internal list containing the rules currently loaded in the program. */
    private List<Rule> currentRules = new ArrayList<>();

    /**
     * Loads an argumentative program from the given input request.
     *
     * <p>
     * This method maps the facts and rules defined in the
     * {@link ProgramInputRequest} into their corresponding domain objects
     * and stores them as the current active program.
     * </p>
     *
     * @param request the input request containing facts and rules to be loaded
     */
    public void loadProgram(ProgramInputRequest request) {
        this.currentFacts = mapFacts(request.getFacts());
        this.currentRules = mapRules(request.getRules());
    }

    /**
     * Returns the list of facts currently loaded in the program.
     *
     * @return the list of domain {@link Fact} objects
     */
    public List<Fact> getCurrentFacts() {
        return currentFacts;
    }

    /**
     * Returns the list of rules currently loaded in the program.
     *
     * @return the list of domain {@link Rule} objects
     */
    public List<Rule> getCurrentRules() {
        return currentRules;
    }

    /**
     * Maps a list of {@link FactDTO} objects into a list of domain
     * {@link Fact} instances.
     *
     * <p>
     * Each DTO is transformed by copying its name, argument and attributes
     * into a new domain object.
     * </p>
     *
     * @param factDtos The list of fact DTOs to be mapped.
     * @return A list of domain {@link Fact} objects.
     */
    public List<Fact> mapFacts(List<FactDTO> factDtos) {
        List<Fact> facts = new ArrayList<>();
        if (factDtos == null) return facts;

        for (FactDTO dto : factDtos) {
            Fact fact = new Fact(dto.getName(), dto.getArgument(), dto.getAttributes());
            facts.add(fact);
        }
        return facts;
    }

    /**
     * Maps a list of {@link RuleDTO} objects into a list of domain
     * {@link Rule} instances.
     *
     * <p>
     * Each DTO is transformed by copying its head literal, body literals
     * and attributes into a new domain object.
     * </p>
     *
     * @param ruleDtos The list of rule DTOs to be mapped
     * @return A list of domain {@link Rule} objects
     */
    public List<Rule> mapRules(List<RuleDTO> ruleDtos) {
        List<Rule> rules = new ArrayList<>();
        if (ruleDtos == null) return rules;

        for (RuleDTO dto : ruleDtos) {
            Rule rule = new Rule(dto.getHeadName(), dto.getBodyLiterals(), dto.getAttributes());
            rules.add(rule);
        }
        return rules;
    }
}