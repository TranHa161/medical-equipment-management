package com.example.demo.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {
    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .acl(ObjectCannedACL.PUBLIC_READ) // Để giảng viên có thể xem ảnh qua link
                .contentType(file.getContentType())
                .build(), 
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Trả về URL của ảnh trên AWS S3
        return String.format("https://%s.s3.ap-southeast-1.amazonaws.com/%s", bucketName, fileName);
    }
    
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.contains(".amazonaws.com/")) {
            return; // Nếu là ảnh mặc định hoặc link ngoài thì không xóa
        }

        try {
            String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            
            System.out.println("Đã xóa file trên S3: " + key);
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa file S3: " + e.getMessage());
        }
    }
}