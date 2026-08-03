package api.m2.file.service;

import api.m2.file.clients.identity.response.UserMe;
import api.m2.file.configuration.properties.StorageProperties;
import api.m2.file.entity.AppFileShare;
import api.m2.file.entity.FileEntity;
import api.m2.file.enums.FileType;
import api.m2.file.exceptions.BusinessException;
import api.m2.file.exceptions.EntityNotFoundException;
import api.m2.file.exceptions.PermissionDeniedException;
import api.m2.file.exceptions.ServiceException;
import api.m2.file.mappers.FileNodeMapper;
import api.m2.file.record.DownloadableFile;
import api.m2.file.record.FileNode;
import api.m2.file.repository.AppFileShareRepository;
import api.m2.file.repository.FileRepository;
import api.m2.file.service.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final String ROOT_PATH = "Home";
    private static final long MAX_UPLOAD_SIZE_BYTES = 50L * 1024 * 1024;

    private final FileRepository fileRepository;
    private final AppFileShareRepository appFileShareRepository;
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

        var shareWithByFileId = appFileShareRepository.findByFileIdIn(files.stream().map(FileEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(AppFileShare::getFileId,
                        Collectors.mapping(AppFileShare::getApiName, Collectors.toList())));

        return fileNodeMapper.toFileNode(root, childrenByParentId, shareWithByFileId);
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

        Path location = validateWithinBasePath(Path.of(file.getLocation()));

        if (file.getType() == FileType.FOLDER) {
            return downloadFolder(file, location);
        }

        if (!Files.isRegularFile(location)) {
            throw new EntityNotFoundException("El archivo no existe en el disco: " + location);
        }

        StreamingResponseBody body = out -> Files.copy(location, out);

        return DownloadableFile.builder()
                .body(body)
                .filename(file.getName())
                .contentType(resolveContentType(location))
                .build();
    }

    private DownloadableFile downloadFolder(FileEntity folder, Path location) {
        if (!Files.isDirectory(location)) {
            throw new EntityNotFoundException("La carpeta no existe en el disco: " + location);
        }

        StreamingResponseBody body = out -> zipDirectory(location, out);

        return DownloadableFile.builder()
                .body(body)
                .filename(folder.getName() + ".zip")
                .contentType("application/zip")
                .build();
    }

    private void zipDirectory(Path sourceDir, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out);
             var stream = Files.walk(sourceDir)) {
            for (Path path : stream.filter(p -> !p.equals(sourceDir)).sorted().toList()) {
                String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                if (Files.isDirectory(path)) {
                    zos.putNextEntry(new ZipEntry(entryName + "/"));
                    zos.closeEntry();
                } else {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            }
        }
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
        validateUploadableFile(file);

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

    private void validateUploadableFile(MultipartFile file) {
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new BusinessException("El archivo supera el tamaño máximo permitido de 50MB");
        }

        String contentType = file.getContentType();
        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new BusinessException("No se permite subir imágenes ni videos");
        }
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

    @Transactional(rollbackFor = Exception.class)
    public FileNode moveNode(Long id, Long newParentId) {
        var owner = userService.getMe();
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el archivo con id " + id));

        workspaceService.verifyUserIsMemberOfWorkspace(entity.getWorkspaceId(), owner.id());

        if (entity.getParentId() == null) {
            throw new BusinessException("No se puede mover la carpeta raíz");
        }

        if (Objects.equals(entity.getParentId(), newParentId)) {
            return toResponseNode(entity);
        }

        Map<Long, List<FileEntity>> childrenByParentId = childrenByParentId(entity.getWorkspaceId());

        FileEntity newParent = resolveParent(entity.getWorkspaceId(), newParentId, owner);

        if (newParent.getId().equals(entity.getId())
                || isDescendant(entity.getId(), newParent.getId(), childrenByParentId)) {
            throw new BusinessException("No se puede mover una carpeta dentro de sí misma o de una subcarpeta suya");
        }

        Path oldLocation = Path.of(entity.getLocation());
        Path newLocation = validateWithinBasePath(Path.of(newParent.getLocation()).resolve(entity.getName()));

        if (Files.exists(newLocation)) {
            throw new BusinessException(
                    "Ya existe un archivo con el nombre '" + entity.getName() + "' en ese destino");
        }

        try {
            Files.move(oldLocation, newLocation);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo mover: " + oldLocation, e);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setParentId(newParent.getId());
        entity.setLocation(newLocation.toString());
        entity.setUpdatedAt(now);

        if (entity.getType() == FileType.FOLDER) {
            relocateDescendants(entity, oldLocation, newLocation, now);
        }

        fileRepository.save(entity);

        return toResponseNode(entity);
    }

    private boolean isDescendant(Long ancestorId, Long candidateId, Map<Long, List<FileEntity>> childrenByParentId) {
        for (FileEntity child : childrenByParentId.getOrDefault(ancestorId, List.of())) {
            if (child.getId().equals(candidateId) || isDescendant(child.getId(), candidateId, childrenByParentId)) {
                return true;
            }
        }
        return false;
    }

    private void relocateDescendants(FileEntity folder, Path oldLocation, Path newLocation, LocalDateTime now) {
        Map<Long, List<FileEntity>> childrenByParentId = childrenByParentId(folder.getWorkspaceId());

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

    private Map<Long, List<FileEntity>> childrenByParentId(Long workspaceId) {
        return fileRepository.findByWorkspaceId(workspaceId).stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.groupingBy(FileEntity::getParentId));
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
