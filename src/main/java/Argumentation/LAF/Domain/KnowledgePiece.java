package Argumentation.LAF.Domain;

public abstract class KnowledgePiece {
    
    protected String[] attributes;
    protected String[] deltaAttributes;

    public String[] getAttributes() {
        return attributes;
    }

    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }
    
    public String[] getDeltaAttributes() {
        return deltaAttributes;
    }

    public void setDeltaAttributes(String[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }

    @Override
    public abstract String toString();
    
}