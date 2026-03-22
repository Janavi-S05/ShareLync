package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.FileMetadataUploadRequest;
import com.project.filesharingapp.asset.model.FileType;
import com.project.filesharingapp.asset.model.db.File;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FileRepositoryImpl {

    @Autowired private DynamoDBMapper mapper;
    @Autowired private AmazonDynamoDB dynamoDB;

    public File getDocument(String id) {
        try {
            return mapper.load(File.class, id);
        } catch (Exception e) {
            log.error("getDocument error: {}", e.getMessage());
            return null;
        }
    }

    public Integer saveFileMetadata(FileMetadataUploadRequest request) {
        File asset = FileMetadataUploadRequest.convertRequest(request);
        log.info("Saving file metadata: {}", asset.getName());
        mapper.save(asset);
        return HttpStatus.CREATED.value();
    }

    public Integer deleteFile(String id) {
        File file = getDocument(id);
        if (file == null) return HttpStatus.NOT_FOUND.value();
        try {
            DeleteItemRequest request = new DeleteItemRequest();
            request.setTableName("File");
            request.addKeyEntry("id", new AttributeValue().withS(id));
            dynamoDB.deleteItem(request);
            return HttpStatus.NO_CONTENT.value();
        } catch (Exception e) {
            log.error("deleteFile error: {}", e.getMessage());
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }

    public Integer updateDocument(String id, FileMetadataUploadRequest req) {
        File fileToUpdate = getDocument(id);
        if (fileToUpdate == null) return HttpStatus.NOT_FOUND.value();
        fileToUpdate.transformForUpdate(req);
        DynamoDBSaveExpression saveOp = new DynamoDBSaveExpression()
                .withExpectedEntry("id", new ExpectedAttributeValue()
                        .withValue(new AttributeValue(id)));
        mapper.save(fileToUpdate, saveOp);
        return HttpStatus.NO_CONTENT.value();
    }

    public List<File> getUserDocuments(String userId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":userId", new AttributeValue().withS(userId));
        DynamoDBQueryExpression<File> queryExp = new DynamoDBQueryExpression<File>()
                .withIndexName("user_id-index")
                .withKeyConditionExpression("user_id = :userId")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        return mapper.query(File.class, queryExp);
    }

    public void deleteFileByUser(String filename, String username) {
        log.info("Deleting file metadata for {} / {}", username, filename);
        getUserDocuments(username).stream()
                .filter(f -> f.getName().equalsIgnoreCase(filename))
                .forEach(f -> deleteFile(f.getId()));
        log.info("Deleted file metadata for {}", filename);
    }

    public List<File> getDocumentsOfType(FileType fileType) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":type", new AttributeValue().withS(fileType.getType()));
        DynamoDBQueryExpression<File> queryExp = new DynamoDBQueryExpression<File>()
                .withIndexName("asset_type-index")
                .withKeyConditionExpression("asset_type = :type")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        return mapper.query(File.class, queryExp);
    }
}
