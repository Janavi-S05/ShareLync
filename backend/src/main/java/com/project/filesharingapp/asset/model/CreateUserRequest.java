package com.project.filesharingapp.asset.model;

import com.project.filesharingapp.asset.model.db.FileUser;
import com.amazonaws.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
@NoArgsConstructor
public class CreateUserRequest {

    private String name;
    private String userId = "";
    private String username;
    private Date dateJoined = null;
    private List<String> filesUploaded;
    private String password;
    private boolean isSocialLoginGoogle = false;
    private String profilePicLink;

    public static FileUser convertRequest(CreateUserRequest request) {
        FileUser fileUser = FileUser.builder()
                .name(request.getName())
                .userid(StringUtils.isNullOrEmpty(request.getUserId()) ? "" : request.getUserId())
                .dateJoined(request.getDateJoined())
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
        if (request.getFilesUploaded() != null) {
            fileUser.setFilesUploaded(request.getFilesUploaded());
        }
        log.info("About to save fileUser obj [ {} ]", fileUser.toString());
        return fileUser;
    }
}