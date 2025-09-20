package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.DocService;

public class LocalStorageService implements DocService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${storage.location}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new BaseException("Failed to create storage directory.", ExceptionCodes.STORAGE_INIT_ERROR);
        }
    }

    @Override
    public String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BaseException("File is empty.", ExceptionCodes.EMPTY_FILE);
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new BaseException("File name is invalid.", ExceptionCodes.INVALID_FILE_NAME);
        }

        try {
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

            Path destinationFile = this.rootLocation.resolve(uniqueFilename)
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new BaseException("File path error.", ExceptionCodes.INVALID_FILE_PATH);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return uniqueFilename;
        } catch (IOException e) {
            throw new BaseException("File path error.", ExceptionCodes.FILE_IO_ERROR);
        }
    }
}