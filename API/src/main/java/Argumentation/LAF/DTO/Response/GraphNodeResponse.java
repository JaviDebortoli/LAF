package Argumentation.LAF.DTO.Response;

/**
 * Data Transfer Object (DTO) used to represent a node of the argumentation
 * graph in REST responses.
 *
 * <p>
 * Each instance of this class corresponds to a node generated from the
 * Label-Based Argumentation Framework (LAF) internal graph and is intended
 * to be consumed by client-side components, such as visualization or
 * analysis tools.
 * </p>
 *
 * <p>
 * A graph node may represent different conceptual entities depending on its
 * {@code type}, such as information nodes, rule application nodes or conflict
 * nodes.
 * </p>
 *
 * @author JaviDebórtoli
 */
public class GraphNodeResponse {
    /** Unique identifier of the node within the argumentation graph. */
    private String id;
    /** Human-readable label associated with the node. */
    private String label;
    /** Type of the node in the argumentation graph. */
    private String type;
    /** Attributes associated with the node. */
    private String[] attributes;
    /**
     * Variation applied to the node attributes during the last inference step.
     * A value of {@code 0.0} indicates that the attribute was not modified by
     * any algebraic operation.
     */
    private String[] deltaAttributes;

    /**
     * Returns the unique identifier of the node.
     *
     * @return the node identifier
     */
    @Deprecated
    public String getId() {
        return id;
    }

    /**
     * Returns the label associated with the node.
     *
     * @return the node label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the type of the node.
     *
     * @return the node type
     */
    @Deprecated
    public String getType() {
        return type;
    }

    /**
     * Returns the attributes assigned to the node.
     *
     * @return an array of attribute values
     */
    @Deprecated
    public String[] getAttributes() {
        return attributes;
    }

    /**
     * Returns the delta attributes of the node.
     *
     * @return an array representing delta attribute values
     */
    @Deprecated
    public String[] getDeltaAttributes() {
        return deltaAttributes;
    }

    /**
     * Sets the unique identifier of the node.
     *
     * @param id the node identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the label associated with the node.
     *
     * @param label the node label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets the type of the node.
     *
     * @param type the node type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets the attributes assigned to the node.
     *
     * @param attributes an array of attribute values
     */
    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }

    /**
     * Sets the delta attributes of the node.
     *
     * @param deltaAttributes an array representing delta attribute values
     */
    public void setDeltaAttributes(String[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }
}