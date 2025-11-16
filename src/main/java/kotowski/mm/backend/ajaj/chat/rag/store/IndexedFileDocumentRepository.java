package kotowski.mm.backend.ajaj.chat.rag.store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexedFileDocumentRepository extends JpaRepository<IndexedFileDocument, Long> {
}
