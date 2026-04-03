package Argumentation.LAF.Controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldHandleMethodArgumentNotValidAsBadRequest() throws Exception {
        mockMvc.perform(post("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("facts: must not be null"))
                .andExpect(jsonPath("$.path").value("/test/validation"));
    }

    @Test
    void shouldHandleIllegalArgumentAsBadRequest() throws Exception {
        mockMvc.perform(post("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("invalid graph input"))
                .andExpect(jsonPath("$.path").value("/test/illegal-argument"));
    }

    @Test
    void shouldHandleUnexpectedExceptionsAsInternalServerError() throws Exception {
        mockMvc.perform(post("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."))
                .andExpect(jsonPath("$.path").value("/test/generic"));
    }

    @RestController
    @RequestMapping("/test")
    static class ThrowingController {
        @PostMapping("/validation")
        void validation() throws NoSuchMethodException, MethodArgumentNotValidException {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "graphRequest");
            bindingResult.addError(new FieldError("graphRequest", "facts", "must not be null"));
            MethodParameter parameter = new MethodParameter(
                    ThrowingController.class.getDeclaredMethod("sampleMethod", String.class),
                    0);
            throw new MethodArgumentNotValidException(parameter, bindingResult);
        }

        @PostMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("invalid graph input");
        }

        @PostMapping("/generic")
        void generic() {
            throw new RuntimeException("sensitive detail");
        }

        @SuppressWarnings("unused")
        void sampleMethod(String value) {
        }
    }
}
