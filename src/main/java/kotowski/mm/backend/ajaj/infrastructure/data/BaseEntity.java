package kotowski.mm.backend.ajaj.infrastructure.data;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();
}
