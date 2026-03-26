package Argumentation.LAF.Domain;

import java.util.List;
import java.util.Map;

/**
 * Immutable representation of the final argumentative graph produced
 * by the inference engine.
 *
 * <p>It contains two kinds of information:</p>
 * <ul>
 *   <li>{@link #edges}: derivation edges, connecting pieces of knowledge
 *       (facts or rules) with the facts they support or aggregate.</li>
 *   <li>{@link #conflictiveNodes}: pairs of facts in conflict, used to
 *       draw conflict (attack) edges in the visualization.</li>
 * </ul>
 *
 * <p>
 * This record is used as an internal data-transfer structure between
 * the {@code InferenceEngine} and the {@code GraphBuilderService}.
 * </p>
 *
 * @param edges             map from parent knowledge piece to list of
 *                          facts that are derived or affected by it
 * @param conflictiveNodes  list of conflicting fact pairs
 * 
 * @author JaviDebórtoli
 */
public record ArgumentativeGraph(
        Map<KnowledgePiece, List<Fact>> edges,
        List<PairInConflict> conflictiveNodes
) {}
