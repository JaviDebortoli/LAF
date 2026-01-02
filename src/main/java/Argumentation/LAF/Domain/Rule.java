package Argumentation.LAF.Domain;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a rule in the knowledge base.
 * <p>
 * A rule has the general form:
 * </p>
 *
 * <pre>
 *   head(X) :- body1(X), body2(X), ..., bodyN(X).
 * </pre>
 *
 * where:
 * <ul>
 *   <li>{@link #head} is the name of the head predicate (e.g. {@code buy})</li>
 *   <li>{@link #body} is the list of predicate names in the body
 *       (e.g. {@code ["goodArea", "cheap"]})</li>
 *   <li>The attributes and deltaAttributes (inherited from
 *       {@link KnowledgePiece}) encode the labels attached to the rule
 *       in the label algebra.</li>
 * </ul>
 *
 * @author JaviDebórtoli
 */
public class Rule extends KnowledgePiece{
    /** Name of the head predicate of the rule. */
    private final String head;
    /** List of predicate names appearing in the rule body. */
    private final List<String> body;

    /**
     * Creates a rule with a head, body and label vector.
     *
     * @param head       head predicate name
     * @param body       list of body predicate names
     * @param attributes initial label values for this rule
     */
    public Rule(String head, List<String> body, String[] attributes) {
        this.head = head;
        this.body = body;
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }

    /**
     * Returns the head predicate name.
     *
     * @return head predicate (e.g. "buy").
     */
    public String getHead() {
        return head;
    }

    /**
     * Returns the list of body predicate names.
     *
     * @return list of predicate names (e.g. ["goodArea", "cheap"]).
     */
    public List<String> getBody() {
        return body;
    }

    /**
     * Human-readable representation of the rule, following the
     * standard Prolog-style syntax used in the project.
     * <p>
     * Example:
     * </p>
     * <pre>
     * buy(X) :- goodArea(X), cheap(X). {1.0} {1.0}
     * </pre>
     *
     * @return textual representation of the rule
     */
    @Override
    public String toString() {
        String bodyString = body.stream()
            .map(pred -> pred + "(X)")
            .collect(java.util.stream.Collectors.joining(", "));

        return head 
                + "(X) :- " 
                + bodyString 
                + ". " 
                + Arrays.toString(attributes)
                + " "
                + Arrays.toString(deltaAttributes);
    }
    
}