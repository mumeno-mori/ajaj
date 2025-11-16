package kotowski.mm.backend.ajaj.chat.rag.store;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kotowski.mm.backend.ajaj.infrastructure.data.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexedFileDocument extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "indexed_file_id")
    private IndexedFile file;
    private String documentId;
}
