package api.m2.file.service;

import api.m2.file.clients.identity.response.UserMe;
import api.m2.file.configuration.properties.StorageProperties;
import api.m2.file.entity.FileEntity;
import api.m2.file.enums.FileType;
import api.m2.file.exceptions.BusinessException;
import api.m2.file.exceptions.EntityNotFoundException;
import api.m2.file.exceptions.PermissionDeniedException;
import api.m2.file.exceptions.ServiceException;
import api.m2.file.mappers.FileNodeMapper;
import api.m2.file.record.DownloadableFile;
import api.m2.file.record.FileNode;
import api.m2.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final StorageProperties storageProperties;
    private final FileNodeMapper fileNodeMapper;
    private final UserService userService;

    public FileNode getPersonalFolder() {
        var owner = userService.getMe();

        FileEntity root = fileRepository.findByOwnerIdAndParentIdIsNull(owner.id())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la carpeta raíz del usuario"));

        List<FileEntity> files = fileRepository.findByOwnerId(owner.id());

        var childrenByParentId = files.stream()
                .filter(file -> file.getParentId() != null)
                .collect(Collectors.groupingBy(FileEntity::getParentId));

        return fileNodeMapper.toFileNode(root, childrenByParentId);
    }


    public DownloadableFile downloadFile(Long id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        if (file.getType() != FileType.FILE) {
            throw new BusinessException("No se puede descargar una carpeta");
        }

        Path basePath = Path.of(storageProperties.basePath()).normalize().toAbsolutePath();
        Path location = Path.of(file.getLocation()).normalize().toAbsolutePath();

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

    public FileNode uploadFile(Long parentId, MultipartFile file) {
        var owner = userService.getMe();

        FileEntity parent = resolveParent(parentId, owner);
        Path targetDirectory = Path.of(parent.getLocation());

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
        FileEntity entity = FileEntity.builder()
                .parentId(parent.getId())
                .ownerId(owner.id())
                .workspaceId(12L)
                .name(filename)
                .type(FileType.FILE)
                .size(file.getSize())
                .location(target.toString())
                .createdAt(now)
                .updatedAt(now)
                .build();
        fileRepository.save(entity);

        return FileNode.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .lastModified(now)
                .build();
    }

    private FileEntity resolveParent(Long parentId, UserMe owner) {
        if (parentId == null) {
            return fileRepository.findByOwnerIdAndParentIdIsNull(owner.id())
                    .orElseThrow(() -> new EntityNotFoundException("No se encontró la carpeta raíz del usuario"));
        }

        FileEntity parent = fileRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la carpeta con id " + parentId));

        if (parent.getType() != FileType.FOLDER) {
            throw new BusinessException("El destino no es una carpeta");
        }

        if (!parent.getOwnerId().equals(owner.id())) {
            throw new PermissionDeniedException("No tiene permisos sobre esta carpeta");
        }

        return parent;
    }
}
