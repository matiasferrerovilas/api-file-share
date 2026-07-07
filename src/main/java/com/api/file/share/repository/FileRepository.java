package com.api.file.share.repository;

import com.api.file.share.entity.FileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByOwnerId(Long ownerId);
}
