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
    private String[] attributes;

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

    public String[] getAttributes() {
        return attributes;
    }

    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }
    
}
