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
import org.springframework.http.HttpStatus;
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
    @Autowired
    private S3Repository s3Repository;

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private AWSConfig awsConfig;

    @Autowired
    private AmazonS3 amazonS3;

    public ServiceResponse uploadFile(MultipartFile file, String username) throws Exception 
    {

        List<String> files;

        try {
            ServiceResponse serviceResponse = getFilesUploadedByUser(username);
            files = (List<String>) serviceResponse.getData();
            assert files != null;

            log.info("Fetching files by username [ {} ]", files.size());

            if (files.size() >= awsConfig.getUploadLimit()) {
                log.info("Max upload limit reached");
                return ServiceResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .message("You have reached the limit of the no of documents you can upload")
                        .build();
            }

        } catch (SdkClientException e) {
            return ServiceResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(e.getMessage())
                    .build();
        }

        String originalName = file.getOriginalFilename();
        if(originalName == null){
            return ServiceResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Invalid file name")
                    .build();
}
        String extension = StringManipService.getExtension(originalName);
        String baseName = originalName.replace("." + extension, "");

        long version = files.stream()
        .filter(f -> f.contains(baseName + "_v"))
        .count() + 1;


        String versionedFilename =
                username + "/" + baseName + "_v" + version + "." + extension;

        log.info("Uploading versioned file {}", versionedFilename);

        ServiceResponse metadataUploadResp =
                saveFileMetadata(originalName, username, (int) version);

        if (metadataUploadResp.getStatus() != HttpStatus.CREATED.value()) {
            return ServiceResponse.builder()
                    .message("Something went wrong, please try again later")
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        File convertedMultiPFile = convertMultipartFile(file);

        ServiceResponse response =
                s3Repository.putS3Object(convertedMultiPFile, versionedFilename);

        convertedMultiPFile.deleteOnExit();

        userServiceImpl.addFIlenameToFilesUploadedByUser(username, versionedFilename);

        return response;
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
        log.info("About to save document metadata [ {} ]", request.toString());
        return fileService.saveDocumentMetadata(request);
    }

    public ServiceResponse getFilesUploadedByUser(String userId) {
        List<String> objectsSavedByUser = s3Repository.filesByUser(userId);
        log.info("No. of files found [ {} ]", objectsSavedByUser.size());
        return ServiceResponse
                .builder()
                .data(objectsSavedByUser)
                .status(HttpStatus.OK.value())
                .message("")
                .build();
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
        log.info("About to delete file metadata ");
        fileService.deleteFilesByUser(filename, userId);

        return s3Repository.deleteFile(fullpath);
    }

    public ServiceResponse downloadFile(String userId, String filename) throws IOException {
        String fullpath = userId + "/" + filename;
        log.info("Checking if file [ {} ] exists?", fullpath);
        if (!s3Repository.fileExists(fullpath)) {
            return ServiceResponse.builder()
                    .data(null)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("'" + filename + "' - not found. are you sure you passed the correct filename?")
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

    public String generatePresignedUrl(String key , boolean download) {

            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 5;
            expiration.setTime(expTimeMillis);

            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(
                            awsConfig.getS3UploadBucketName(),
                            key)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);

            if(download) {
                ResponseHeaderOverrides headers = new ResponseHeaderOverrides()
                        .withContentDisposition("attachment");

                request.setResponseHeaders(headers);
            }

            URL url = amazonS3.generatePresignedUrl(request);

            return url.toString();
    }
}