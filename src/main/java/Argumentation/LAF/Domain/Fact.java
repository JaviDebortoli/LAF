package Argumentation.LAF.Domain;

import java.util.Arrays;

/**
 * Represents an atomic fact in the knowledge base.
 * <p>
 * Facts have the form {@code name(argument)} and carry a vector of
 * labels (see {@link KnowledgePiece}) that quantify or qualify the fact
 * according to the label algebra.
 * </p>
 *
 * @author JaviDebórtoli
 */
public class Fact extends KnowledgePiece{
    
    private final String name;      /** Predicate name. */
    private final String argument;  /** Argument of the fact. */

    /**
     * Creates a new fact with the given name, argument and attribute vector.
     *
     * @param name       predicate name
     * @param argument   term used as argument of the predicate
     * @param attributes initial label values for this fact
     */
    public Fact(String name, String argument, String[] attributes) {
        this.name = name;
        this.argument = argument;
        this.attributes = attributes;
        this.deltaAttributes = attributes; // At creation time, deltaAttributes coincide with the original ones
    }

    /**
     * Returns the predicate name of this fact.
     *
     * @return predicate name (e.g. "goodArea").
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the argument of this fact.
     *
     * @return argument term (e.g. "houseA").
     */
    public String getArgument() {
        return argument;
    }

    /**
     * Human-readable representation of the fact including both
     * the original and the current labels.
     *
     * @return string in the form {@code name(argument). [attributes] [deltaAttributes]}
     */
    @Override
    public String toString() {
        return name + '('
                + argument + "). "
                + Arrays.toString(attributes)
                + " "
                + Arrays.toString(deltaAttributes);
    }
}