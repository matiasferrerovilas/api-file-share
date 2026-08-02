package api.m2.file.record;

import api.m2.file.enums.FileType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record FileNode(
        String id,
        String name,
        FileType type,
        Long size,
        LocalDateTime lastModified,
        List<FileNode> children,
        List<String> shareWith
) {
}
