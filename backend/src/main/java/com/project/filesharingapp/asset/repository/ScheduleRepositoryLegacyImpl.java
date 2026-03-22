package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.model.db.Schedule;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ScheduleRepositoryLegacyImpl {

    @Autowired private DynamoDBMapper dynamoDBMapper;
    @Autowired private AmazonDynamoDB dynamoDB;

    public Map.Entry<String, Schedule> createSchedule(Schedule schedule) {
        try {
            dynamoDBMapper.save(schedule);
        } catch (UnsupportedOperationException uoe) {
            log.error("createSchedule error: {}", uoe.getMessage());
            return new AbstractMap.SimpleEntry<>(uoe.getMessage(), null);
        }
        log.info("Schedule [{}] saved", schedule);
        return new AbstractMap.SimpleEntry<>(null, schedule);
    }

    public void deleteSchedule(String id) {
        log.info("Deleting schedule {}", id);
        DeleteItemRequest req = new DeleteItemRequest();
        req.setTableName("Schedule");
        req.addKeyEntry("id", new AttributeValue().withS(id));
        dynamoDB.deleteItem(req);
        log.info("Schedule {} deleted", id);
    }

    public List<Schedule> getUserSchedule(String userId) {
        return null;
    }

    private ServiceResponse updateSchedule(String userId, String scheduleId) {
        return null;
    }
}
