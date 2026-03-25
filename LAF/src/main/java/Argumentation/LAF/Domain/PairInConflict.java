package Argumentation.LAF.Domain;

/**
 * Simple value object representing a pair of conflicting facts.
 * <p>
 * It is used in {@link ArgumentativeGraph#conflictiveNodes()} to
 * represent each symmetric conflict relation (p, ~p).
 * </p>
 * 
 * @author JaviDebórtoli
 */
public record PairInConflict(Fact first, Fact second) {}
