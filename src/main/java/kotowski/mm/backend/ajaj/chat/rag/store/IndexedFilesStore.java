package kotowski.mm.backend.ajaj.chat.rag.store;

import jakarta.persistence.Index;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IndexedFilesStore {
    private final IndexedFileRepository repository;
    private final ProjectsProperties properties;
    public void invalidateAll() {
        repository.invalidateAll();
    }
    public void store(String projectId, String appId, String filePath, LocalDateTime modifiedAt) {
        repository.upsert(projectId, appId, filePath, modifiedAt);
    }
    public void delete(IndexedFile indexedFile) {
        repository.delete(indexedFile);
    }
    public List<UUID> getAllModifiedFiles() {
        return repository.getAllModified();
    }
    public Path getPath(IndexedFile indexedFile) {
        var rootPath = properties.getProjects().stream()
                .filter(project -> project.getId().equals(indexedFile.getProjectId()))
                .flatMap(project -> project.getApps().stream())
                .filter(app -> app.getId().equals(indexedFile.getAppId()))
                .map(ProjectsProperties.ProjectApp::getPath)
                .findFirst()
                .orElseThrow();
        return Path.of(rootPath, indexedFile.getPath()).normalize().toAbsolutePath();
    }
    public IndexedFile findById(UUID indexedFileId) {
        return repository.findById(indexedFileId).orElseThrow();
    }
    public void store(IndexedFile indexedFile) {
        repository.save(indexedFile);
    }
}
