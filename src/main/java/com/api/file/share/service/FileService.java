package com.api.file.share.service;

import com.api.file.share.configuration.properties.StorageProperties;
import com.api.file.share.entity.FileEntity;
import com.api.file.share.entity.UserEntity;
import com.api.file.share.enums.FileType;
import com.api.file.share.exceptions.BusinessException;
import com.api.file.share.exceptions.EntityNotFoundException;
import com.api.file.share.exceptions.PermissionDeniedException;
import com.api.file.share.exceptions.ServiceException;
import com.api.file.share.record.DownloadableFile;
import com.api.file.share.record.FileNode;
import com.api.file.share.repository.FileRepository;
import com.api.file.share.repository.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageProperties storageProperties;
    private static final String ROOT_PATH = "Home";

    public FileNode getPersonalFolder(String ownerEmail) {
        UserEntity owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el usuario " + ownerEmail));

        List<FileEntity> files = fileRepository.findByOwnerId(owner.getId());
        Map<UUID, List<FileEntity>> childrenByParentId = files.stream()
                .filter(file -> file.getParentId() != null)
                .collect(Collectors.groupingBy(FileEntity::getParentId));

        List<FileNode> rootChildren = files.stream()
                .filter(file -> file.getParentId() == null)
                .sorted(Comparator.comparing(FileEntity::getName))
                .map(file -> toFileNode(file, childrenByParentId))
                .toList();

        return FileNode.builder()
                .id("root")
                .name(ROOT_PATH)
                .type(FileType.FOLDER)
                .lastModified(LocalDateTime.now())
                .children(rootChildren)
                .build();
    }

    private FileNode toFileNode(FileEntity file, Map<UUID, List<FileEntity>> childrenByParentId) {
        FileNode.FileNodeBuilder builder = FileNode.builder()
                .id(file.getId().toString())
                .name(file.getName())
                .type(file.getType())
                .size(file.getSize())
                .lastModified(file.getUpdatedAt());

        if (file.getType() == FileType.FOLDER) {
            List<FileNode> children = childrenByParentId
                    .getOrDefault(file.getId(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(FileEntity::getName))
                    .map(child -> toFileNode(child, childrenByParentId))
                    .toList();
            builder.children(children);
        }

        return builder.build();
    }

    public DownloadableFile downloadFile(UUID id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        if (file.getType() != FileType.FILE) {
            throw new BusinessException("No se puede descargar una carpeta");
        }

        Path basePath = Path.of(storageProperties.basePath()).normalize().toAbsolutePath();
        Path location = Path.of(file.getUbicacion()).normalize().toAbsolutePath();

        if (!location.startsWith(basePath)) {
            throw new ServiceException("La ubicación del archivo está fuera del directorio permitido");
        }

        if (!Files.isRegularFile(location)) {
            throw new EntityNotFoundException("El archivo no existe en el disco: " + location);
        }

        Resource resource;
        try {
            resource = new UrlResource(location.toUri());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo: " + location, e);
        }

        String contentType = resolveContentType(location);

        return DownloadableFile.builder()
                .resource(resource)
                .filename(file.getName())
                .contentType(contentType)
                .build();
    }

    private String resolveContentType(Path location) {
        try {
            String contentType = Files.probeContentType(location);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public FileNode uploadFile(String ownerEmail, UUID parentId, MultipartFile file) {
        UserEntity owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el usuario " + ownerEmail));

        Path targetDirectory = resolveTargetDirectory(parentId, owner);

        String filename = Path.of(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString();
        Path basePath = Path.of(storageProperties.basePath()).normalize().toAbsolutePath();
        Path target = targetDirectory.resolve(filename).normalize().toAbsolutePath();

        if (!target.startsWith(basePath)) {
            throw new ServiceException("La ubicación de destino está fuera del directorio permitido");
        }

        if (Files.exists(target)) {
            throw new BusinessException("Ya existe un archivo con el nombre '" + filename + "' en ese destino");
        }

        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo: " + target, e);
        }

        LocalDateTime now = LocalDateTime.now();
        FileEntity entity = new FileEntity();
        entity.setId(UUID.randomUUID());
        entity.setParentId(parentId);
        entity.setOwnerId(owner.getId());
        entity.setName(filename);
        entity.setType(FileType.FILE);
        entity.setSize(file.getSize());
        entity.setUbicacion(target.toString());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        fileRepository.save(entity);

        return FileNode.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .lastModified(now)
                .build();
    }

    private Path resolveTargetDirectory(UUID parentId, UserEntity owner) {
        if (parentId == null) {
            return Path.of(storageProperties.basePath());
        }

        FileEntity parent = fileRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la carpeta con id " + parentId));

        if (parent.getType() != FileType.FOLDER) {
            throw new BusinessException("El destino no es una carpeta");
        }

        if (!parent.getOwnerId().equals(owner.getId())) {
            throw new PermissionDeniedException("No tiene permisos sobre esta carpeta");
        }

        return Path.of(parent.getUbicacion());
    }
}
