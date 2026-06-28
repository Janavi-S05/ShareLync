# ShareLync - Secure File Sharing Platform

ShareLync is a cloud-based file sharing and management platform built using Spring Boot and React. It enables users to securely upload, organize, search, and share files while leveraging AI to automatically generate document summaries. The application integrates AWS cloud services for scalable storage and metadata management and uses JWT-based authentication to ensure secure access.

---

## Features

- Secure user registration and login using JWT Authentication
- OAuth2 login support
- Upload and download files
- Secure file sharing through public/private links
- Store uploaded files in Amazon S3
- Store file metadata in Amazon DynamoDB
- Search and organize uploaded files
- Background asynchronous processing for AI tag
- Role-based API security using Spring Security
- RESTful APIs with Swagger documentation

---

## Tech Stack

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data
- JWT Authentication
- OAuth2
- Maven

### Frontend
- React.js
- Material UI
- Axios

### Database & Cloud
- Amazon S3
- Amazon DynamoDB

### AI Integration
- Groq API

### Documentation
- Swagger / OpenAPI

---

## System Architecture

```
                +--------------------+
                |    React Frontend  |
                +---------+----------+
                          |
                     REST APIs
                          |
                +---------v----------+
                | Spring Boot Backend|
                +---------+----------+
                          |
          +---------------+----------------+
          |                                |
          |                                |
     Amazon S3                      DynamoDB
 (File Storage)                 (File Metadata)
          |
          |
      Groq AI API
(Document tag)
```

## Database Schema

### User

| Field | Type |
|------|------|
| id | UUID |
| username | String |
| email | String |
| password | String |
| role | String |

### File Metadata

| Field | Type |
|------|------|
| id | UUID |
| fileName | String |
| fileType | String |
| fileSize | Long |
| uploadedBy | UUID |
| s3Key | String |
| summary | String |
| uploadTime | Timestamp |

### Shared Link

| Field | Type |
|------|------|
| id | UUID |
| fileId | UUID |
| shareToken | String |
| expiryDate | Timestamp |
| accessType | Public / Private |

---
