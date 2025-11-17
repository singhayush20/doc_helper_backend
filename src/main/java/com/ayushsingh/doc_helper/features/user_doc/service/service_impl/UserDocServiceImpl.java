package com.ayushsingh.doc_helper.features.user_doc.service.service_impl;

import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.config.security.UserContext;
import com.ayushsingh.doc_helper.features.chat.service.ChatService;
import com.ayushsingh.doc_helper.features.doc_util.DocService;
import com.ayushsingh.doc_helper.features.doc_util.EmbeddingService;
import com.ayushsingh.doc_helper.features.doc_util.dto.DocSaveResponse;
import com.ayushsingh.doc_helper.features.user.entity.User;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileDeletionVerificationResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.FileUploadResponse;
import com.ayushsingh.doc_helper.features.user_doc.dto.UserDocDetailsListDto;
import com.ayushsingh.doc_helper.features.user_doc.entity.DocumentStatus;
import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;
import com.ayushsingh.doc_helper.features.user_doc.repository.UserDocRepository;
import com.ayushsingh.doc_helper.features.user_doc.service.UserDocService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;

@Slf4j
@Service
public class UserDocServiceImpl implements UserDocService {

    private final DocService docService;
    private final UserDocRepository userDocRepository;
    private final EmbeddingService embeddingService;
    private final ChatService chatService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(2);

    public UserDocServiceImpl(
            DocService docService,
            UserDocRepository userDocRepository,
            EmbeddingService embeddingService,
            ChatService chatService,
            RedisTemplate<String, Object> redisTemplate) {
        this.docService = docService;
        this.userDocRepository = userDocRepository;
        this.embeddingService = embeddingService;
        this.chatService = chatService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public FileUploadResponse uploadDocument(MultipartFile file) {
        var savedFileInfo = saveFile(file);
        final var authUser = UserContext.getCurrentUser();
        var savedFile = saveFileInfo(savedFileInfo, authUser.getUser());

        Resource resource = docService.loadFileAsResource(savedFileInfo.storedFileName());
        embeddingService.generateAndStoreEmbeddings(savedFile.getId(), authUser.getUser().getId(), resource);

        // Invalidate user's search cache since a new file was uploaded
        clearUserSearchCache(authUser.getUser().getId());

        return new FileUploadResponse(savedFile.getFileName(), savedFile.getFileName());
    }

    private DocSaveResponse saveFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("text/plain"))) {
            log.error("Wrong file format! Only .pdf and .txt are allowed.");
            throw new BaseException("Wrong file format! Only .pdf and .txt are allowed.",
                    ExceptionCodes.WRONG_FILE_FORMAT);
        }
        final var savedFileInfo = docService.saveFile(file);
        log.info("File uploaded successfully...");
        return savedFileInfo;
    }

    @Transactional
    private UserDoc saveFileInfo(DocSaveResponse savedFileInfo, User user) {
        UserDoc newUserDoc = new UserDoc();
        newUserDoc.setUser(user);
        newUserDoc.setFileName(savedFileInfo.storedFileName());
        newUserDoc.setOriginalFilename(savedFileInfo.originalFileName());
        newUserDoc.setStatus(DocumentStatus.UPLOADED);
        var savedFile = userDocRepository.save(newUserDoc);
        log.info("File record saved successfully...");
        return savedFile;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDocDetailsListDto getUserDocuments(Pageable pageable) {
        final var authUser = UserContext.getCurrentUser();
        var page = userDocRepository.findDocsByUserId(authUser.getUser().getId(), pageable);
        var docDetails = new UserDocDetailsListDto();
        if (page != null) {
            docDetails.setUserDocs(page.getContent());
            docDetails.setCurrentPageNumber((long) page.getNumber());
            docDetails.setCurrentPageSize((long) page.getSize());
            docDetails.setFirst(page.isFirst());
            docDetails.setLast(page.isLast());
        }
        return docDetails;
    }

    @Override
    @Transactional
    public FileDeletionVerificationResponse deleteDocument(Long documentId) {
        final var authUser = UserContext.getCurrentUser();
        final var sourcePath = userDocRepository.findFileNameById(documentId);
        final var affectedRows = userDocRepository.deleteDocument(documentId, authUser.getUser().getId());
        if (affectedRows != 0) {
            chatService.deleteChatHistoryForDocument(documentId);
            embeddingService.deleteEmbeddingsByDocumentId(documentId);
            sourcePath.ifPresent(docService::deleteFile);

            // Clear user's cached search results
            clearUserSearchCache(authUser.getUser().getId());
        }

        return new FileDeletionVerificationResponse(affectedRows != 0);
    }

    @Transactional(readOnly = true)
    public UserDocDetailsListDto searchUserDocuments(String query, Pageable pageable) {
        final var authUser = UserContext.getCurrentUser();
        final Long userId = authUser.getUser().getId();

        if (query == null || query.trim().isEmpty()) {
            return new UserDocDetailsListDto();
        }

        String normalizedQuery = query.toLowerCase().trim();
        String cacheKey = String.format("search:user:%d:q:%s:p:%d",
                userId, normalizedQuery, pageable.getPageNumber());

        UserDocDetailsListDto cached = getCachedSearchResult(cacheKey);

        if (cached != null) {
            log.debug("Cache hit for key: {}", cacheKey);
            return cached;
        }

        log.debug("Cache miss for key: {}", cacheKey);

        Page<UserDocDetails> result = userDocRepository.searchByOriginalFilename(userId, normalizedQuery, pageable);

        if (result.isEmpty() && normalizedQuery.length() > 2) {
            result = userDocRepository.searchByOriginalFilenameFuzzy(userId, normalizedQuery, pageable);
        }

        // Build DTO from Page and cache the DTO (not the Page, which lacks a default constructor)
        var searchResponse = new UserDocDetailsListDto();
        if (result != null) {
            searchResponse.setUserDocs(result.getContent());
            searchResponse.setCurrentPageNumber((long) result.getNumber());
            searchResponse.setCurrentPageSize((long) result.getSize());
            searchResponse.setFirst(result.isFirst());
            searchResponse.setLast(result.isLast());
        }

        redisTemplate.opsForValue().set(cacheKey, searchResponse, SEARCH_CACHE_TTL);
        log.debug("Cached result for key: {} for TTL: {}s", cacheKey, SEARCH_CACHE_TTL.toSeconds());

        return searchResponse;
    }

    private UserDocDetailsListDto getCachedSearchResult(String cacheKey) {
        try {
            return (UserDocDetailsListDto) redisTemplate.opsForValue().get(cacheKey);
        } catch (ClassCastException e) {
            log.warn("Cache key {} had unexpected type", cacheKey);
            return null;
        }
    }

    private void clearUserSearchCache(Long userId) {
        String pattern = String.format("search:user:%d:*", userId);
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} cached search keys for user {}", keys.size(), userId);
        }
    }
}
