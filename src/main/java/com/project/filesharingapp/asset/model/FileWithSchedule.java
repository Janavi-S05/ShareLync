package com.project.filesharingapp.asset.model;

import com.project.filesharingapp.asset.model.db.Schedule;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class FileWithSchedule {
    private String filename;
    private Schedule schedule;
}