package Argumentation.LAF.DTO;

import java.util.List;

/**
 * Data Transfer Object (DTO) used to represent a rule provided by the client.
 *
 * <p>
 * A {@code RuleDTO} defines an inference rule composed of a head literal and
 * a list of body literals. This structure is later mapped to the domain-level
 * {@link Argumentation.LAF.Domain.Rule} entity used during the inference process.
 * </p>
 *
 * <p>
 * Each rule consists of:
 * </p>
 * <ul>
 *     <li>A {@code headName} representing the conclusion of the rule.</li>
 *     <li>A list of {@code bodyLiterals} representing the premises.</li>
 *     <li>An optional set of {@code attributes} associated with the rule.</li>
 * </ul>
 * 
 * @author JaviDebórtoli
 */
public class RuleDTO {
    /** Name of the rule head, representing the conclusion inferred by the rule. */
    private String headName;
    /** List of literals forming the body of the rule (its premises). */
    private List<String> bodyLiterals;
    /** Set of attributes associated with the rule. */
    private String[] attributes;

    /**
     * Returns the head of the rule.
     *
     * @return the rule head name
     */
    public String getHeadName() {
        return headName;
    }

    /**
     * Returns the body literals of the rule.
     *
     * @return a list of rule premises
     */
    public List<String> getBodyLiterals() {
        return bodyLiterals;
    }
    
    /**
     * Returns the attributes associated with the rule.
     *
     * @return an array of attribute identifiers
     */
    public String[] getAttributes() {
        return attributes;
    }
    
    /**
     * Sets the head of the rule.
     *
     * @param headName the rule conclusion
     */
    public void setHeadName(String headName) {
        this.headName = headName;
    }

    /**
     * Sets the body literals of the rule.
     *
     * @param bodyLiterals the list of rule premises
     */
    public void setBodyLiterals(List<String> bodyLiterals) {
        this.bodyLiterals = bodyLiterals;
    }

    /**
     * Sets the attributes associated with the rule.
     *
     * @param attributes an array of attribute identifiers
     */
    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }
}