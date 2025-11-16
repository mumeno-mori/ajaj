package kotowski.mm.backend.ajaj.chat.tools.rag;

import kotowski.mm.backend.ajaj.chat.tools.AiToolPack;
import kotowski.mm.backend.ajaj.chat.tools.GenericToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagTools implements AiToolPack {
    private final VectorStore vectorStore;

    @Value("${app.ai.rag-tool.context-results}")
    private int ragContextResults;

    @Value("${app.ai.rag-tool.similarity-threshold}")
    private double similiarityThreshold;

    @Tool(description = """
            Retrieves the most relevant context information from the project's knowledge base (RAG)
            for a given user question, using semantic similarity search over vector embeddings.
            
            ---
            ### Purpose:
            Use this tool when you need additional background, code, documentation, or configuration context
            to accurately answer a user's question. It searches a local VectorStore (e.g., pgvector)
            that contains previously embedded project data such as source code, configuration files,
            documentation, or related notes.
            
            ---
            ### Parameters:
            - `query` *(string)* — the natural language question or topic for which you need relevant context.
              Examples:
                - "How is user authentication implemented?"
                - "Show how files are stored in the project."
                - "Where are API endpoints defined?"
            
            ---
            ### Behavior:
            - Performs a semantic similarity search in the vector database.
            - Retrieves the most relevant content chunks (e.g., code fragments, configs, docs).
            - Aggregates them into a single text block separated by `---` delimiters.
            - Returns this block as plain text.
            
            ---
            ### Returns:
            A JSON object with the following structure:
            {
              "success": true | false,
              "errorMessage": "optional string if an error occurred",
              "data": "text with relevant context chunks separated by ---"
            }
            
            If no matching data is found, the tool still returns success=true but with an empty `data` field.
            
            ---
            ### Example usage:
            - When a question refers to code, configuration, or internal logic of the system,
              call this tool first to gather context, then use it to answer the question accurately.
            """)
    public GenericToolResponse<String> getAnyContextData(@ToolParam(description = "The question to retrieve context for") String query) {
        try {
            log.info("Retrieving RAG context for question (tool): {}", query);
            var data = vectorStore.similaritySearch(query).stream()
                    .map(Document::getFormattedContent)
                    .collect(Collectors.joining("\n---\n"));
            return new GenericToolResponse<>(true, null, data);
        } catch (Exception e) {
            log.error("Error retrieving RAG context for question: {}", query, e);
            return new GenericToolResponse<>(false, "Error retrieving RAG context: " + e.getMessage(), null);
        }
    }
}
