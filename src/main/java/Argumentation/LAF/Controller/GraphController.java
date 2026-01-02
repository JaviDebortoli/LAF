package Argumentation.LAF.Controller;

import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.Service.AlgebraService;
import Argumentation.LAF.Service.GraphBuilderService;
import Argumentation.LAF.Service.InferenceService;
import Argumentation.LAF.Service.ProgramLoaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for exposing the argumentation graph generation
 * functionality through HTTP endpoints.
 *
 * <p>
 * This controller acts as the main orchestration point between the different
 * application services involved in the construction of a Labeled Argumentation
 * Framework (LAF) graph. It retrieves the currently loaded facts and rules,
 * accesses the algebraic operations defined for each label, triggers the
 * inference process and finally builds a graph representation suitable for
 * client consumption.
 * </p>
 *
 * <p>
 * The controller does not perform any inference or graph construction logic
 * itself; instead, it delegates these responsibilities to the corresponding
 * services following a clear separation of concerns.
 * </p>
 *
 * <h3>Request Flow</h3>
 * <ol>
 *     <li>Retrieve facts and rules from {@link ProgramLoaderService}.</li>
 *     <li>Retrieve algebraic operations from {@link AlgebraService}.</li>
 *     <li>Build the argumentation graph using {@link InferenceService}.</li>
 *     <li>Transform the graph into a REST-ready response using
 *     {@link GraphBuilderService}.</li>
 * </ol>
 *
 * @see ProgramLoaderService
 * @see AlgebraService
 * @see InferenceService
 * @see GraphBuilderService
 * 
 * @author JaviDebórtoli
 */
@RestController
@RequestMapping("/api")
public class GraphController {
    /** Service responsible for loading and providing the current set of facts and rules defined by the user. */
    private final ProgramLoaderService programLoaderService; 
    /** Service responsible for managing and providing the algebraic operations associated with argument labels. */
    private final AlgebraService algebraService;
    /** Service responsible for building the internal argumentation graph by applying inference rules over facts and algebraic operations. */
    private final InferenceService inferenceService;
    /** Service responsible for transforming the internal graph representation into a response object suitable for REST communication. */
    private final GraphBuilderService graphBuilderService;

    /**
     * Creates a new {@code GraphController} with all required services injected.
     *
     * @param programLoaderService service providing access to the current facts
     *                             and rules
     * @param algebraService service providing the algebraic operations
     *                       associated with labels
     * @param inferenceService service responsible for generating the
     *                         argumentation graph
     * @param graphBuilderService service responsible for building the
     *                            REST response from the internal graph
     */
    public GraphController(ProgramLoaderService programLoaderService, AlgebraService algebraService, InferenceService inferenceService, GraphBuilderService graphBuilderService) {
        this.programLoaderService = programLoaderService;
        this.algebraService = algebraService;
        this.inferenceService = inferenceService;
        this.graphBuilderService = graphBuilderService;
    }
    
    /**
     * Returns the current argumentation graph generated from the loaded
     * program (facts and rules) and the configured algebraic operations.
     *
     * <p>
     * This endpoint builds the argumentation graph by invoking the inference
     * service and then converts it into a {@link GraphResponse} suitable for
     * visualization or further processing by the client.
     * </p>
     *
     * @return A {@link ResponseEntity} containing the generated
     *         {@link GraphResponse}
     */
    @GetMapping("/graph")
    public ResponseEntity<GraphResponse> getGraph() {
        var facts = programLoaderService.getCurrentFacts();
        var rules = programLoaderService.getCurrentRules();
        var operations = algebraService.getOperationsByLabel();
        var argumentativeGraph = inferenceService.buildGraph(facts, rules, operations);
        var response = graphBuilderService.toGraphResponse(argumentativeGraph);

        return ResponseEntity.ok(response);
    }
}
