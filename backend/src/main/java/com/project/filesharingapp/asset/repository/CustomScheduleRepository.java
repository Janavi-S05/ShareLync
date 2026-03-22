package com.project.filesharingapp.asset.repository;

import com.project.filesharingapp.asset.model.db.Schedule;

import java.util.List;

public interface CustomScheduleRepository<T, Z> {
    List<Schedule> getScheduleByUser(String username);
    void updateScheduleIsSent(String scheduleId, boolean isSent);
}