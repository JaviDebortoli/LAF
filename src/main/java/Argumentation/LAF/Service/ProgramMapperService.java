package Argumentation.LAF.Service;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import Argumentation.LAF.Domain.Fact;
import Argumentation.LAF.Domain.Rule;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Stateless service responsible for mapping input DTOs into
 * domain-level objects used by the inference engine.
 *
 * <p>
 * This service performs pure transformations and does not keep
 * any internal state, making it thread-safe and suitable for
 * request-driven execution.
 * </p>
 * 
 * @author JaviDebórtoli
 */
@Service
public class ProgramMapperService {
    /**
     * Maps a list of {@link FactDTO} objects into a list of domain {@link Fact}s.
     *
     * @param factDtos the list of fact DTOs received from the client
     * @return a list of mapped {@link Fact} instances (empty if input is null)
     */
    public List<Fact> mapFacts(List<FactDTO> factDtos) {
        List<Fact> facts = new ArrayList<>();

        if (factDtos == null || factDtos.isEmpty()) {
            return facts;
        }

        for (FactDTO dto : factDtos) {
            Fact fact = new Fact(
                    dto.getName(),
                    dto.getArgument(),
                    dto.getAttributes()
            );
            facts.add(fact);
        }

        return facts;
    }
    /**
     * Maps a list of {@link RuleDTO} objects into a list of domain {@link Rule}s.
     *
     * @param ruleDtos the list of rule DTOs received from the client
     * @return a list of mapped {@link Rule} instances (empty if input is null)
     */
    public List<Rule> mapRules(List<RuleDTO> ruleDtos) {
        List<Rule> rules = new ArrayList<>();

        if (ruleDtos == null || ruleDtos.isEmpty()) {
            return rules;
        }

        for (RuleDTO dto : ruleDtos) {
            Rule rule = new Rule(
                    dto.getHeadName(),
                    dto.getBodyLiterals(),
                    dto.getAttributes()
            );
            rules.add(rule);
        }

        return rules;
    }
}
