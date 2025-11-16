package kotowski.mm.backend.ajaj.chat.tools.files;

import lombok.Data;

@Data
public class FileInfo {
    private String name;
    private String path;
    private long size;
    private boolean dir;
}
