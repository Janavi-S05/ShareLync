package com.project.filesharingapp.asset.model.db;

import com.project.filesharingapp.asset.model.FileMetadataUploadRequest;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "File")
public class File {

    @DynamoDBAttribute(attributeName = "name")
    private String name;

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "key_store_path")
    private String keyStorePath;

    @DynamoDBAttribute(attributeName = "asset_type")
    private String assetType;

    // the user id is the owner of the file
    @DynamoDBAttribute(attributeName = "user_id")
    private String userId;

    @DynamoDBAttribute(attributeName = "version")
    private Integer version;

    @DynamoDBAttribute(attributeName = "parent_file_id")
    private String parentFileId;

    public void transformForUpdate(FileMetadataUploadRequest fileMetadataUploadRequest) {
        setName(fileMetadataUploadRequest.getName());
        setAssetType(fileMetadataUploadRequest.getAssetType());
        setUserId(fileMetadataUploadRequest.getUserId());
        setKeyStorePath(fileMetadataUploadRequest.getPath());
    }
}