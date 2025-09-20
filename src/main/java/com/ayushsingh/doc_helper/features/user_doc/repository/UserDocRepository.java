package com.ayushsingh.doc_helper.features.user_doc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.user_doc.entity.UserDoc;

public interface UserDocRepository extends JpaRepository<UserDoc, Long> {

    Optional<UserDoc> findByIdAndUserId(Long documentId, Long userId);

    List<UserDoc> findByUserId(Long userId);
}
