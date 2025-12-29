package Argumentation.LAF.Domain;

import java.util.Arrays;

public class Fact extends KnowledgePiece{
    
    private final String name;
    private final String argument;
    private Double[] deltaAttributes;

    public Fact(String name, String argument, Double[] attributes) {
        this.name = name;
        this.argument = argument;
        this.attributes = attributes;
        this.deltaAttributes = (attributes != null) ? attributes.clone() : null;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }

    public Double[] getDeltaAttributes() {
        return deltaAttributes;
    }

    public void setDeltaAttributes(Double[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }

    @Override
    public void setAttributes(Double[] attributes) {
        super.setAttributes(attributes);
        this.deltaAttributes = (attributes != null) ? attributes.clone() : null;
    }

    @Override
    public String toString() {
        return name + '('
                + argument + "). "
                + Arrays.toString(attributes)
                + " "
                + Arrays.toString(deltaAttributes);
    }
    
}