package Argumentation.LAF.Controller;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.Service.AlgebraMapperService;
import Argumentation.LAF.Service.GraphBuilderService;
import Argumentation.LAF.Service.InferenceService;
import Argumentation.LAF.Service.ProgramMapperService;
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
    private final ProgramMapperService programMapperService;
    private final AlgebraMapperService algebraMapperService;
    private final InferenceService inferenceService;
    private final GraphBuilderService graphBuilderService;
    
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
                           GraphBuilderService graphBuilderService) {
        this.programMapperService = programMapperService;
        this.algebraMapperService = algebraMapperService;
        this.inferenceService = inferenceService;
        this.graphBuilderService = graphBuilderService;
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
    public ResponseEntity<GraphResponse> buildGraph(@RequestBody GraphRequest request) {

        var facts = programMapperService.mapFacts(request.getFacts());
        var rules = programMapperService.mapRules(request.getRules());
        var operations = algebraMapperService.mapOperations(request.getOperations());

        var argumentativeGraph = inferenceService.buildGraph(facts, rules, operations);
        var response = graphBuilderService.toGraphResponse(argumentativeGraph);

        return ResponseEntity.ok(response);
    }
}
