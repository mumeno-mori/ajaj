package kotowski.mm.backend.ajaj.chat.rag;

import kotowski.mm.backend.ajaj.chat.rag.store.IndexedFilesStore;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScannerService {
    private final ProjectsProperties projectsProperties;
    private final IndexedFilesStore indexedFilesStore;

    @SneakyThrows
    @Transactional
    public void scanProjects() {
        indexedFilesStore.invalidateAll();
        for (var project : projectsProperties.getProjects()) {
            scanProject(project);
        }
    }

    private void scanProject(ProjectsProperties.Project project) throws IOException {
        var projectId = project.getId();
        for (var app : project.getApps()) {
            var appId = app.getId();
            for (var watchDir : app.getWatch()) {
                var rootPath = Path.of(app.getPath()).normalize().toAbsolutePath();
                scan(rootPath.toString(), watchDir.getPath(), watchDir.getPatterns())
                    .forEach(path -> {
                        var relative = rootPath.relativize(path).toString();
                        try {
                            var modifiedAt = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
                            indexedFilesStore.store(projectId, appId, relative, modifiedAt);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }
    }

    private static List<Path> scan(String rootPath, String subPathPattern, List<String> patterns) throws IOException {
        var recursive = subPathPattern.endsWith("**");
        var root = Paths.get(rootPath, patternToPath(subPathPattern));

        log.info("Scanning directory {} for pattern {} (recursion: {}).", rootPath, patterns, recursive);

        List<PathMatcher> matchers = patterns.stream()
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .toList();

        List<Path> matched = new ArrayList<>();

        try (var stream = recursive ? Files.walk(root) : Files.list(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> matchers.stream().anyMatch(m -> m.matches(path.getFileName())))
                    .forEach(matched::add);
        }

        return matched;
    }

    private static String patternToPath(String pattern) {
        var suffixLen = pattern.endsWith("**") ? 2 : pattern.endsWith("*") ? 1 : 0;
        if (suffixLen == 0) {
            return pattern;
        }
        return pattern.substring(0, pattern.length() - suffixLen);
    }
}
