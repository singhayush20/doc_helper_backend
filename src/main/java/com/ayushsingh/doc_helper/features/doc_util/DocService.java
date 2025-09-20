package com.ayushsingh.doc_helper.features.doc_util;

import org.springframework.web.multipart.MultipartFile;

public interface DocService {
    String saveFile(MultipartFile file);
}
