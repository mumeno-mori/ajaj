package kotowski.mm.backend.ajaj.chat.rag;

import kotowski.mm.backend.ajaj.chat.rag.store.IndexedFilesStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {
    private final VectorStore vectorStore;
    private final FileScannerService fileScannerService;
    private final IndexedFilesStore indexedFilesStore;
    private final RagDataTransformer ragDataTransformer;
    private final RagBuilder ragBuilder;

    @Value("${app.ai.rag.context-results}")
    private int ragContextResults;

    @Value("${app.ai.rag.similarity-threshold}")
    private double similiarityThreshold;

    public void buildDb() {
        try {
            fileScannerService.scanProjects();
            indexedFilesStore.getAllModifiedFiles().forEach(ragBuilder::describeFile);
        } catch (Exception e) {
            log.warn("Error refreshing RAG DB", e);
        }
    }

    public String getInitialContextForQuestion(String question) {
        var ragQuestion = ragDataTransformer.transformQuestion(question);
        var docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(ragQuestion)
                .topK(ragContextResults)
                .similarityThreshold(similiarityThreshold)
                .build());
        if (docs.isEmpty()) {
            return "";
        }
        return docs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n---"));

    }

}
