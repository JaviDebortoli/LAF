package Argumentation.LAF.Domain;

public abstract class KnowledgePiece {
    
    protected Double[] attributes;

    public Double[] getAttributes() {
        return attributes;
    }

    public void setAttributes(Double[] attributes) {
        this.attributes = attributes;
    }

    @Override
    public abstract String toString();
    
}