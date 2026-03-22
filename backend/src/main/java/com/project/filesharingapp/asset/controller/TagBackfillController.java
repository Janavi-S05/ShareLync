package com.project.filesharingapp.asset.controller;

import com.project.filesharingapp.asset.model.FileMetadataUploadRequest;
import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.model.db.File;
import com.project.filesharingapp.asset.service.AITaggingService;
import com.project.filesharingapp.asset.service.FileServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class TagBackfillController {

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private AITaggingService aiTaggingService;

    @PostMapping("/backfill-tags")
    public ResponseEntity<String> backfillTags(@RequestParam String username) {
        log.info("Starting tag backfill for user: {}", username);

        // Load all DynamoDB records for this user
        List<File> allFiles = fileService.getUserDocuments(username);

        // Only process files that don't have tags yet
        List<File> untagged = allFiles.stream()
                .filter(f -> f.getTags() == null || f.getTags().isEmpty())
                .collect(Collectors.toList());

        int alreadyTagged = allFiles.size() - untagged.size();

        if (untagged.isEmpty()) {
            return ResponseEntity.ok("All " + allFiles.size() + " files already have tags. Nothing to do.");
        }

        log.info("Found {} untagged files (skipping {} already tagged)", untagged.size(), alreadyTagged);

        // Extract just the filenames for the batch call
        List<String> filenames = untagged.stream()
                .map(File::getName)
                .collect(Collectors.toList());

        // ONE API call tags all files at once
        Map<String, List<String>> tagMap = aiTaggingService.generateTagsBatch(filenames);

        int tagged = 0;
        int failed = 0;

        for (File dbFile : untagged) {
            List<String> tags = tagMap.get(dbFile.getName());
            if (tags != null && !tags.isEmpty()) {
                try {
                    FileMetadataUploadRequest updateReq = new FileMetadataUploadRequest();
                    updateReq.setName(dbFile.getName());
                    updateReq.setAssetType(dbFile.getAssetType());
                    updateReq.setUserId(dbFile.getUserId());
                    updateReq.setPath(dbFile.getKeyStorePath());
                    updateReq.setTags(tags);
                    fileService.updateFileMetadata(dbFile.getId(), updateReq);
                    log.info("Tagged '{}': {}", dbFile.getName(), tags);
                    tagged++;
                } catch (Exception e) {
                    log.error("Failed to save tags for {}: {}", dbFile.getName(), e.getMessage());
                    failed++;
                }
            } else {
                log.warn("No tags returned for: {}", dbFile.getName());
                failed++;
            }
        }

        String result = String.format(
                "Backfill complete. Tagged: %d | Failed: %d | Already had tags: %d | Total: %d",
                tagged, failed, alreadyTagged, allFiles.size()
        );
        log.info(result);
        return ResponseEntity.ok(result);
    }
}