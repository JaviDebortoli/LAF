/*
 * 
 */
package Argumentation.LAF.DTO;

import java.util.Arrays;

/**
 * @author JaviDebórtoli
 */
public class FactDTO {
    
    private String name;     
    private String argument;   
    private Double[] attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public Double[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Double[] attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "FactDto{" +
                "name='" + name + '\'' +
                ", argument='" + argument + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }
        
}
