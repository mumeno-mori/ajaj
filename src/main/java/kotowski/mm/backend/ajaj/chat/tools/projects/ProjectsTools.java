package kotowski.mm.backend.ajaj.chat.tools.projects;

import kotowski.mm.backend.ajaj.chat.tools.AiToolPack;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectsTools implements AiToolPack {

    private final ProjectsProperties projectsProperties;

    @Tool(description = """
                Retrieving a list of projects, applications and it's specifications.
                Returns a JSON array of projects details:
                {
                  "projects": [
                    {
                      "id": "Project identifier",
                      "name": "Project name",
                      "apps": [
                        {
                          "id": "Application identifier",
                          "type": "backend|frontend",
                          "path": "path on drive",
                          "development-platform": "windows|linux|mac",
                          "watch": [
                            {
                              "path": "Path to watch",
                              "patterns": "File extensions"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
            """)
    ProjectsProperties getProjects(@ToolParam(description = "Drive or directory") String dir) {
        log.info("Returning projects properties");
        return projectsProperties;
    }

}