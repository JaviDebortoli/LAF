package Argumentation.LAF.Domain;

public abstract class KnowledgePiece {
    
    protected Double[] attributes;
    protected Double[] deltaAttributes;

    public Double[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Double[] attributes) {
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }
    
    public Double[] getDeltaAttributes() {
        return deltaAttributes;
    }

    public void setDeltaAttributes(Double[] deltaAttributes) {
        this.deltaAttributes = deltaAttributes;
    }

    @Override
    public abstract String toString();
    
}