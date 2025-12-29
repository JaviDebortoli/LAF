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
    private Double[] attributes;
    private Double[] deltaAttributes;

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

    public Double[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Double[] attributes) {
        this.attributes = attributes;
    }

    public Double[] getDeltaAttributes() {
        return deltaAttributes;
    }

    public void setDeltaAttributes(Double[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }
    
}
