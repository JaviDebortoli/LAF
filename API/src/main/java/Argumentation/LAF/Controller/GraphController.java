package Argumentation.LAF.Controller;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.DTO.Response.GraphProcessResponse;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.Service.AlgebraMapperService;
import Argumentation.LAF.Service.GraphBuilderService;
import Argumentation.LAF.Service.GraphProcessService;
import Argumentation.LAF.Service.InferenceService;
import Argumentation.LAF.Service.ProgramMapperService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for generating argumentation graphs
 * using a stateless, request-driven approach.
 *
 * <p>
 * All information required to build the graph (facts, rules and
 * algebraic operations) is provided in a single request, ensuring
 * thread-safety, scalability and reproducibility.
 * </p>
 * 
 * @author JaviDebórtoli
 */
@RestController
@RequestMapping("/api")
public class GraphController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphController.class);

    private final ProgramMapperService programMapperService;
    private final AlgebraMapperService algebraMapperService;
    private final InferenceService inferenceService;
    private final GraphBuilderService graphBuilderService;
    private final GraphProcessService graphProcessService;
    
    /**
     * Constructs a {@code GraphController} with all required stateless services.
     *
     * @param programMapperService service responsible for mapping facts and rules
     * @param algebraMapperService service responsible for mapping algebraic operations
     * @param inferenceService service responsible for building the argumentation graph
     * @param graphBuilderService service responsible for serializing the graph
     */
    public GraphController(ProgramMapperService programMapperService,
                           AlgebraMapperService algebraMapperService,
                           InferenceService inferenceService,
                           GraphBuilderService graphBuilderService,
                           GraphProcessService graphProcessService) {
        this.programMapperService = programMapperService;
        this.algebraMapperService = algebraMapperService;
        this.inferenceService = inferenceService;
        this.graphBuilderService = graphBuilderService;
        this.graphProcessService = graphProcessService;
    }
    
    /**
     * Builds and returns an argumentation graph based on the data
     * provided in the request.
     *
     * <p>
     * This endpoint performs the complete workflow:
     * </p>
     * <ol>
     *     <li>Maps input DTOs into domain objects.</li>
     *     <li>Builds the internal argumentation graph.</li>
     *     <li>Transforms the graph into a response DTO.</li>
     * </ol>
     *
     * @param request the request containing facts, rules and algebraic operations
     * @return a {@link ResponseEntity} containing the generated {@link GraphResponse}
     */
    @PostMapping("/graph")
    public ResponseEntity<GraphResponse> buildGraph(@Valid @RequestBody GraphRequest request) {
        final String endpoint = "/api/graph";
        LOGGER.info(
                "Processing request endpoint={} factsCount={} rulesCount={}",
                endpoint,
                safeSize(request.getFacts()),
                safeSize(request.getRules()));
        try {
            var facts = programMapperService.mapFacts(request.getFacts());
            var rules = programMapperService.mapRules(request.getRules());
            var operations = algebraMapperService.mapOperations(request.getOperations());
            var argumentativeGraph = inferenceService.buildGraph(facts, rules, operations);
            var response = graphBuilderService.toGraphResponse(argumentativeGraph);

            LOGGER.info(
                    "Completed request endpoint={} nodesCount={} edgesCount={}",
                    endpoint,
                    safeSize(response.getNodes()),
                    safeSize(response.getEdges()));
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed request endpoint={} errorType={}",
                    endpoint,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    @PostMapping("/graph/process")
    public ResponseEntity<GraphProcessResponse> processGraphAndNarrative(@Valid @RequestBody GraphRequest request) {
        final String endpoint = "/api/graph/process";
        LOGGER.info(
                "Processing request endpoint={} factsCount={} rulesCount={}",
                endpoint,
                safeSize(request.getFacts()),
                safeSize(request.getRules()));
        try {
            GraphProcessResponse response = graphProcessService.process(request);
            GraphResponse graphResponse = response.getGraph();

            LOGGER.info(
                    "Completed request endpoint={} nodesCount={} edgesCount={} llmModel={}",
                    endpoint,
                    graphResponse != null ? safeSize(graphResponse.getNodes()) : 0,
                    graphResponse != null ? safeSize(graphResponse.getEdges()) : 0,
                    response.getMeta() != null ? response.getMeta().getModel() : "n/a");
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed request endpoint={} errorType={}",
                    endpoint,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private int safeSize(java.util.Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }
}
