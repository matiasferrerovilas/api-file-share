package api.m2.file.repository;

import api.m2.file.entity.AppFileShare;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppFileShareRepository extends JpaRepository<AppFileShare, Long> {
    List<AppFileShare> findByFileId(Long fileId);

    List<AppFileShare> findByFileIdIn(List<Long> fileIds);

    Optional<AppFileShare> findByFileIdAndApiName(Long fileId, String apiName);

    boolean existsByFileIdAndApiName(Long fileId, String apiName);
}
