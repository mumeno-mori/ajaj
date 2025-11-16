package kotowski.mm.backend.ajaj.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class ProjectsProperties {
    private List<Project> projects;

    public Set<String> getRootDirectories() {
        return projects.stream()
                .flatMap(project -> project.getApps().stream())
                .map(ProjectApp::getPath)
                .collect(Collectors.toSet());
    }

    public String getContext() {
        return projects.stream()
                .map(project -> this.getProjectAndAppContext(project, null))
                .collect(Collectors.joining());

    }


    public String getProjectAndAppContext(String appId) {
        return projects.stream()
                .filter(it -> it.getApps().stream().anyMatch(app -> app.getId().equals(appId)))
                .map(project -> getProjectAndAppContext(project, appId))
                .findFirst()
                .orElse(null);

    }

    private String getProjectAndAppContext(Project project, String appId) {
        return """
                === Project ===
                Project id: %s,
                Project name: %s
                Apps:
                %s
            """.formatted(
                project.getId(),
                project.getName(),
                project.getApps().stream()
                    .filter(it -> appId == null || it.getId().equals(appId))
                    .map(this::getAppContext)
                    .collect(Collectors.joining())
            );
    }

    private String getAppContext(ProjectApp app) {
        return """
                ---- App ----
                App id: %s,
                App type: %s,
                App path: %s,
                Development platform: %s,
            """.formatted(
                app.getId(),
                app.getType(),
                app.getPath(),
                app.getDevelopmentPlatform()
            );
    }

    public enum AppType {
        FRONTEND,
        BACKEND
    }

    public enum DevelopmentPlatform {
        WINDOWS
    }

    @Data
    public static class Project {
        private String id;
        private String name;
        private List<ProjectApp> apps;
    }

    @Data
    public static class ProjectApp {
        private String id;
        private AppType type;
        private String path;
        private DevelopmentPlatform developmentPlatform;
        private List<WatchDirectory> watch;
    }

    @Data
    public static class WatchDirectory {
        private String path;
        private List<String> patterns;
    }
}