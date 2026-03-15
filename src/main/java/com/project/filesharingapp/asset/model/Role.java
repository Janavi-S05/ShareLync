package com.project.filesharingapp.asset.model;

import com.project.filesharingapp.asset.model.db.FileUser;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Role {
    enum ROLE_NAME {
        ADMIN,
        USER,
    }

    private Long id;
    private String name;
    private List<FileUser> users;
}