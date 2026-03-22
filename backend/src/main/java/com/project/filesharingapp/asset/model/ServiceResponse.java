package com.project.filesharingapp.asset.model;

import java.io.Serializable;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ServiceResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer status;
    private Object data;
    private String message = "";
    private boolean isError = false;
}