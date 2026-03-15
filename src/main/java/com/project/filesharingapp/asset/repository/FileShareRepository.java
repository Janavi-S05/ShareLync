package com.project.filesharingapp.asset.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.project.filesharingapp.asset.model.db.FileShareLink;

@Repository
public class FileShareRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public void save(FileShareLink shareLink){
        dynamoDBMapper.save(shareLink);
    }

    public FileShareLink find(String shareId){
        return dynamoDBMapper.load(FileShareLink.class, shareId);
    }
}