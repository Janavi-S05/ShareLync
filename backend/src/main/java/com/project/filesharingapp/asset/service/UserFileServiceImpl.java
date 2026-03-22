package com.project.filesharingapp.asset.service;

import com.project.filesharingapp.asset.model.FileWithSchedule;
import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.model.db.Schedule;
import com.project.filesharingapp.asset.repository.S3Repository;
import com.project.filesharingapp.asset.repository.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserFileServiceImpl {

    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private S3Repository s3Repository;
    @Autowired private FileServiceImpl fileService;

    public ServiceResponse getUserFileSchedules(String username) {
        List<String> userUploadedFiles = s3Repository.filesByUser(username);
        List<Schedule> schedules = scheduleRepository.getScheduleByUser(username);
        log.info("Schedules for user: {}", schedules.size());

        Map<String, Schedule> scheduleByFilename = schedules.stream()
                .filter(s -> s.getFilename() != null)
                .collect(Collectors.toMap(
                        s -> s.getFilename().toLowerCase(),
                        s -> s,
                        (a, b) -> a
                ));

        List<com.project.filesharingapp.asset.model.db.File> dbFiles =
                fileService.getUserDocuments(username);

        Map<String, List<String>> tagsByBase = dbFiles.stream()
                .filter(f -> f.getName() != null && f.getTags() != null && !f.getTags().isEmpty())
                .collect(Collectors.toMap(
                        f -> stripExtension(f.getName()).toLowerCase(),
                        com.project.filesharingapp.asset.model.db.File::getTags,
                        (a, b) -> a
                ));

        List<FileWithSchedule> userFileList = new ArrayList<>();

        for (String s3Filename : userUploadedFiles) {
            FileWithSchedule uf = FileWithSchedule.builder().filename(s3Filename).build();
            Schedule match = scheduleByFilename.get(s3Filename.toLowerCase());
            if (match != null) uf.setSchedule(match);

            String bare = s3Filename.contains("/")
                    ? s3Filename.substring(s3Filename.lastIndexOf('/') + 1) : s3Filename;
            String base = stripVersionSuffix(stripExtension(bare)).toLowerCase();
            List<String> tags = tagsByBase.get(base);
            if (tags != null) uf.setTags(tags);

            userFileList.add(uf);
        }

        return ServiceResponse.builder()
                .data(userFileList)
                .status(HttpStatus.OK.value())
                .build();
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private String stripVersionSuffix(String base) {
        return base.replaceAll("_v\\d+$", "");
    }
}
