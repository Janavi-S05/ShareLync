package com.project.filesharingapp.asset.controller;

import com.project.filesharingapp.asset.error.Validator;
import com.project.filesharingapp.asset.model.EmailRequest;
import com.project.filesharingapp.asset.model.ServiceResponse;
import com.project.filesharingapp.asset.service.FileServiceImpl;
import com.project.filesharingapp.asset.service.MailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social controller")
@Slf4j
public class SocialController {

    @Autowired
    private FileServiceImpl fileService;

    @Autowired
    private MailService mailService;

    @PostMapping(value = "/sendMail")
    public ResponseEntity<ServiceResponse> sendMail(@RequestBody EmailRequest emailRequest) {
        log.info(emailRequest.toString());
        if(fileService.validateFileType(emailRequest.getFilesToAttach()).getStatus() != null) {
            ServiceResponse response = fileService.validateFileType(emailRequest.getFilesToAttach());
            return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
        }
        Validator.validateEmailRequest(emailRequest);

        ServiceResponse serviceResponse = mailService.sendEmail(emailRequest);
        return new ResponseEntity<>(serviceResponse, HttpStatus.valueOf(serviceResponse.getStatus()));
    }
}