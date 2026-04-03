package Argumentation.LAF.Controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Argumentation.LAF.DTO.Request.GraphRequest;
import Argumentation.LAF.Service.AlgebraMapperService;
import Argumentation.LAF.Service.GraphBuilderService;
import Argumentation.LAF.Service.GraphProcessService;
import Argumentation.LAF.Service.InferenceService;
import Argumentation.LAF.Service.NarrativeServiceUnavailableException;
import Argumentation.LAF.Service.ProgramMapperService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GraphController.class)
@Import(GlobalExceptionHandler.class)
class GraphProcessControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgramMapperService programMapperService;

    @MockitoBean
    private AlgebraMapperService algebraMapperService;

    @MockitoBean
    private InferenceService inferenceService;

    @MockitoBean
    private GraphBuilderService graphBuilderService;

    @MockitoBean
    private GraphProcessService graphProcessService;

    @Test
    void shouldReturnServiceUnavailableWhenLlmIsNotConfigured() throws Exception {
        given(graphProcessService.process(any(GraphRequest.class)))
                .willThrow(new NarrativeServiceUnavailableException(
                        "Narrative generation service is temporarily unavailable."));

        mockMvc.perform(post("/api/graph/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Narrative generation service is temporarily unavailable."))
                .andExpect(jsonPath("$.path").value("/api/graph/process"));
    }

    @Test
    void shouldReturnBadRequestWhenFactsListIsEmpty() throws Exception {
        mockMvc.perform(post("/api/graph/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInvalidRequestJsonWithEmptyFacts()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", Matchers.containsString("facts")))
                .andExpect(jsonPath("$.path").value("/api/graph/process"));

        verifyNoInteractions(graphProcessService);
    }

    @Test
    void shouldReturnBadRequestForGraphEndpointWhenFactsListIsEmpty() throws Exception {
        mockMvc.perform(post("/api/graph")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildInvalidRequestJsonWithEmptyFacts()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", Matchers.containsString("facts")))
                .andExpect(jsonPath("$.path").value("/api/graph"));

        verifyNoInteractions(programMapperService, algebraMapperService, inferenceService, graphBuilderService);
    }

    private String buildRequestJson() {
        return """
                {
                  "facts": [
                    {"name":"basicServices","argument":"houseA","attributes":["0.75","0.95"],"sourceKey":"FACT|basicServices|houseA"},
                    {"name":"gangOperate","argument":"houseA","attributes":["0.5","1.0"],"sourceKey":"FACT|gangOperate|houseA"}
                  ],
                  "rules": [
                    {"headName":"goodArea","bodyLiterals":["basicServices"],"attributes":["0.85","0.95"],"sourceKey":"RULE|goodArea|basicServices"},
                    {"headName":"~goodArea","bodyLiterals":["gangOperate"],"attributes":["0.5","0.8"],"sourceKey":"RULE|~goodArea|gangOperate"}
                  ],
                  "operations": {
                    "labels": [
                      {"labelName":"label_1","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"},
                      {"labelName":"label_2","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"}
                    ]
                  }
                }
                """;
    }

    private String buildInvalidRequestJsonWithEmptyFacts() {
        return """
                {
                  "facts": [],
                  "rules": [
                    {"headName":"goodArea","bodyLiterals":["basicServices"],"attributes":["0.85","0.95"],"sourceKey":"RULE|goodArea|basicServices"}
                  ],
                  "operations": {
                    "labels": [
                      {"labelName":"label_1","supportFunction":"X + Y","aggregationFunction":"X + Y","conflictFunction":"max(X-Y,0)"}
                    ]
                  }
                }
                """;
    }
}
