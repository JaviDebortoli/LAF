package Argumentation.LAF.Domain;

import java.util.Arrays;

public class Fact extends KnowledgePiece{
    
    private final String name;
    private final String argument;

    public Fact(String name, String argument, String[] attributes) {
        this.name = name;
        this.argument = argument;
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
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