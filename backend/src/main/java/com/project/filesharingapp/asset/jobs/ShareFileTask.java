package com.project.filesharingapp.asset.jobs;

import com.project.filesharingapp.asset.model.EmailRequest;
import com.project.filesharingapp.asset.model.db.Schedule;
import com.project.filesharingapp.asset.service.MailService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@ToString
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileTask implements Runnable {

    private Schedule schedule;

    private MailService mailService;

    @Override
    public void run() {
        final String message = String.format("%s shared documents with you, check attachments in mail",
                schedule.getSender());
        String emailSubject = String.format("%s has shared %s with you", schedule.getUsername(), schedule.getFilename());
        log.info("Executing share file task");
        schedule.getReceivers().forEach(receiver -> {
        log.info("Sending it to user {}", receiver);
            log.info("in the runAsync");
            String filename = schedule.getUsername() + "/" + schedule.getFilename();
            EmailRequest request = EmailRequest.builder()
                    .from(schedule.getUsername())
                    .to(receiver)
                    .filesToAttach(new String[]{filename})
                    .subject(emailSubject)
                    .body(message)
                    .build();
            log.info("About to send mail");
            
            mailService.sendEmail(request);
        });
    }
}