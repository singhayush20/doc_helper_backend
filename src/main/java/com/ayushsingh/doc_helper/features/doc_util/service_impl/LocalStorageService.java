package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.doc_util.DocService;

@Service
@Slf4j
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
            log.error("File is empty.");
            throw new BaseException("File is empty.", ExceptionCodes.EMPTY_FILE);
        }
        final var originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            log.error("File name is null.");
            throw new BaseException("File name is null.", ExceptionCodes.INVALID_FILE_NAME);
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            log.error("File name is invalid.");
            throw new BaseException("File name is invalid.", ExceptionCodes.INVALID_FILE_NAME);
        }

        try {
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + (extension != null ? "." + extension : "");

            Path destinationFile = this.rootLocation.resolve(uniqueFilename)
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                log.error("File path error.");
                throw new BaseException("File path error.", ExceptionCodes.INVALID_FILE_PATH);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return uniqueFilename;
        } catch (IOException e) {
            log.error("File path error {}", e.getMessage());
            throw new BaseException("File path error.", ExceptionCodes.FILE_IO_ERROR);
        }
    }

    @Override
    public Resource loadFileAsResource(String sourcePath) {
        try {
            Path filePath = rootLocation.resolve(sourcePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new InternalServerException("Could not read file: " + sourcePath, ExceptionCodes.INTERNAL_FILE_IO_ERROR);
            }
        } catch (MalformedURLException e) {
            log.error("File path is invalid {}", e.getMessage());
            throw new InternalServerException("Could not read file: " + sourcePath, ExceptionCodes.INTERNAL_FILE_IO_ERROR);
        }
    }
}
