package api.m2.file.repository;

import api.m2.file.entity.FileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByOwnerId(Long ownerId);

    Optional<FileEntity> findByOwnerIdAndParentIdIsNull(Long ownerId);
}
