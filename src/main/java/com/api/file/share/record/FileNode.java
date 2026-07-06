package com.api.file.share.record;

import com.api.file.share.enums.FileType;
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
        List<FileNode> children
) {
}
