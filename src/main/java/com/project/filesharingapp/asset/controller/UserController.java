package com.project.filesharingapp.asset.controller;

import com.project.filesharingapp.asset.error.Validator;
import com.project.filesharingapp.asset.model.*;
import com.project.filesharingapp.asset.service.FileServiceImpl;
import com.project.filesharingapp.asset.service.UserServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Controller")
@Slf4j
public class UserController {
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private FileServiceImpl fileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ServiceResponse> getUser(@PathVariable("userId") String userId) {
        log.info("About to query user with id {}", userId);
        ServiceResponse response = userService.getUser(userId);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PostMapping(value = "/create", consumes = {"application/json"})
    public ResponseEntity<ServiceResponse> createUser(@RequestBody CreateUserRequest createUserRequest) {
        log.info("Got request [ {} ]", createUserRequest.toString());
        ServiceResponse response = userService.saveUser(createUserRequest);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ServiceResponse> deleteUser(@PathVariable("userId") String userId) {
        log.info("Request to delete user [ {} ]", userId);
        ServiceResponse deleteState = userService.deleteUser(userId);
        return new ResponseEntity<>(deleteState, HttpStatus.valueOf(deleteState.getStatus()));
    }

    @PutMapping(value = "/{userId}", consumes = {"application/json"})
    public ResponseEntity<ServiceResponse> updateUserDetails(@PathVariable("userId") String userId,
                                                             @RequestBody CreateUserRequest createUserRequest) {

        log.info("Got a request to update user, [ {} ]", createUserRequest.toString());
        ServiceResponse response = userService.updateUser(userId, createUserRequest);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    /**
     * Endpoint to list which files the user has access to
     *
     * @param username
     * @return
     */
    @GetMapping(value = "/files")
    public ResponseEntity<ServiceResponse> getFilesAccessibleToUser(@RequestParam("username") String username) {
        log.info("Got request to get files accessible to user [ {} ]", username);
        Validator.validateUsernameAndToken(username);
        ServiceResponse response = userService.getFilesAccessibleToUser(username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Leter, add the Role check so if not the
     * same user id, only the admin can get it
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @GetMapping("/username")
    public ResponseEntity<ServiceResponse> getByUsername(@RequestParam("username") String username) throws UsernameNotFoundException{
        // a user can only see their own details
        // check for user role to be added later i.e. admin can see everything
        log.info("Got request to get user by name {}", username);
        Validator.validateUsernameAndToken(username);
        ServiceResponse response = userService.getByUsername(username);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PostMapping(value = "/share", consumes = {"application/json"})
    public ResponseEntity<ServiceResponse> postShareResponse(@RequestBody ShareFileRequest shareFileRequest)
            throws Exception {
        log.info("Share file request received for {}", shareFileRequest.toString());
        // for now just return Http.OK
        userService.shareFile(shareFileRequest.getFromUsername(), shareFileRequest.getToUsername(),
                shareFileRequest.getFilename());
        return new ResponseEntity<>(new ServiceResponse(), HttpStatus.OK);
    }
}