package com.api.file.share.mappers;

import com.api.file.share.entity.FileEntity;
import com.api.file.share.enums.FileType;
import com.api.file.share.record.FileNode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FileNodeMapper {

    default FileNode toFileNode(FileEntity file, Map<UUID, List<FileEntity>> childrenByParentId) {
        List<FileNode> children = file.getType() == FileType.FOLDER
                ? childrenByParentId.getOrDefault(file.getId(), List.of())
                        .stream()
                        .sorted(Comparator.comparing(FileEntity::getName))
                        .map(child -> toFileNode(child, childrenByParentId))
                        .toList()
                : null;

        return toFileNode(file, children);
    }

    @Mapping(target = "lastModified", source = "file.updatedAt")
    FileNode toFileNode(FileEntity file, List<FileNode> children);
}
