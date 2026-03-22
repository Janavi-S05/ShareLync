package com.project.filesharingapp.asset.model;

import jakarta.validation.constraints.Email;
import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {
    @Email
    private String from;
    
    @Email
    private String to;
    
    @Email
    private String[] cc;
    
    @Email
    private String[] bcc;
    
    private String subject;
    private String body;
    private String[] filesToAttach;
}