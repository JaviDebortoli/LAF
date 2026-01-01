package Argumentation.LAF.Domain;

import java.util.List;

public class Rule extends KnowledgePiece{
    
    private final String head;
    private final List<String> body;

    public Rule(String head, List<String> body, String[] attributes) {
        this.head = head;
        this.body = body;
        this.attributes = attributes;
        this.deltaAttributes = attributes;
    }

    public String getHead() {
        return head;
    }

    public List<String> getBody() {
        return body;
    }

    @Override
    public String toString() {
        return head + "(X) :- " +
                body.toString().substring(1, body.toString().length() - 1) +
                "(X).";
    }
    
}