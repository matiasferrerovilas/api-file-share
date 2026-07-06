package com.api.file.share.service;

import com.api.file.share.enums.FileType;
import com.api.file.share.record.FileNode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FileService {

    private static final long PHOTO_1_SIZE = 2_458_112L;
    private static final long PHOTO_2_SIZE = 3_145_728L;
    private static final long PHOTO_3_SIZE = 1_887_436L;
    private static final long CERTIFICATE_1_SIZE = 512_000L;
    private static final long CERTIFICATE_2_SIZE = 604_160L;
    private static final long CERTIFICATE_3_SIZE = 458_752L;

    public FileNode getPersonalFolder() {
        LocalDateTime now = LocalDateTime.now();

        FileNode fotos = FileNode.builder()
                .id("folder-fotos")
                .name("Fotos")
                .type(FileType.FOLDER)
                .lastModified(now)
                .children(List.of(
                        FileNode.builder().id("photo-1").name("vacaciones.jpg").type(FileType.FILE)
                                .size(PHOTO_1_SIZE).lastModified(now).build(),
                        FileNode.builder().id("photo-2").name("cumpleanos.jpg").type(FileType.FILE)
                                .size(PHOTO_2_SIZE).lastModified(now).build(),
                        FileNode.builder().id("photo-3").name("familia.jpg").type(FileType.FILE)
                                .size(PHOTO_3_SIZE).lastModified(now).build()
                ))
                .build();

        FileNode certificados = FileNode.builder()
                .id("folder-certificados")
                .name("Certificados")
                .type(FileType.FOLDER)
                .lastModified(now)
                .children(List.of(
                        FileNode.builder().id("cert-1").name("titulo-universitario.pdf").type(FileType.FILE)
                                .size(CERTIFICATE_1_SIZE).lastModified(now).build(),
                        FileNode.builder().id("cert-2").name("certificado-ingles.pdf").type(FileType.FILE)
                                .size(CERTIFICATE_2_SIZE).lastModified(now).build(),
                        FileNode.builder().id("cert-3").name("certificado-curso.pdf").type(FileType.FILE)
                                .size(CERTIFICATE_3_SIZE).lastModified(now).build()
                ))
                .build();

        return FileNode.builder()
                .id("folder-personal")
                .name("Personal")
                .type(FileType.FOLDER)
                .lastModified(now)
                .children(List.of(fotos, certificados))
                .build();
    }
}
