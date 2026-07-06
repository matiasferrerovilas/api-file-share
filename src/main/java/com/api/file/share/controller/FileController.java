package com.api.file.share.controller;

import com.api.file.share.record.FileNode;
import com.api.file.share.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @GetMapping
    public ResponseEntity<FileNode> listFiles() {
        return ResponseEntity.ok(fileService.getPersonalFolder());
    }
}
