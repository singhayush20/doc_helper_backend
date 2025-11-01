package com.ayushsingh.doc_helper.features.doc_util;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.features.doc_util.dto.DocSaveResponse;

public interface DocService {

    DocSaveResponse saveFile(MultipartFile file);

    Resource loadFileAsResource(String sourcePath);

    Boolean deleteFile(String sourcePath);
}
