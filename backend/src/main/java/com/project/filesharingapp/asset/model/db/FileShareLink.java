
package com.project.filesharingapp.asset.model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDBTable(tableName = "FileShareLink")
public class FileShareLink {

    @DynamoDBHashKey(attributeName = "share_id")
    private String shareId;

    @DynamoDBAttribute(attributeName = "file_name")
    private String fileName;

    @DynamoDBAttribute(attributeName = "owner_username")
    private String ownerUsername;

    @DynamoDBAttribute(attributeName = "created_at")
    private Long createdAt;
}