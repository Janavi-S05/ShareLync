package com.project.filesharingapp.asset.controller;

import com.project.filesharingapp.asset.error.Validator;
import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.service.FileServiceImpl;
import com.project.filesharingapp.asset.service.StorageServiceImpl;
import com.project.filesharingapp.asset.service.UserFileServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@Tag(name = "File Storage Controller")
@Slf4j
public class FileStorageController {

    @Autowired private StorageServiceImpl storageService;
    @Autowired private FileServiceImpl fileService;
    @Autowired private UserFileServiceImpl userFileService;

    @PostMapping("/upload")
    public ResponseEntity<ServiceResponse> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username) throws Exception {

        log.info("Original filename to upload {}", file.getOriginalFilename());
        Validator.validateUsernameAndToken(username);

        ServiceResponse validationResp = fileService.validateFileType(new String[]{file.getOriginalFilename()});
        if (validationResp.getStatus() != null) {
            return new ResponseEntity<>(validationResp, HttpStatus.valueOf(validationResp.getStatus()));
        }

        ServiceResponse response = storageService.uploadFile(file, username);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/list")
    public ResponseEntity<ServiceResponse> listFilesUploadedByUser(@RequestParam("userId") String userId) {
        log.info("Request to list files for [{}]", userId);
        Validator.validateUsernameAndToken(userId);
        ServiceResponse response = storageService.getFilesUploadedByUser(userId);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/by")
    public ResponseEntity<ServiceResponse> listFilesWithSchedulesByUser(@RequestParam("username") String username) {
        log.info("Getting files with schedules for user");
        Validator.validateUsernameAndToken(username);
        ServiceResponse response = userFileService.getUserFileSchedules(username);
        return new ResponseEntity<>(response, HttpStatusCode.valueOf(response.getStatus()));
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<ServiceResponse> deleteFile(
            @RequestParam("userId") String userId,
            @PathVariable("fileId") String fileId) {
        log.info("Request to delete file [{}] by user [{}]", fileId, userId);
        Validator.validateUsernameAndToken(userId);
        ServiceResponse response = storageService.deleteFile(userId, fileId);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<String> getFileData(
            @RequestParam("userId") String userId,
            @PathVariable("fileId") String fileId) {
        log.info("Generating presigned download URL for {} {}", userId, fileId);
        String key = userId + "/" + fileId;
        String presignedUrl = storageService.generatePresignedUrl(key, true);
        return ResponseEntity.ok(presignedUrl);
    }

    @GetMapping("/{fileId}/view")
    public ResponseEntity<String> viewFile(
            @RequestParam("userId") String userId,
            @PathVariable("fileId") String fileId) {
        String key = userId + "/" + fileId;
        String presignedUrl = storageService.generatePresignedUrl(key, false);
        return ResponseEntity.ok(presignedUrl);
    }

    @PutMapping("/updateFile")
    public ResponseEntity<ServiceResponse> replaceUploadedFile(@PathVariable("fileId") String fileId) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/search")
    public ResponseEntity<ServiceResponse> searchFiles(
            @RequestParam String username,
            @RequestParam String query) {
        Validator.validateUsernameAndToken(username);
        ServiceResponse response = fileService.searchFiles(username, query);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}
