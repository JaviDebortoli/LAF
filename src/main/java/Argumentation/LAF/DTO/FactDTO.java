package Argumentation.LAF.DTO;

import java.util.Arrays;

/**
 * Data Transfer Object (DTO) representing a fact provided as input to the
 * argumentation system.
 *
 * <p>
 * This class is used to transport fact information from the client layer
 * (e.g. REST requests) to the application services, before being mapped
 * to the corresponding domain object {@link Argumentation.LAF.Domain.Fact}.
 * </p>
 *
 * <p>
 * A fact is defined by:
 * </p>
 * <ul>
 *     <li>A unique name that identifies the fact.</li>
 *     <li>An argument expression representing its content.</li>
 *     <li>An optional set of attributes associated with the fact.</li>
 * </ul>
 * 
 * @author JaviDebórtoli
 */
public class FactDTO {
    /** Identifier of the fact. */
    private String name;
    /** Logical or textual expression representing the argument. */
    private String argument;
    /** Set of attributes associated with the fact. */
    private String[] attributes;

    /**
     * Returns the name of the fact.
     *
     * @return the fact identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the argument expression associated with the fact.
     *
     * @return the argument expression
     */
    public String getArgument() {
        return argument;
    }
    
    /**
     * Returns the attributes associated with the fact.
     *
     * @return an array of attribute identifiers, or {@code null} if none are defined
     */
    public String[] getAttributes() {
        return attributes;
    }
    
    /**
     * Sets the name of the fact.
     *
     * @param name the fact identifier
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Sets the argument expression associated with the fact.
     *
     * @param argument the argument expression
     */
    public void setArgument(String argument) {
        this.argument = argument;
    }

    /**
     * Sets the attributes associated with the fact.
     *
     * @param attributes an array of attribute identifiers
     */
    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns a string representation of this FactDTO instance,
     * including its name, argument and attributes.
     *
     * @return a string representation of the fact DTO
     */
    @Override
    public String toString() {
        return "FactDto{" +
                "name='" + name + '\'' +
                ", argument='" + argument + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }
}