/*
 * Convierte DTOs → domain (instancia Facts y Rules)
 */
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
 * @author JaviDebórtoli
 */
@Service
public class ProgramLoaderService {
    
    private List<Fact> currentFacts = new ArrayList<>();
    private List<Rule> currentRules = new ArrayList<>();

    public void loadProgram(ProgramInputRequest request) {
        this.currentFacts = mapFacts(request.getFacts());
        this.currentRules = mapRules(request.getRules());
    }

    public List<Fact> getCurrentFacts() {
        return currentFacts;
    }

    public List<Rule> getCurrentRules() {
        return currentRules;
    }

    public List<Fact> mapFacts(List<FactDTO> factDtos) {
        List<Fact> facts = new ArrayList<>();
        if (factDtos == null) return facts;

        for (FactDTO dto : factDtos) {
            Fact fact = new Fact(dto.getName(), dto.getArgument(), dto.getAttributes());
            facts.add(fact);
        }
        return facts;
    }

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
