package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadResponse {

    private String objectName;
    private String objectPath;
    private String objectUrl;
    private String bucket;
}
