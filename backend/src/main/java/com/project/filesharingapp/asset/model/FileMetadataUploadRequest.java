package com.project.filesharingapp.asset.model;

import com.project.filesharingapp.asset.model.db.File;

import java.util.List;

import com.amazonaws.util.StringUtils;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadataUploadRequest {

    private String name;
    private String path;
    private String assetType;
    private String id;
    private String userId;
    private Integer version;
    private List<String> tags;

    /**
     * @param uploadRequest
     * @return
     */
    public static File convertRequest(FileMetadataUploadRequest uploadRequest) {
        return File.builder()
                .keyStorePath(uploadRequest.getPath())
                .assetType(uploadRequest.getAssetType())
                .name(uploadRequest.getName())
                .id(StringUtils.isNullOrEmpty(uploadRequest.getId()) ? null : uploadRequest.getId())
                .userId(uploadRequest.getUserId())
                .build();
    }

}