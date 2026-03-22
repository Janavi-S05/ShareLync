package com.project.filesharingapp.asset.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.filesharingapp.asset.model.db.FileShareLink;
import com.project.filesharingapp.asset.repository.S3Repository;
import com.project.filesharingapp.asset.service.FileShareService;

@RestController
public class PublicShareController {
    
    @Autowired
    private FileShareService fileShareService;

    @Autowired
    private S3Repository s3Repository;
    
    @PostMapping("/api/link/generate")
    public ResponseEntity<?> shareFile(
        @RequestParam String username,
        @RequestParam String filename){

        String shareId = fileShareService.generateShareLink(username, filename);

        String link = "http://localhost:5000/public/" + shareId;

        return ResponseEntity.ok(link);
    }

    @GetMapping("/public/{shareId}")
public ResponseEntity<byte[]> downloadPublic(
        @PathVariable String shareId) throws IOException {

    FileShareLink link = fileShareService.getSharedFile(shareId);

    if (link == null) {
        return ResponseEntity.notFound().build();
    }

    String fullpath = link.getOwnerUsername() + "/" + link.getFileName();

    byte[] fileData = s3Repository.getDataForFile(fullpath);

    return ResponseEntity.ok()
            .header("Content-Disposition",
                    "attachment; filename=\"" + link.getFileName() + "\"")
            .header("Content-Type", "application/octet-stream")
            .body(fileData);
}
}
