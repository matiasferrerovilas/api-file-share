package api.m2.file.controller;

import api.m2.file.record.DownloadableFile;
import api.m2.file.record.FileNode;
import api.m2.file.service.FileService;
import java.util.UUID;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/folders")
public class FileController {

    private final FileService fileService;

    @GetMapping("/tree")
    @ApiResponse(responseCode = "200", description = "Listado retornado correctamente")
    public FileNode listFiles(@RequestParam Long workspaceId) {
        return fileService.getPersonalFolder(workspaceId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        DownloadableFile file = fileService.downloadFile(id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.filename())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.resource());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileNode uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long workspaceId,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        return fileService.uploadFile(workspaceId, parentId, file);
    }
}
