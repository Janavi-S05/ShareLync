package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.CreateUserRequest;
import com.project.filesharingapp.asset.model.db.FileUser;
import com.project.filesharingapp.asset.service.UserAuthServiceImpl;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class UserRepositoryImpl {

    @Autowired private DynamoDBMapper mapper;
    @Autowired private AmazonDynamoDB dynamoDB;

    public Optional<FileUser> getUserByUsername(String username) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":username", new AttributeValue().withS(username));
        DynamoDBQueryExpression<FileUser> queryExp = new DynamoDBQueryExpression<FileUser>()
                .withIndexName("username-index")
                .withKeyConditionExpression("username = :username")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        return mapper.query(FileUser.class, queryExp).stream().findFirst();
    }

    public FileUser getUser(String id) {
        return mapper.load(FileUser.class, id);
    }

    public void initLoadUsers() {
        log.info("Loading all users for in-memory auth store");
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withConsistentRead(true);
        List<FileUser> users = new ArrayList<>(mapper.scan(FileUser.class, scanExpression));
        for (FileUser user : users) {
            if (user.getPassword() == null) continue;
            UserAuthServiceImpl.instance.addUser(user);
        }
        log.info("Loaded {} users", users.size());
    }

    public Map.Entry<String, FileUser> saveUser(CreateUserRequest createUserRequest) {
        log.info("Saving user: {}", createUserRequest.getUsername());
        FileUser user = CreateUserRequest.convertRequest(createUserRequest);
        user.setDateJoined(new Date());
        user.setLastLogin(new Date());
        user.setIsSocialLoginGoogle(createUserRequest.isSocialLoginGoogle());
        user.setPassword(createUserRequest.getPassword());
        try {
            mapper.save(user);
        } catch (Exception e) {
            log.error("saveUser error: {}", e.getMessage());
            return new AbstractMap.SimpleEntry<>(e.getMessage(), user);
        }
        return new AbstractMap.SimpleEntry<>(null, user);
    }

    public Integer deleteUser(String userId) {
        FileUser user = getUser(userId);
        if (user == null) return HttpStatus.NOT_FOUND.value();
        DeleteItemRequest request = new DeleteItemRequest();
        request.setTableName("AssetUser");
        request.addKeyEntry("user_id", new AttributeValue().withS(userId));
        dynamoDB.deleteItem(request);
        return HttpStatus.NO_CONTENT.value();
    }

    public FileUser addFilenameToFilesUploaded(String userId, List<String> filenames) {
        log.info("Updating files_uploaded for user {}", userId);
        Map<String, AttributeValue> itemKey = new HashMap<>();
        itemKey.put("user_id", new AttributeValue().withS(userId));
        List<AttributeValue> attributeValues = new ArrayList<>();
        for (String doc : filenames) {
            attributeValues.add(new AttributeValue().withS(doc));
        }
        Map<String, AttributeValueUpdate> updatedValues = new HashMap<>();
        updatedValues.put("files_uploaded",
                new AttributeValueUpdate().withValue(new AttributeValue().withL(attributeValues)));
        UpdateItemRequest request = new UpdateItemRequest()
                .withTableName("FileUser")
                .withKey(itemKey)
                .withAttributeUpdates(updatedValues);
        try {
            dynamoDB.updateItem(request);
        } catch (Exception e) {
            log.error("addFilenameToFilesUploaded error: {}", e.getMessage());
        }
        return null;
    }

    public void updateUser(String userId, FileUser user) {
        log.info("updateUser: {}", userId);
        Map<String, AttributeValue> itemKey = new HashMap<>();
        itemKey.put("user_id", new AttributeValue().withS(userId));
        Map<String, AttributeValueUpdate> updatedValues = new HashMap<>();
        updatedValues.put("username",
                new AttributeValueUpdate().withValue(new AttributeValue().withS(user.getUsername())));
        updatedValues.put("name",
                new AttributeValueUpdate().withValue(new AttributeValue().withS(user.getName())));
        updatedValues.put("password",
                new AttributeValueUpdate().withValue(new AttributeValue().withS(user.getPassword())));
        List<AttributeValue> attrVals = new ArrayList<>();
        for (String doc : user.getFilesUploaded()) {
            attrVals.add(new AttributeValue().withS(doc));
        }
        updatedValues.put("files_uploaded",
                new AttributeValueUpdate().withValue(new AttributeValue().withL(attrVals)));
        dynamoDB.updateItem(new UpdateItemRequest()
                .withTableName("FileUser").withKey(itemKey).withAttributeUpdates(updatedValues));
    }

    public void updateUserFilesUploaded(String userId, FileUser user) {
        log.info("updateUserFilesUploaded: {}", userId);
        Map<String, AttributeValue> itemKey = new HashMap<>();
        itemKey.put("user_id", new AttributeValue().withS(userId));
        List<AttributeValue> attrVals = new ArrayList<>();
        for (String doc : user.getFilesUploaded()) {
            attrVals.add(new AttributeValue().withS(doc));
        }
        Map<String, AttributeValueUpdate> updatedValues = new HashMap<>();
        updatedValues.put("files_uploaded",
                new AttributeValueUpdate().withValue(new AttributeValue().withL(attrVals)));
        dynamoDB.updateItem(new UpdateItemRequest()
                .withTableName("FileUser").withKey(itemKey).withAttributeUpdates(updatedValues));
    }
}
