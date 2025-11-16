package kotowski.mm.backend.ajaj.chat.rag.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

interface IndexedFileRepository extends JpaRepository<IndexedFile, UUID> {

    @Modifying
    @NativeQuery("""
            INSERT INTO public.indexed_file (project_id, app_id, path, modified_at)
            VALUES (:projectId, :appId, :path, :modifiedAt)
            ON CONFLICT (project_id, app_id, path)
            DO UPDATE SET
                modified_at = EXCLUDED.modified_at;""")
    void upsert(String projectId, String appId, String path, LocalDateTime modifiedAt);

    @Modifying
    @NativeQuery("""
            DELETE FROM public.indexed_file
            WHERE project_id = :projectId
              AND app_id = :appId
              AND path = :path;""")
    void delete(String projectId, String appId, String path);

    @Modifying
    @NativeQuery("""
            UPDATE public.indexed_file
            SET modified_at = NULL;""")
    void invalidateAll();

    @Query("select f.id from IndexedFile f where f.isModified")
    List<UUID> getAllModified();
}
