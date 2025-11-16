package kotowski.mm.backend.ajaj.chat.rag;

import kotowski.mm.backend.ajaj.chat.rag.store.IndexedFile;
import kotowski.mm.backend.ajaj.chat.rag.store.IndexedFileDocument;
import kotowski.mm.backend.ajaj.chat.rag.store.IndexedFilesStore;
import kotowski.mm.backend.ajaj.infrastructure.bench.LogExecutionTime;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagBuilder {

    private final VectorStore vectorStore;
    private final IndexedFilesStore indexedFilesStore;
    private final RagDataTransformer ragDataTransformer;
    private final ProjectsProperties projectsProperties;

    @Transactional
    public void describeFile(UUID indexedFileId) {
        try {
            var indexedFile = indexedFilesStore.findById(indexedFileId);
            var path = indexedFilesStore.getPath(indexedFile);
            var singleFileDocuments = getDocuments(indexedFile.getProjectId(), indexedFile.getAppId(), path);
            storeSingleFileDocuments(indexedFile, singleFileDocuments);
        } catch (Exception e) {
            log.error("Error describing file with id: {}", indexedFileId, e);
        }
    }

    @SneakyThrows
    @LogExecutionTime
    private List<Document> getDocuments(String projectId, String appId, Path path) {
        log.info("Indexing file: " + path);
        var content = Files.readString(path);
        // Prepare metadata
        var metadata = new HashMap<String, Object>();
        metadata.putAll(Map.of(
                "filepath", path.toString(),
                "projectId", projectId,
                "appId", appId,
                "contains", "source code"
        ));
        var projectContext = projectsProperties.getProjectAndAppContext(appId);
        ragDataTransformer.getMetadataForSourceCode(projectContext, path.toString() , content).forEach(
                (key, value) -> metadata.merge(
                        key,
                        value,
                        (oldVal, newVal) -> {
                            if (oldVal.equals(newVal)) {
                                return oldVal;
                            }
                            return oldVal + "," + newVal;
                        }));
        // Splitting
        var splitter = new TokenTextSplitter();
        var splitted = splitter.split(new Document(content, metadata));
        for (var i = 0; i < splitted.size(); i++) {
            splitted.get(i).getMetadata().put("chunkNumber", i);
        }
        return splitted;
    }

    private void storeSingleFileDocuments(IndexedFile indexedFile, List<Document> documents) {
        if (indexedFile.getModifiedAt() == null) {
            var docIds = indexedFile.getDocuments().stream()
                    .map(IndexedFileDocument::getDocumentId)
                    .toList();
            vectorStore.delete(docIds);
            indexedFilesStore.delete(indexedFile);
            return;
        }
        indexedFile.setModifiedAtStored(indexedFile.getModifiedAt());
        indexedFile.getDocuments().clear();
        indexedFile.getDocuments().addAll(documents.stream()
                .map(doc -> IndexedFileDocument.builder()
                        .file(indexedFile)
                        .documentId(doc.getId())
                        .build())
                .toList());
        vectorStore.add(documents);
        indexedFilesStore.store(indexedFile);
        documents.clear();
    }
}
