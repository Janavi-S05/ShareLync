package com.project.filesharingapp.asset.model;

import java.io.Serializable;
import java.util.List;

import com.project.filesharingapp.asset.model.db.Schedule;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class FileWithSchedule implements Serializable{
    private static final long serialVersionUID = 1L;
    private String filename;
    private Schedule schedule;
    private List<String> tags;
}