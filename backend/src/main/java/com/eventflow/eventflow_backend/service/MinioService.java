package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.config.properties.MinioProperties;
import com.eventflow.eventflow_backend.dto.response.FileUploadResponse;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .build()
                );
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось инициализировать bucket MinIO", ex);
        }
    }

    @Transactional
    public FileUploadResponse uploadPoster(MultipartFile file) {
        validateFile(file);
        ensureBucketExists();

        String objectPath = buildPosterObjectPath(file);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось загрузить файл в MinIO", ex);
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setObjectName(extractObjectName(objectPath));
        response.setObjectPath(objectPath);
        response.setObjectUrl(buildPublicUrl(objectPath));
        response.setBucket(minioProperties.getBucket());

        return response;
    }

    public void deleteObject(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectPath)
                            .build()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось удалить файл из MinIO", ex);
        }
    }

    public String buildPublicUrl(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }

        return minioProperties.getPublicBaseUrl() + "/" + objectPath;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не был передан");
        }
    }

    private String buildPosterObjectPath(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        return "posters/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf("."));
    }

    private String extractObjectName(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }

        int slashIndex = objectPath.lastIndexOf('/');
        if (slashIndex == -1) {
            return objectPath;
        }

        return objectPath.substring(slashIndex + 1);
    }
}