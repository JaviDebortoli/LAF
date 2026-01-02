package Argumentation.LAF.Domain;

/**
 * Base abstract class for any piece of knowledge in the system.
 * <p>
 * A KnowledgePiece is a node in the argumentative graph and can be
 * either a {@link Fact} or a {@link Rule}. Both kinds of nodes share
 * the idea of having a vector of label values (attributes) and a
 * possibly modified version of those labels (deltaAttributes) that
 * result from the application of the label algebra
 * (support, aggregation, conflict).
 * </p>
 * 
 * @author JaviDebórtoli
 */
public abstract class KnowledgePiece {
    
    protected String[] attributes;      /** Original label values. */
    protected String[] deltaAttributes; /** Current label values after applying the label algebra. */

    /**
     * Returns the original attributes associated to this piece of knowledge.
     *
     * @return attribute vector, never modified by the inference process
     */
    public String[] getAttributes() {
        return attributes;
    }

    /**
     * Returns the current attributes.
     *
     * @return current attribute vector after inference
     */
    public String[] getDeltaAttributes() {
        return deltaAttributes;
    }
    
    /**
     * Sets the original attributes for this piece of knowledge.
     * <p>
     * As a convenience and to keep the model consistent, this method
     * also initializes {@link #deltaAttributes} with the same values,
     * so that before any inference both representations coincide.
     * </p>
     *
     * @param attributes attribute vector to assign
     */
    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }
    
    /**
     * Sets the current attributes.
     * <p>
     * Typically called by the {@code InferenceEngine}
     * </p>
     *
     * @param deltaAttributes new current attributes
     */
    public void setDeltaAttributes(String[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }

    /**
     * String representation of the piece of knowledge.
     * <p>
     * Each concrete subclass is responsible for providing a human
     * readable form.
     * </p>
     * 
     * @return form of knowledge representation that 
     * is understandable to humans.
     */
    @Override
    public abstract String toString();
    
}