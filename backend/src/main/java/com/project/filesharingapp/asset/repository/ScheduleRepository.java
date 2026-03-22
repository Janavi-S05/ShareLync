package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.db.Schedule;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ScheduleRepository implements CustomScheduleRepository<Schedule, String> {

    @Autowired private DynamoDBMapper mapper;
    @Autowired private AmazonDynamoDB dynamoDB;

    public Schedule save(Schedule schedule) {
        mapper.save(schedule);
        log.info("Saved schedule {}", schedule.getId());
        return schedule;
    }

    public Optional<Schedule> findById(String id) {
        Schedule schedule = mapper.load(Schedule.class, id);
        return Optional.ofNullable(schedule);
    }

    public void delete(Schedule schedule) {
        mapper.delete(schedule);
        log.info("Deleted schedule {}", schedule.getId());
    }

    public Iterable<Schedule> findAll() {
        return mapper.scan(Schedule.class, new DynamoDBScanExpression());
    }

    @Override
    public List<Schedule> getScheduleByUser(String username) {
        log.info("Querying schedules for user: {}", username);
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
        log.info("Marking schedule {} isSent={}", scheduleId, isSent);
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", new AttributeValue().withS(scheduleId));
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("is_sent",
                new AttributeValueUpdate().withValue(new AttributeValue().withBOOL(isSent)));
        dynamoDB.updateItem(new UpdateItemRequest()
                .withTableName("Schedule")
                .withKey(key)
                .withAttributeUpdates(updates));
    }
}
