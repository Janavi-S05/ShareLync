package com.project.filesharingapp.asset.error;

public class EntityWithIdNotFoundException extends RuntimeException  {
    public EntityWithIdNotFoundException(String message) {
        super(message);
    }
}