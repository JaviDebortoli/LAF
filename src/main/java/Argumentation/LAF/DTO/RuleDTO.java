/*
 * 
 */
package Argumentation.LAF.DTO;

import java.util.List;

/**
 * @author JaviDebórtoli
 */
public class RuleDTO {
    
    private String headName;
    private List<String> bodyLiterals;
    private Double[] attributes;

    public String getHeadName() {
        return headName;
    }

    public void setHeadName(String headName) {
        this.headName = headName;
    }

    public List<String> getBodyLiterals() {
        return bodyLiterals;
    }

    public void setBodyLiterals(List<String> bodyLiterals) {
        this.bodyLiterals = bodyLiterals;
    }

    public Double[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Double[] attributes) {
        this.attributes = attributes;
    }
    
}
