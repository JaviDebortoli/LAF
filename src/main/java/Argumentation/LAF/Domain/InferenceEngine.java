package Argumentation.LAF.Domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class InferenceEngine {
    
    private final Map<KnowledgePiece, List<Fact>> edges;
    private final List<Fact> facts;
    private final List<Rule> rules;
    private final String[][] functions;
    private final List<KnowledgePiece> removableEdges;
    private final List<Pair> conflictiveNodes;
    
    public InferenceEngine(List<Fact> facts, List<Rule> rules, String[][] functions) {
        this.edges = new HashMap<>();
        this.facts = facts;
        this.rules = rules;
        this.functions = functions;
        this.removableEdges = new ArrayList<>();
        this.conflictiveNodes = new ArrayList<>();
    }
    
    /**
     * Clase que se encarga de la generacion de la estructura que
     * representa las aristas de un grafo argumentativo
     * 
     * @return Estructura que representa las aristas del grafo en su atributo 
     * "edges" y los nodos que se contradicen en su atributo "conflictiveNodes"
     */
    public ArgumentativeGraph buildTree() {
        
        List<Fact> potentialFacts = new ArrayList<>();
        boolean anyNewFact;
        Fact newFact = null;
        int bodyPartsVerified;
        List<String> arguments = new ArrayList<>();
        
        // Se obtienen todos los argumentos diferentes de los hechos
        for (Fact fact : facts) {
            if (!arguments.contains(fact.getArgument())) {
                arguments.add(fact.getArgument());
            }
        }
        
        do {
            // Indica si se modifico el grafo y hay que repetir el ciclo
            anyNewFact = false; 

            for (String argument : arguments) { // Ciclo de argumentos
                for (Rule rule : rules) { // Ciclo de reglas

                    potentialFacts.clear();
                    bodyPartsVerified = 0;

                    for (String bodypart : rule.getBody()) { // Ciclo del cuerpo de cada regla
                        for (Fact fact : facts) { // Ciclo de hechos                       
                            
                            if ( bodypart.equals(fact.getName()) && fact.getArgument().equals(argument)) {    
                                // Nuevo hecho
                                newFact = new Fact(rule.getHead(), fact.getArgument(), null);
                                // Se cuenta el hecho dentro de los antecedentes de la regla
                                potentialFacts.add(fact);
                                bodyPartsVerified++;
                            }
                            
                        }
                    }

                    if ( bodyPartsVerified == rule.getBody().size() 
                            && !alreadyExists(newFact, rule) 
                            && !anyAggregation(newFact) ) {

                        addFact(potentialFacts, newFact, rule); // Añade un nuevo hecho 
                        anyNewFact = true; // Indica que hay que repetir el ciclo
                    } else if ( bodyPartsVerified == rule.getBody().size() 
                            && !alreadyExists(newFact, rule) 
                            && anyAggregation(newFact) ){

                        doAggregation(potentialFacts, newFact, rule); // Añade un hecho con agregación 
                        anyNewFact = true; // Indica que hay que repetir el ciclo
                    }
                }
            }
            
            
        } while (anyNewFact);
        
        conflict(); // Se resuelven los conflictos entre hechos
        
        return new ArgumentativeGraph(edges, conflictiveNodes);
    }
    
    /**
     * Crea las aristas para agregar el nuevo hecho
     * al grafo argumentativo
     * @param potentialFacts Lista de hechos potenciales que activan la regla
     * @param 
     */
    private void addFact (List<Fact> potentialFacts, Fact newFact, Rule rule) {
        
        // Calcular los valores las etiquetas del nuevo hecho 
        newFact.setAttributes( support (potentialFacts, rule) );
        
        // Añadir el nuevo hecho a la lista de hechos
        facts.add(newFact); 
        
        // Añadir la arista desde la regla al nuevo hecho
        if (!edges.containsKey(rule)) {
            edges.put(rule, new ArrayList<>());
        }
        edges.get(rule).add(newFact);

        // Añadir aristas desde los hechos que permitieron inferir el nuevo hecho
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new ArrayList<>());
            }
            edges.get(potentialFact).add(newFact);
        }
    }
    
    /**
     * Calcula el valor de los atributos de un hecho inferido
     * con la operacion definida para el soporte
     */
    private String[] support (List<Fact> potentialFacts, Rule rule) {
        String[] atributtes = new String[ potentialFacts.getFirst().getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            atributtes[i] = "0.0";
            // Reemplazar los valores de X y Y, y evaluar la funcion para cada uno de los antecedentes
            try {
                for (Fact fact : potentialFacts) {
                    expression = new ExpressionBuilder( functions[i][0] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(atributtes[i]))
                        .setVariable("Y", Double.parseDouble(fact.getAttributes()[i]));

                    atributtes[i] = String.valueOf(expression.evaluate());
                }
                // Reemplazar los valores de X y Y, y evaluar la funcion para la regla
                expression = new ExpressionBuilder( functions[i][0] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(atributtes[i]))
                        .setVariable("Y", Double.parseDouble(rule.getAttributes()[i]));

                atributtes[i] = String.valueOf(expression.evaluate());
                // Ubicar los valores en el intervalo [0, 1]
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                
                try {
                    union.clear();
                    
                    switch (functions[i][0]) {
                        case "Union" -> {
                            for (Fact fact : potentialFacts) {
                                union.add(fact.getAttributes()[i]);
                            }
                            union.add(rule.getAttributes()[i]);
                            atributtes[i] = String.join(" ", union);
                            break;
                        }
                        
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }
    
    /**
     * Determina si dos hechos son iguales
     */
    private boolean equalFacts (Fact firstFact, Fact secondFact) {
        return firstFact.getName().equals(secondFact.getName()) 
                && firstFact.getArgument().equals(secondFact.getArgument());
    } 
    
    /**
     * Determina si una inferencia en 
     * particular ya fue realizada
     */
    private boolean alreadyExists (Fact newFact, Rule rule) {
        boolean existsRule = false;

        for ( Map.Entry<KnowledgePiece, List<Fact>> edge : edges.entrySet() ) {
            for (Fact fact : edge.getValue()) {
                if ( equalFacts(fact, newFact) && edge.getKey() == rule ) {
                    existsRule = true;
                }
            }
        }
        
        return existsRule;
    }
    
    // Determina si existe agregacion cada vez que se infiere un nuevo hecho
    private boolean anyAggregation(Fact newFact) {
        boolean aggregation = false;

        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            KnowledgePiece key = entry.getKey();

            if (key instanceof Fact && equalFacts((Fact) key, newFact)) {
                aggregation = true;
                break;
            }

            for (Fact fact : entry.getValue()) {
                if (equalFacts(fact, newFact)) {
                    aggregation = true;
                    break;
                }
            }

            if (aggregation) {
                break;
            }
        }

        return aggregation;
    }

    // Realiza la agregación entre hechos, vuelve a construir el arbol
    private void doAggregation(List<Fact> potentialFacts, Fact newFact, Rule rule) {
        Fact auxFact = null;

        for (Fact fact : facts) {
            if (equalFacts(fact, newFact)) {
                auxFact = fact; // Se encuentra el hecho igual en la lista
                break;
            }
        }

        if (auxFact != null) {
            facts.remove(auxFact); // Se remueve el hecho igual de la lista hechos
        } else {
            auxFact = combineFacts(newFact);
        }

        newFact.setAttributes(support(potentialFacts, rule)); // Calcular los valores de la inferencia
        // Añadir la arista desde la regla al nuevo hecho
        if (!edges.containsKey(rule)) {
            edges.put(rule, new ArrayList<>());
        }
        edges.get(rule).add(newFact);

        // Añadir aristas desde los hechos que permitieron inferir el nuevo hecho
        for (Fact potentialFact : potentialFacts) {
            if (!edges.containsKey(potentialFact)) {
                edges.put(potentialFact, new ArrayList<>());
            }
            edges.get(potentialFact).add(newFact);
        }

        Fact aggregatedFact = new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(newFact, auxFact)); // Se calcula el hecho agregado
        // Se agrega el nuevo hecho a la lista
        facts.add(aggregatedFact);
        // Reconstruir el árbol para el nuevo hecho
        reBuilTree(aggregatedFact);
    }
    
    // Crea un nuevo hecho agregado a partir de hechos iguales en el grafo
    private Fact combineFacts (Fact newFact) {
        List<Fact> aggregatedFacts = new ArrayList<>();
        // Se buscan los hechos iguales el nuevo hecho que no estan en la lista    
        for (Map.Entry<KnowledgePiece, List<Fact>> edge : edges.entrySet()) { 
            if (edge.getKey() instanceof Fact && equalFacts( (Fact) edge.getKey(), newFact) ) {
                aggregatedFacts.add((Fact) edge.getKey());
            }
            
            for (Fact piece : edge.getValue()) {
                if ( equalFacts(piece, newFact) ) {
                    aggregatedFacts.add( piece );
                }
            }
        }
        // Se combina los hechos encontrados en un unico hecho agregado
        return new Fact(newFact.getName(), newFact.getArgument(), calculateAggregation(aggregatedFacts) );
    }
        
    // Reconstruye el grafo cada vez que se identifica una nueva agregacion
    private void reBuilTree (Fact newFact) {
        //Limpiar la lista de hechos a remover
        removableEdges.clear();
        
        Set<KnowledgePiece> newEdges = new HashSet<>(); // Nodos origen de las nuevas aristas hacia el nodo agregado
        // Se recorre el grafo buscando hechos iguales
        for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
            // Se buscan las aristas que se originen en los nodos agregados
            if ( entry.getKey() instanceof Fact && equalFacts( (Fact) entry.getKey(), newFact ) ) {
                
                newEdges.add(entry.getKey());
                removableEdges.add( (Fact) entry.getKey() );
                
                // Eliminar los nodos superiores la nueva agregacion
                eraseUpperNodes( entry.getValue() );
            } else {
                for (Fact fact : entry.getValue()) {
                    if ( equalFacts(fact, newFact) ) {
                        newEdges.add(fact);
                    }
                }
            }
        }
        // Se eliminan las aristas
        for (KnowledgePiece removableEdge : removableEdges) {
            edges.remove(removableEdge);
        }
        // Se agregan aristas desde los nodos agregados hacia el nuevo nodo
        for (KnowledgePiece edge : newEdges) {
            if (!edges.containsKey(edge)) {
                edges.put(edge, new ArrayList<>());
            }
            edges.get(edge).add(newFact);
        }
    }
    
    // Eliminar los nodos superiores dado un conjunto de nodos
    public void eraseUpperNodes (List<Fact> values) {        
        for (Fact value : values) {
            // Eliminar nodos superiores recursivamente
            if (edges.containsKey(value)) {
                eraseUpperNodes(edges.get(value));
            }
            // Eliminacion del nodo
            removableEdges.add(value); 
            // Eliminar aristas que tengan como destino el nodo eliminado
            for (Map.Entry<KnowledgePiece, List<Fact>> entry : edges.entrySet()) {
                for (Fact fact : entry.getValue()) {
                    if ( fact == value ) {
                        removableEdges.add(entry.getKey()); 
                    }
                }
            }
        } 
    }
    
    // Calcular los valores de los atributos cuando hay agregacion
    private String[] calculateAggregation(Fact newFact, Fact removableFact) {
        String[] atributtes = new String[ newFact.getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            try {
                atributtes[i] = "0.0";
                // Reemplazar las variables X y Y de la expresion
                expression = new ExpressionBuilder( functions[i][1] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(newFact.getAttributes()[i]))
                        .setVariable("Y", Double.parseDouble(removableFact.getAttributes()[i]));

                // Evaluar la expresion con los parametros actuales
                atributtes[i] = String.valueOf(expression.evaluate());
                // Ubicar los valores en el intervalo [0, 1]
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                    
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                
                try {
                    union.clear();
                    
                    switch (functions[i][1]) {
                        case "Union" -> {
                            union.addAll(Arrays.asList(newFact.getAttributes()[i].split(" ")));
                            union.addAll(Arrays.asList(removableFact.getAttributes()[i].split(" ")));
                            
                            atributtes[i] = String.join(" ", union);
                        }
                        
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }

    // Calcular los valores de los atributos cuando hay agregacion en hechos que no estan en la lista
    private String[] calculateAggregation(List<Fact> aggregatedFacts) {
        String[] atributtes = new String[ aggregatedFacts.getFirst().getAttributes().length ];
        Expression expression;
        LinkedHashSet<String> union = new LinkedHashSet<>();
        
        for (int i = 0; i < atributtes.length ; i++) {
            try {
                atributtes[i] = null;
            
                for (Fact fact : aggregatedFacts) {
                    if(atributtes[i] == null){
                        atributtes[i] = fact.getAttributes()[i];
                    } else {
                        // Reemplazar las variables X y Y de la expresion
                        expression = new ExpressionBuilder(functions[i][1])
                                .variables("X", "Y")
                                .build()
                                .setVariable("X", Double.parseDouble(atributtes[i]))
                                .setVariable("Y", Double.parseDouble(fact.getAttributes()[i]));

                        // Evaluar la expresion con los parametros actuales
                        atributtes[i] = String.valueOf(expression.evaluate());
                    }
                }
                // Ubicar los valores en el intervalo [0, 1]
                if (Double.parseDouble(atributtes[i])>1) {
                    atributtes[i] = "1.0";
                } else if (Double.parseDouble(atributtes[i])<0) {
                    atributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
                
                /*
                *
                * ONLY UNION SUPPORTED!!!!!!!!!!
                *
                */
                
                try {
                    union.clear();
                    
                    switch (functions[i][1]) {
                        case "Union" -> {
                            for (Fact fact : aggregatedFacts) {
                                union.addAll(Arrays.asList(fact.getAttributes()[i].split(" ")));
                            }
                            
                            atributtes[i] = String.join(" ", union);
                        }
                        
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            }
        }
        
        return atributtes;
    }
    
    // Trata conflictos entre hechos que se contradicen
    private void conflict() {
        List<Fact> negativeFacts = new ArrayList<>();
        String[] Attributte1;
        String[] Attributte2;
        
        // Capturar todos los hechos con una negación
        for (Fact fact : facts) {
            if (fact.getName().contains("~")) {
                negativeFacts.add(fact);
            }
        }

        // Recorrer todos los hechos con una negación
        for (Fact nf : negativeFacts) {
            // Recorrer todos los hechos y compararlos con los hechos negados
            for (Fact fact : facts) {
                if (nf.getName().replace("~", "").equals(fact.getName()) &&
                    nf.getArgument().equals(fact.getArgument())) {
                    
                    Attributte1 = calculateAttack(nf, fact);
                    Attributte2 = calculateAttack(fact, nf);
                    
                    nf.setDeltaAttributes(Attributte1);
                    fact.setDeltaAttributes(Attributte2);
                    
                    conflictiveNodes.add(new Pair(nf, fact));
                }
            }
        }
    }
    
    // Calcular valores de los atributos para los hechos en conflicto
    private String[] calculateAttack (Fact f1, Fact f2) {
        String[] attributtes = new String[f1.getAttributes().length]; // Array vacio
        Expression expression;
        LinkedHashSet<String> intersection1 = new LinkedHashSet<>();
        LinkedHashSet<String> intersection2 = new LinkedHashSet<>();
        
        for (int i = 0; i < attributtes.length; i++) { 
            try {
                // Reemplazar las variables X y Y de la expresion
                expression = new ExpressionBuilder( functions[i][2] )
                        .variables("X", "Y")
                        .build()
                        .setVariable("X", Double.parseDouble(f1.getAttributes()[i]))
                        .setVariable("Y", Double.parseDouble(f2.getAttributes()[i]));

                // Evaluar la expresion con los parametros actuales
                attributtes[i] = String.valueOf(expression.evaluate());
                // Ubicar los valores en el intervalo [0, 1]
                if (Double.parseDouble(attributtes[i])>1) {
                    attributtes[i] = "1.0";
                } else if (Double.parseDouble(attributtes[i])<0) {
                    attributtes[i] = "0.0";
                }
            } catch (IllegalArgumentException exception1) {
            
                /*
                *
                * ONLY INTERSECTION SUPPORTED!!!!!!!!!!
                *
                */
                
                try {
                    intersection1.clear();
                    intersection2.clear();
                    
                    switch (functions[i][2]) {
                        case "Intersection" -> {
                            intersection1.addAll(Arrays.asList(f1.getAttributes()[i].split(" ")));
                            intersection2.addAll(Arrays.asList(f2.getAttributes()[i].split(" ")));
                            
                            intersection1.retainAll(intersection2);
                            
                            attributtes[i] = String.join(" ", intersection1);
                        }
                        
                        /*
                        *
                        * TO DO: OTHER OPERATORS
                        *
                        */
                    
                    }
                } catch (Exception exception2) {
                    throw exception2;
                }
            
            }
        }
        
        return attributtes;
    }
}