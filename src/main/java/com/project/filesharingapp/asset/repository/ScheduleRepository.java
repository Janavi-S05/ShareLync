package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.db.Schedule;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface ScheduleRepository extends CrudRepository<Schedule, String>, CustomScheduleRepository<Schedule, String> {
}