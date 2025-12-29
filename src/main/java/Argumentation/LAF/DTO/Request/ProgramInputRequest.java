/*
 * Facts + Rules + Labels received from the user
 */
package Argumentation.LAF.DTO.Request;

import Argumentation.LAF.DTO.FactDTO;
import Argumentation.LAF.DTO.RuleDTO;
import java.util.List;

/**
 * @author JaviDebórtoli
 */
public class ProgramInputRequest {
    
    private List<FactDTO> facts;
    private List<RuleDTO> rules;
    
    public List<FactDTO> getFacts() {
        return facts;
    }

    public void setFacts(List<FactDTO> facts) {
        this.facts = facts;
    }

    public List<RuleDTO> getRules() {
        return rules;
    }

    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
    
}
