/*
* Endpoints REST
*/
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

@RestController
@RequestMapping("/api")
public class GraphController {

    private final ProgramLoaderService programLoaderService;
    private final AlgebraService algebraService;
    private final InferenceService inferenceService;
    private final GraphBuilderService graphBuilderService;

    public GraphController(ProgramLoaderService programLoaderService, AlgebraService algebraService, InferenceService inferenceService, GraphBuilderService graphBuilderService) {
        this.programLoaderService = programLoaderService;
        this.algebraService = algebraService;
        this.inferenceService = inferenceService;
        this.graphBuilderService = graphBuilderService;
    }

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
