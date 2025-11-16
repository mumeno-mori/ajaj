package kotowski.mm.backend.ajaj.chat.tools.files;

import jakarta.annotation.PostConstruct;
import kotowski.mm.backend.ajaj.chat.tools.AiToolPack;
import kotowski.mm.backend.ajaj.chat.tools.GenericToolResponse;
import kotowski.mm.backend.ajaj.infrastructure.config.ProjectsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTools implements AiToolPack {

    private final ProjectsProperties projectsProperties;

    private Set<Path> rootDirectories;

    @Tool(description = """
                Retrieving a list of files from a specified directory.
                Returns info objects with the following structure:
                {
                        "success": "boolean value, that indicates if the operation was successful",
                        "errorMessage": "error message in case of failure, null otherwise",
                        "data": [
                              {
                                "name": "file name with extension",
                                "path": "absolute file path",
                                "size": "file size in bytes, value -1 means that the file size could not be retrieved.",
                                "dir": "is directory"
                              }
                        ]
                }
            """)
    GenericToolResponse<List<FileInfo>> getFileInfos(@ToolParam(description = "Drive or directory") String dir) {
        try {
            log.info("Start listing files from directory {}", dir);
            var dirPath = Path.of(dir);
            ensureWithinAllowed(dirPath);
            var data = Files.list(dirPath)
                    .map(path -> {
                        FileInfo info = new FileInfo();
                        info.setName(path.getFileName().toString());
                        info.setPath(path.toAbsolutePath().toString());
                        if (Files.isDirectory(path)) {
                            info.setSize(-1);
                        } else {
                            try {
                                info.setSize(Files.size(path));
                            } catch (Exception e) {
                                log.warn("Could not read file size for {}", path, e);
                                info.setSize(-1);
                            }
                        }
                        info.setDir(path.toFile().isDirectory());
                        return info;
                    })
                    .toList();
            return new GenericToolResponse<>(true, null, data);
        } catch (IOException e) {
            log.error("Error listing files from directory {}", dir, e);
            return new GenericToolResponse<>(false, e.getMessage(), null);
        }
    }

    @Tool(description = """
            Reads the content of a local file from the file system.
            
            Use this tool when the user asks to read, analyze, summarize,\s
            or inspect the contents of a specific file.
            
            ---
            ### Parameters:
            - `filePath` *(string)* — the absolute path to the file on the local machine.
              The path must include the full directory and filename with extension,\s
              for example:
              - `D:\\\\Projects\\\\MercatoMagic\\\\src\\\\main\\\\java\\\\com\\\\example\\\\FileService.java`
              - `/home/user/project/config/application.yaml`
            
            ---
            ### Behavior:
            - The tool reads the file content as plain UTF-8 text.
            - If the file cannot be found or opened, the tool returns an error message instead of content.
            - It is designed to read text files only — source code, configuration files, documentation, etc.
            - The tool **does not modify** or delete any files.
            - Use this tool only when you need to view or analyze the contents of a specific file.
            
            ---
            ### Returns:
            Returns info objects with the following structure:
            {
                    "success": "boolean value, that indicates if the operation was successful",
                    "errorMessage": "error message in case of failure, null otherwise",
                    "data": "A string containing the entire file content.
            }
            """)
    GenericToolResponse<String> getFileContent(@ToolParam(description = "The absolute path to the file on the local machine") String filePath) {
        try {
            log.info("Reading file content: {}", filePath);
            final var path = Path.of(filePath);
            ensureWithinAllowed(path);
            var data = Files.readString(path);
            return new GenericToolResponse<>(true, null, data);
        } catch (IOException e) {
            log.error("Error retrieving file content from {}", filePath, e);
            return new GenericToolResponse<>(false, null, null);
        }
    }

    @Tool(description = """
                Retrieving a list of root directories for getFileInfos tool. Root directory is also a project application directory.
                There may be many applications for one project. And may be only one root directory for each application.
                This is a list of directories that are accessible, as well as each of their subdirectories.
                Should be used to validate input parameters for getFileInfos tool.
                Returns list of directory paths:
                [
                    "absolute path to root directory 1",
                    "absolute path to root directory 2"
                ]
            """)
    Set<String> getRootDirectories() {
        log.info("Returning list of root directories");
        return rootDirectories.stream()
                .map(Path::toString)
                .collect(Collectors.toSet());
    }

    @Tool(description = """
                Creates a new file or overwrites an existing one with the provided content.
                Use this tool when the user explicitly asks to create, update, or modify a file.
            
                ---
                ### Parameters:
                - `filePath` *(string)* — absolute path of the file to write to, including its name and extension.
                  Example:
                    - D:\\Projects\\MercatoMagic\\src\\main\\java\\com\\example\\TestService.java
                    - /home/user/projects/config/app.yaml
                - `content` *(string)* — text to be written into the file.
            
                ---
                ### Behavior:
                - If the file does not exist, it will be created along with any missing directories.
                - If the file exists, it will be **overwritten** with the new content.
                - The content is written as UTF-8 text.
                - This tool cannot modify files outside the allowed project directory.
                - Use this tool only for writing text files such as source code, configuration, or documentation.
            
                ---
                ### Returns:
                A confirmation message indicating success or error details.
            """)
    public String writeFile(
            @ToolParam(description = "Absolute path to the file to be created or overwritten.") String filePath,
            @ToolParam(description = "Text content to be written to the file.") String content) {
        log.info("Writing file: {}", filePath);
        try {
            Path targetPath = Paths.get(filePath).toAbsolutePath();
            ensureWithinAllowed(targetPath);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            return "File saved successfully.";
        } catch (IOException e) {
            log.error("Error writing file: {}", filePath, e);
            return "Error writing file: " + e.getMessage();
        }
    }

    @PostConstruct
    void registerRootDirectories() {
        rootDirectories = projectsProperties.getRootDirectories().stream()
                .map(Path::of)
                .map(Path::normalize)
                .collect(Collectors.toSet());
    }

    private void ensureWithinAllowed(Path requestedPath) {
        var normalized = requestedPath.toAbsolutePath().normalize();
        if (rootDirectories.stream().noneMatch(normalized::startsWith)) {
            throw new SecurityException("Root directory is not allowed: " + normalized);
        }
        if (Files.isSymbolicLink(requestedPath)) {
            throw new SecurityException("Symbolic links are not allowed: " + normalized);
        }
    }

}