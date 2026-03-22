package com.project.filesharingapp.asset.service;

import com.project.filesharingapp.asset.config.AWSConfig;
import com.project.filesharingapp.asset.model.FileMetadataUploadRequest;
import com.project.filesharingapp.asset.model.FileType;
import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.repository.S3Repository;
import com.project.filesharingapp.asset.utilities.StringManipService;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class StorageServiceImpl {

    @Autowired private S3Repository s3Repository;
    @Autowired private FileServiceImpl fileService;
    @Autowired private UserServiceImpl userServiceImpl;
    @Autowired private AWSConfig awsConfig;
    @Autowired private AmazonS3 amazonS3;
    @Autowired private AITaggingService aiTaggingService;
    @Autowired private CacheEvictionService cacheEvictionService;

    public ServiceResponse uploadFile(MultipartFile file, String username) throws Exception {
        List<String> files;
        try {
            ServiceResponse serviceResponse = getFilesUploadedByUser(username);
            files = (List<String>) serviceResponse.getData();
            assert files != null;
            log.info("Fetching files by username [{}]", files.size());
            if (files.size() >= awsConfig.getUploadLimit()) {
                log.info("Max upload limit reached");
                return ServiceResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .message("You have reached the limit of documents you can upload")
                        .build();
            }
        } catch (SdkClientException e) {
            return ServiceResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(e.getMessage())
                    .build();
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return ServiceResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid file name")
                    .build();
        }

        String extension = StringManipService.getExtension(originalName);
        String rawbaseName  = originalName.replace("." + extension, "");
        String baseName = rawbaseName.replaceAll("_v\\d+$", "");
        long version = files.stream()
                .filter(f -> f.contains(baseName + "_v"))
                .count() + 1;

        String versionedFilename = username + "/" + baseName + "_v" + version + "." + extension;
        log.info("Uploading versioned file {}", versionedFilename);
        ServiceResponse metadataUploadResp = saveFileMetadata(originalName, username, (int) version);
        if (metadataUploadResp.getStatus() != HttpStatus.CREATED.value()) {
            return ServiceResponse.builder()
                    .message("Something went wrong, please try again later")
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        String fileId = (String) metadataUploadResp.getData();
        File convertedFile = convertMultipartFile(file);
        ServiceResponse response;
        try {
            response = s3Repository.putS3Object(convertedFile, versionedFilename);
        } finally {
            if (convertedFile.exists()) {
                convertedFile.delete();
                log.info("Cleaned up temp file {}", convertedFile.getName());
            }
        }

        userServiceImpl.addFIlenameToFilesUploadedByUser(username, versionedFilename);
        cacheEvictionService.evictFileListCache(username);
        generateAndSaveTags(originalName, extension, "", fileId);
        return response;
    }

    @Cacheable(value = "filesByUser", key = "#userId")
    public ServiceResponse getFilesUploadedByUser(String userId) {
        List<String> objectsSavedByUser = s3Repository.filesByUser(userId);
        log.info("No. of files found [{}]", objectsSavedByUser.size());
        return ServiceResponse.builder()
                .data(objectsSavedByUser)
                .status(HttpStatus.OK.value())
                .message("")
                .build();
    }

    @Async("asyncExecutor")
    public void generateAndSaveTags(String originalName, String extension,
                                     String extractedText, String fileId) {
        try {
            log.info("Starting async tag generation for {} (id={})", originalName, fileId);
            List<String> tags = aiTaggingService.generateTags(originalName, extension, extractedText);
            if (!tags.isEmpty()) {
                ServiceResponse docResp = fileService.getDocument(fileId);
                if (docResp.getStatus() == 200 && docResp.getData() != null) {
                    com.project.filesharingapp.asset.model.db.File dbFile =
                            (com.project.filesharingapp.asset.model.db.File) docResp.getData();
                    FileMetadataUploadRequest updateReq = new FileMetadataUploadRequest();
                    updateReq.setName(dbFile.getName());
                    updateReq.setAssetType(dbFile.getAssetType());
                    updateReq.setUserId(dbFile.getUserId());
                    updateReq.setPath(dbFile.getKeyStorePath());
                    updateReq.setTags(tags);
                    fileService.updateFileMetadata(fileId, updateReq);
                    log.info("Saved {} tags to DynamoDB for file {} (id={})", tags.size(), originalName, fileId);
                } else {
                    log.warn("Could not find DynamoDB record with id={} to attach tags", fileId);
                }
            }
        } catch (Exception e) {
            log.error("Async tag generation failed for {}: {}", originalName, e.getMessage());
        }
    }

    public ServiceResponse saveFileMetadata(String originalFilename, String username, int version) {
        FileMetadataUploadRequest request = new FileMetadataUploadRequest();
        request.setName(originalFilename);
        String extension = StringManipService.getExtension(originalFilename);
        log.info("Got the file extension as {}", extension);
        FileType fileType = FileType.getFileTypeFromExtension(extension);
        assert fileType != null;
        request.setAssetType(fileType.getType());
        request.setUserId(username);
        request.setVersion(version);
        log.info("About to save document metadata [{}]", request);
        return fileService.saveDocumentMetadata(request);
    }

    @Transactional
    public ServiceResponse deleteFile(String userId, String filename) {
        String fullpath = userId + "/" + filename;
        if (!s3Repository.fileExists(fullpath)) {
            return ServiceResponse.builder()
                    .data(null)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message(filename + " - not found, are you sure you passed the correct filename?")
                    .build();
        }
        log.info("About to delete filename from user array");
        userServiceImpl.deleteFilenameFromFilesUploaded(filename, userId);
        log.info("About to delete file metadata");
        fileService.deleteFilesByUser(filename, userId);
        ServiceResponse resp = s3Repository.deleteFile(fullpath);
        cacheEvictionService.evictFileListCache(userId);
        return resp;
    }

    public ServiceResponse downloadFile(String userId, String filename) throws IOException {
        String fullpath = userId + "/" + filename;
        log.info("Checking if file [{}] exists?", fullpath);
        if (!s3Repository.fileExists(fullpath)) {
            return ServiceResponse.builder()
                    .data(null)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("'" + filename + "' - not found.")
                    .build();
        }
        log.info("File exists and now downloading the file");
        return s3Repository.downloadData(fullpath);
    }

    public File convertMultipartFile(MultipartFile multipartFile) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(convFile);
        return convFile;
    }

    private static final java.util.Map<String, String> MIME_TYPES = java.util.Map.of(
        "pdf",  "application/pdf",
        "png",  "image/png",
        "jpg",  "image/jpeg",
        "jpeg", "image/jpeg",
        "gif",  "image/gif",
        "txt",  "text/plain",
        "md",   "text/plain",
        "json", "application/json",
        "doc",  "application/msword",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private String contentTypeFor(String key) {
        String ext = key.contains(".")
            ? key.substring(key.lastIndexOf('.') + 1).toLowerCase()
            : "";
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    public String generatePresignedUrl(String key, boolean download) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime() + 1000L * 60 * 5;
        expiration.setTime(expTimeMillis);
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(awsConfig.getS3UploadBucketName(), key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        if (download) {
            request.setResponseHeaders(new ResponseHeaderOverrides()
                    .withContentDisposition("attachment"));
        } else {
            request.setResponseHeaders(new ResponseHeaderOverrides()
                    .withContentDisposition("inline")
                    .withContentType(contentTypeFor(key)));
        }
        URL url = amazonS3.generatePresignedUrl(request);
        return url.toString();
    }
}