package kotowski.mm.backend.ajaj.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
class AiToolConfiguration {

    @Bean
    List<ToolCallback> aiToolRegistry(List<AiToolPack> tools) {
        return tools.stream()
                .map(ToolCallbacks::from)
                .flatMap(it -> {
                    for (var toolCallback : it) {
                        log.info("Found AI tool: {}", toolCallback.getToolDefinition().name());
                    }
                    return Arrays.stream(it);
                })
                .toList();
    }

    @Bean
    public ToolExecutionExceptionProcessor unknownToolHandler() {
        return exception -> {
            String message = "The tool '" + exception.getToolDefinition().name() + "' execution failed. Error message: " + exception.getMessage();
            log.warn("Tool execution failed: {}", message);
            return message;
        };
    }
}
