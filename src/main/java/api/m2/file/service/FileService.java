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
import api.m2.file.service.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final String ROOT_PATH = "Home";

    private final FileRepository fileRepository;
    private final StorageProperties storageProperties;
    private final FileNodeMapper fileNodeMapper;
    private final UserService userService;
    private final WorkspaceService workspaceService;

    public FileNode getPersonalFolder(Long workspaceId) {
        var owner = userService.getMe();
        workspaceService.verifyUserIsMemberOfWorkspace(workspaceId, owner.id());

        FileEntity root = getOrCreateRoot(workspaceId, owner);

        List<FileEntity> files = fileRepository.findByWorkspaceId(workspaceId);

        var childrenByParentId = files.stream()
                .filter(file -> file.getParentId() != null)
                .collect(Collectors.groupingBy(FileEntity::getParentId));

        return fileNodeMapper.toFileNode(root, childrenByParentId);
    }

    private FileEntity getOrCreateRoot(Long workspaceId, UserMe owner) {
        return fileRepository.findByWorkspaceIdAndParentIdIsNull(workspaceId)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    FileEntity root = FileEntity.builder()
                            .ownerId(owner.id())
                            .workspaceId(workspaceId)
                            .name(ROOT_PATH)
                            .type(FileType.FOLDER)
                            .location("%s/%s".formatted(storageProperties.basePath(), workspaceId))
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return fileRepository.save(root);
                });
    }


    public DownloadableFile downloadFile(Long id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        workspaceService.verifyUserIsMemberOfWorkspace(file.getWorkspaceId(), userService.getMe().id());

        if (file.getType() != FileType.FILE) {
            throw new BusinessException("No se puede descargar una carpeta");
        }

        Path location = validateWithinBasePath(Path.of(file.getLocation()));

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

    public FileNode uploadFile(Long workspaceId, Long parentId, MultipartFile file) {
        var owner = userService.getMe();
        workspaceService.verifyUserIsMemberOfWorkspace(workspaceId, owner.id());

        FileEntity parent = resolveParent(workspaceId, parentId, owner);
        Path targetDirectory = Path.of(parent.getLocation());

        String filename = Path.of(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString();
        Path target = validateWithinBasePath(targetDirectory.resolve(filename));

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
                .workspaceId(workspaceId)
                .name(filename)
                .type(FileType.FILE)
                .size(file.getSize())
                .location(target.toString())
                .createdAt(now)
                .updatedAt(now)
                .build();
        fileRepository.save(entity);

        return toResponseNode(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public FileNode createFolder(Long workspaceId, Long parentId, String name) {
        var owner = userService.getMe();
        workspaceService.verifyUserIsMemberOfWorkspace(workspaceId, owner.id());

        FileEntity parent = resolveParent(workspaceId, parentId, owner);
        String folderName = Path.of(name).getFileName().toString();
        Path target = validateWithinBasePath(Path.of(parent.getLocation()).resolve(folderName));

        if (Files.exists(target)) {
            throw new BusinessException("Ya existe un archivo con el nombre '" + folderName + "' en ese destino");
        }

        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta: " + target, e);
        }

        LocalDateTime now = LocalDateTime.now();
        FileEntity entity = FileEntity.builder()
                .parentId(parent.getId())
                .ownerId(owner.id())
                .workspaceId(workspaceId)
                .name(folderName)
                .type(FileType.FOLDER)
                .location(target.toString())
                .createdAt(now)
                .updatedAt(now)
                .build();
        fileRepository.save(entity);

        return toResponseNode(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public FileNode renameNode(Long id, String name) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        workspaceService.verifyUserIsMemberOfWorkspace(entity.getWorkspaceId(), userService.getMe().id());

        if (entity.getParentId() == null) {
            throw new BusinessException("No se puede renombrar la carpeta raíz");
        }

        String newName = Path.of(name).getFileName().toString();
        Path oldLocation = Path.of(entity.getLocation());
        Path newLocation = validateWithinBasePath(oldLocation.resolveSibling(newName));

        if (Files.exists(newLocation)) {
            throw new BusinessException("Ya existe un archivo con el nombre '" + newName + "' en ese destino");
        }

        try {
            Files.move(oldLocation, newLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo renombrar: " + oldLocation, e);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setName(newName);
        entity.setLocation(newLocation.toString());
        entity.setUpdatedAt(now);

        if (entity.getType() == FileType.FOLDER) {
            relocateDescendants(entity, oldLocation, newLocation, now);
        }

        fileRepository.save(entity);

        return toResponseNode(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        workspaceService.verifyUserIsMemberOfWorkspace(entity.getWorkspaceId(), userService.getMe().id());

        if (entity.getParentId() == null) {
            throw new BusinessException("No se puede eliminar la carpeta raíz");
        }

        Path location = validateWithinBasePath(Path.of(entity.getLocation()));

        try {
            if (entity.getType() == FileType.FOLDER) {
                deleteRecursively(location);
            } else {
                Files.deleteIfExists(location);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar: " + location, e);
        }

        fileRepository.delete(entity);
    }

    private void deleteRecursively(Path location) throws IOException {
        if (!Files.exists(location)) {
            return;
        }
        try (var stream = Files.walk(location)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private void relocateDescendants(FileEntity folder, Path oldLocation, Path newLocation, LocalDateTime now) {
        List<FileEntity> files = fileRepository.findByWorkspaceId(folder.getWorkspaceId());
        Map<Long, List<FileEntity>> childrenByParentId = files.stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(FileEntity::getParentId));

        List<FileEntity> descendants = new ArrayList<>();
        collectDescendants(folder.getId(), childrenByParentId, descendants);

        for (FileEntity descendant : descendants) {
            Path relative = oldLocation.relativize(Path.of(descendant.getLocation()));
            descendant.setLocation(newLocation.resolve(relative).toString());
            descendant.setUpdatedAt(now);
        }
        fileRepository.saveAll(descendants);
    }

    private void collectDescendants(Long parentId, Map<Long, List<FileEntity>> childrenByParentId, List<FileEntity> acc) {
        for (FileEntity child : childrenByParentId.getOrDefault(parentId, List.of())) {
            acc.add(child);
            collectDescendants(child.getId(), childrenByParentId, acc);
        }
    }

    private FileNode toResponseNode(FileEntity entity) {
        return FileNode.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .type(entity.getType())
                .size(entity.getSize())
                .lastModified(entity.getUpdatedAt())
                .build();
    }

    private Path validateWithinBasePath(Path path) {
        Path basePath = Path.of(storageProperties.basePath()).normalize().toAbsolutePath();
        Path normalized = path.normalize().toAbsolutePath();

        if (!normalized.startsWith(basePath)) {
            throw new ServiceException("La ubicación está fuera del directorio permitido");
        }

        return normalized;
    }

    private FileEntity resolveParent(Long workspaceId, Long parentId, UserMe owner) {
        if (parentId == null) {
            return getOrCreateRoot(workspaceId, owner);
        }

        FileEntity parent = fileRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la carpeta con id " + parentId));

        if (parent.getType() != FileType.FOLDER) {
            throw new BusinessException("El destino no es una carpeta");
        }

        if (!parent.getWorkspaceId().equals(workspaceId)) {
            throw new PermissionDeniedException("No tiene permisos sobre esta carpeta");
        }

        return parent;
    }
}
