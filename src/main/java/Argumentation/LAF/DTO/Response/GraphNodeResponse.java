/*
 * 
 */
package Argumentation.LAF.DTO.Response;

/**
 * @author JaviDebórtoli
 */
public class GraphNodeResponse {
    
    private String id;
    private String label;
    private String type;
    private String[] attributes;
    private String[] deltaAttributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }

    public String[] getDeltaAttributes() {
        return deltaAttributes;
    }

    public void setDeltaAttributes(String[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }
    
}
