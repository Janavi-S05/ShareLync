package com.project.filesharingapp.asset.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.filesharingapp.asset.model.db.FileShareLink;
import com.project.filesharingapp.asset.repository.FileShareRepository;

    
@Service
public class FileShareService {

    @Autowired
    private FileShareRepository repository;

    public String generateShareLink(String username, String filename){

        String shareId = UUID.randomUUID().toString().substring(0,8);

        FileShareLink link = FileShareLink.builder()
                .shareId(shareId)
                .fileName(filename)
                .ownerUsername(username)
                .createdAt(System.currentTimeMillis())
                .build();

        repository.save(link);

        return shareId;
    }

    public FileShareLink getSharedFile(String shareId){
        return repository.find(shareId);
    }
}
