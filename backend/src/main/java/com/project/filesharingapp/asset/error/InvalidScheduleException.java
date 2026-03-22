package com.project.filesharingapp.asset.error;

public class InvalidScheduleException extends RuntimeException {
    public InvalidScheduleException(String s){super("Please pass a valid schedule");}
}