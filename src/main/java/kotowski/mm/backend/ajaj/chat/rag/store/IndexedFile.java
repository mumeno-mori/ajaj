package kotowski.mm.backend.ajaj.chat.rag.store;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import kotowski.mm.backend.ajaj.infrastructure.data.BaseEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexedFile extends BaseEntity {
    String projectId;
    String appId;
    String path;
    LocalDateTime modifiedAt;
    @Setter
    LocalDateTime modifiedAtStored;
    @Column(name = "is_modified", insertable = false, updatable = false)
    boolean isModified = false;
    @OneToMany(mappedBy = "file", orphanRemoval = true, cascade = CascadeType.ALL)
    List<IndexedFileDocument> documents;
}
