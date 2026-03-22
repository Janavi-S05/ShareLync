package com.project.filesharingapp.asset.jobs;

import com.project.filesharingapp.asset.model.EmailRequest;
import com.project.filesharingapp.asset.model.db.Schedule;
import com.project.filesharingapp.asset.repository.ScheduleRepository;
import com.project.filesharingapp.asset.service.MailService;
import com.project.filesharingapp.asset.utilities.DateTimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class EmailSchedulerJob {

    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private MailService mailService;
    @Autowired private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor;

    public void scheduleSendMail(Schedule schedule) {
        log.info("Received request to set schedule -> {}", schedule);
        ShareFileTask shareFileTask = new ShareFileTask();
        shareFileTask.setSchedule(schedule);
        shareFileTask.setMailService(mailService);
        if (isDateToday(schedule.getSendDate())) {
            log.info("Send date is today — scheduling in 15 seconds");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 15);
            threadPoolTaskScheduler.schedule(shareFileTask, cal.getTime().toInstant());
        } else {
            threadPoolTaskScheduler.schedule(shareFileTask, schedule.getSendDate().toInstant());
        }
    }

    private boolean isDateToday(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar input = Calendar.getInstance();
        input.setTime(date);
        return now.get(Calendar.DAY_OF_MONTH) == input.get(Calendar.DAY_OF_MONTH)
                && now.get(Calendar.MONTH) == input.get(Calendar.MONTH)
                && now.get(Calendar.YEAR) == input.get(Calendar.YEAR);
    }

    @Async("asyncExecutor")
    public void sendScheduledEmails() {
        List<Schedule> toBeSent = new ArrayList<>();
        log.info("About to get schedules for current hour, {}", new Date());
        scheduleRepository.findAll().forEach(schedule -> {
            if (!schedule.getIsSent()) toBeSent.add(schedule);
        });
        log.info("About to trigger async job to send {} emails", toBeSent.size());
        scheduleAsyncJob(toBeSent);
    }

    public void scheduleAsyncJob(List<Schedule> schedules) {
        schedules.forEach(schedule -> {
            log.info("Queuing schedule [{}] to be sent", schedule);
            final String message = String.format("%s shared documents with you, check attachments in mail",
                    schedule.getSender());
            CompletableFuture.runAsync(() -> {
                String emailSubject = String.format("%s has shared %s with you",
                        schedule.getUsername(), schedule.getFilename());
                EmailRequest request = EmailRequest.builder()
                        .from(schedule.getUsername())
                        .to(schedule.getReceivers().get(0))
                        .filesToAttach(new String[]{schedule.getFilename()})
                        .subject(emailSubject)
                        .body(message)
                        .build();
                mailService.sendEmail(request);
                scheduleRepository.updateScheduleIsSent(schedule.getId(), true);
            }, asyncExecutor);
        });
    }
}
