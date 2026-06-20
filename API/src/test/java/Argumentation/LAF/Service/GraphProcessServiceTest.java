package Argumentation.LAF.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.DTO.Request.OperationInputRequest;
import Argumentation.LAF.DTO.Response.GraphResponse;
import Argumentation.LAF.DTO.Response.NarrativeTraceResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraphProcessServiceTest {
    @Mock
    private ProgramMapperService programMapperService;

    @Mock
    private AlgebraMapperService algebraMapperService;

    @Mock
    private InferenceService inferenceService;

    @Mock
    private GraphBuilderService graphBuilderService;

    @Mock
    private NarrativeTraceBuilderService narrativeTraceBuilderService;

    @Mock
    private LlmNarrativeService llmNarrativeService;

    private GraphProcessService graphProcessService;

    @BeforeEach
    void setUp() {
        graphProcessService = new GraphProcessService(
                programMapperService,
                algebraMapperService,
                inferenceService,
                graphBuilderService,
                narrativeTraceBuilderService,
                llmNarrativeService);
    }

    @Test
    void shouldSkipExplainabilityWhenDisabled() {
        GraphRequest request = buildRequest(false);
        GraphResponse graphResponse = buildGraphResponse();

        given(programMapperService.mapFacts(anyList())).willReturn(List.of());
        given(programMapperService.mapRules(anyList())).willReturn(List.of());
        given(algebraMapperService.mapOperations(any())).willReturn(Map.of());
        given(inferenceService.buildGraph(anyList(), anyList(), anyMap())).willReturn(null);
        given(graphBuilderService.toGraphResponse(any())).willReturn(graphResponse);

        var response = graphProcessService.process(request);

        assertNotNull(response.getGraph());
        assertNotNull(response.getExplainability());
        assertEquals("disabled", response.getExplainability().getStatus());
        assertEquals("Explainability is disabled for this run.", response.getExplainability().getMessage());
        assertNull(response.getTrace());
        assertNull(response.getNarrative());
        assertNull(response.getMeta());
        verify(narrativeTraceBuilderService, never()).build(any());
        verify(llmNarrativeService, never()).generateNarrative(any());
    }

    @Test
    void shouldReturnGraphWhenExplainabilityServiceIsUnavailable() {
        GraphRequest request = buildRequest(true);
        GraphResponse graphResponse = buildGraphResponse();
        NarrativeTraceResponse trace = new NarrativeTraceResponse();

        given(programMapperService.mapFacts(anyList())).willReturn(List.of());
        given(programMapperService.mapRules(anyList())).willReturn(List.of());
        given(algebraMapperService.mapOperations(any())).willReturn(Map.of());
        given(inferenceService.buildGraph(anyList(), anyList(), anyMap())).willReturn(null);
        given(graphBuilderService.toGraphResponse(any())).willReturn(graphResponse);
        given(narrativeTraceBuilderService.build(graphResponse)).willReturn(trace);
        given(llmNarrativeService.generateNarrative(trace))
                .willThrow(new NarrativeServiceUnavailableException(
                        LlmNarrativeService.TEMPORARILY_UNAVAILABLE_MESSAGE));

        var response = graphProcessService.process(request);

        assertNotNull(response.getGraph());
        assertNotNull(response.getTrace());
        assertNotNull(response.getExplainability());
        assertEquals("unavailable", response.getExplainability().getStatus());
        assertEquals(LlmNarrativeService.TEMPORARILY_UNAVAILABLE_MESSAGE, response.getExplainability().getMessage());
        assertNull(response.getNarrative());
        assertNull(response.getMeta());
    }

    @Test
    void shouldReturnNarrativeWhenExplainabilitySucceeds() {
        GraphRequest request = buildRequest(true);
        GraphResponse graphResponse = buildGraphResponse();
        NarrativeTraceResponse trace = new NarrativeTraceResponse();

        given(programMapperService.mapFacts(anyList())).willReturn(List.of());
        given(programMapperService.mapRules(anyList())).willReturn(List.of());
        given(algebraMapperService.mapOperations(any())).willReturn(Map.of());
        given(inferenceService.buildGraph(anyList(), anyList(), anyMap())).willReturn(null);
        given(graphBuilderService.toGraphResponse(any())).willReturn(graphResponse);
        given(narrativeTraceBuilderService.build(graphResponse)).willReturn(trace);
        given(llmNarrativeService.generateNarrative(trace))
                .willReturn(new LlmNarrativeService.GenerationResult("narrative text", "gpt-4o-mini", "narrative-v1"));

        var response = graphProcessService.process(request);

        assertNotNull(response.getGraph());
        assertNotNull(response.getTrace());
        assertNotNull(response.getMeta());
        assertNotNull(response.getExplainability());
        assertEquals("ok", response.getExplainability().getStatus());
        assertEquals("narrative text", response.getNarrative());
        assertEquals("gpt-4o-mini", response.getMeta().getModel());
    }

    private GraphRequest buildRequest(boolean explainabilityEnabled) {
        GraphRequest request = new GraphRequest();
        request.setFacts(List.of());
        request.setRules(List.of());
        request.setOperations(new OperationInputRequest());
        request.setExplainabilityEnabled(explainabilityEnabled);
        return request;
    }

    private GraphResponse buildGraphResponse() {
        GraphResponse graphResponse = new GraphResponse();
        graphResponse.setNodes(List.of());
        graphResponse.setEdges(List.of());
        return graphResponse;
    }
}
