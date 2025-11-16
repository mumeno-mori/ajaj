package kotowski.mm.backend.ajaj.chat.rag;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagDataTransformer {
    private final ChatClient ragChatClient;
    private final ProjectsProperties projectsProperties;
    private final ObjectMapper objectMapper;

    public String transformQuestion(String question) {
        return ragChatClient.prompt()
                .user("""
                        Your task is to prepare a query for Retrieval Augmented Generation (RAG),
                        based on a multilingual embedding model and a VectorStore, to retrieve the data necessary
                        to answer the question provided by the user. Respond with ONLY the optimized query, no explanations,
                        use keywords to search data for provided question. RAG contains source codes and documentation.
                        
                        You are a part of the system responsible for generating code for projects described by this context:
                        %s
                        The data in the RAG contains source codes and related metadata for those codes of the applications being built.
                        
                        QUESTION:
                        %s
                        """.formatted(projectsProperties.getContext(), question)
                )
                .call()
                .content();

    }

    @SneakyThrows
    public Map<String, String> getMetadataForSourceCode(String projectContext, String filename, String sourceCode) {
        var prompt = Prompt.builder()
                .content("""
                        You are an AI assistant responsible for generating structured metadata for a source code knowledge base used in a RAG (Retrieval-Augmented Generation) system.
                        
                        Each document represents a source code file.
                        The same file may be split into multiple chunks.
                        Metadata must therefore describe the ENTIRE file, not just chunk.
                        
                        Your goal is to produce consistent metadata that will allow all chunks belonging to the same file to be retrieved together and correctly ordered later.
                        
                        ---
                        
                        ### Rules
                        
                        1. Output **ONLY** a valid JSON array of objects, with no comments or explanations.
                        2. Each object must have two keys: "key" and "value".
                        3. Metadata must be **identical** across all chunks belonging to the same file.
                        4. Each chunk will automatically receive a "chunkNumber" field later — do not include it yourself, but assume it will exist.
                        5. Ensure the metadata enables RAG to retrieve and group all chunks of a file based on shared fields.
                        6. Keep all values short, factual, and consistent across chunks.
                        7. Use lowercase for all keys.
                        
                        ---
                        
                        ### List of keys, additional keys may be added if relevant, "responsibility" is mandatory, others are optional
                        
                        - "language" → e.g. "java", "dart", "yaml"
                        - "entities" → comma-separated names of classes, functions, or variables
                        - "package" → package/module path (or null)
                        - "responsibility" → short description of purpose (1 line)
                        - "dependencies" → comma-separated list of imports or libraries
                        - "annotations" → comma-separated list of annotations/decorators
                        - "type" → e.g. "controller", "service", "model", "widget", "config", "util"
                        
                        ---
                        
                        ### Output format example
                        
                        [
                          { "key": "language", "value": "java" },
                          { "key": "entities", "value": "FileStorageService, saveFile" },
                          { "key": "package", "value": "com.example.project.service" },
                          { "key": "responsibility", "value": "Handles file storage operations" },
                          { "key": "dependencies", "value": "org.springframework.stereotype.Service" },
                          { "key": "annotations", "value": "Service" },
                          { "key": "type", "value": "service" }
                        ]
                        
                        ---
                        
                        ### Instructions
                        
                        - The code snippet you will receive is a single file.
                        - Base your metadata on what you can infer about the entire file, not only this chunk.
                        - Do NOT include the source code or chunk number.
                        - Output only the JSON array shown above.
                        - If language or role cannot be confidently inferred, make your best guess.
                        
                        ---
                        
                        Now analyze the following source code that will be divided into CHUNKS and return ONLY the JSON array of metadata objects.
                        Avoid using characters that are not allowed in the JSON format.
                        Keys and values cannot contain null.
                        You must respond ONLY with valid JSON as described. Do not include explanations, markdown, or code fences. If you output anything else, it will break the system.
                        
                        ### CODE CONTEXT START
                        %s
                        ### CODE CONTEXT END
                        Source code filename: %s
                        ### SOURCE CODE TO ANALYZE START
                        %s
                        ### SOURCE CODE TO ANALYZE END
                        """.formatted(projectContext, filename, sourceCode)
                )
                .build();
        var content = ragChatClient.prompt(prompt)
                .call()
                .content();
        try {
            var keyValues = objectMapper.readValue(content, new TypeReference<List<KeyValue>>() {
            });
            return getMetadataMap(keyValues);
        } catch (JsonParseException e) {
            return tryAgainOrThrow(e, prompt, content);
        }
    }

    private Map<String, String> tryAgainOrThrow(JsonProcessingException e, Prompt prompt, String response) throws JsonProcessingException {
        try {
            return retry(prompt, response);
        } catch (Exception ne) {
            log.warn("Unable to generate metadata for source code.\nJSON:\n{}", response, ne);
            throw e;
        }
    }

    private Map<String, String> retry(Prompt prompt, String response) throws JsonProcessingException {
        var msgs = new ArrayList<Message>();
        msgs.add(prompt.getUserMessage());
        msgs.add(new AssistantMessage(response));
        msgs.add(new UserMessage("Your previous response was not valid JSON. Please correct it."));
        var newJson = ragChatClient.prompt()
                .messages(msgs)
                .call().content();
        var newKeyValues = objectMapper.readValue(newJson, new TypeReference<List<KeyValue>>() {
        });
        return getMetadataMap(newKeyValues);
    }

    private static Map<String, String> getMetadataMap(List<KeyValue> keyValues) {
        return keyValues.stream()
                .filter(keyValue -> keyValue.key() != null && keyValue.value() != null)
                .collect(Collectors.toMap(KeyValue::key, KeyValue::value, (v1, v2) -> v1 + ", " + v2));
    }
}
