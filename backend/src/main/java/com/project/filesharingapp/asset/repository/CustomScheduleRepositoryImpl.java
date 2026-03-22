package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.db.Schedule;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomScheduleRepositoryImpl implements CustomScheduleRepository<Schedule, String> {

    private DynamoDBMapper mapper;
    private AmazonDynamoDB dynamoDB;

    @Override
    public List<Schedule> getScheduleByUser(String username) {
        log.info("getScheduleByUser: {}", username);
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":username", new AttributeValue().withS(username));
        DynamoDBQueryExpression<Schedule> queryExp = new DynamoDBQueryExpression<Schedule>()
                .withIndexName("username-index")
                .withKeyConditionExpression("username = :username")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        return mapper.query(Schedule.class, queryExp);
    }

    @Override
    public void updateScheduleIsSent(String scheduleId, boolean isSent) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", new AttributeValue().withS(scheduleId));
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("is_sent",
                new AttributeValueUpdate().withValue(new AttributeValue().withBOOL(isSent)));
        dynamoDB.updateItem(new UpdateItemRequest()
                .withTableName("Schedule").withKey(key).withAttributeUpdates(updates));
    }
}
