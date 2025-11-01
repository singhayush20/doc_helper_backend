package com.ayushsingh.doc_helper.features.user_doc.dto;

import java.util.List;

import com.ayushsingh.doc_helper.features.user_doc.repository.projections.UserDocDetails;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDocDetailsListDto {

    private List<UserDocDetails> userDocs;
    private Long currentPageNumber;
    private Long currentPageSize;
    private boolean isFirst;
    private boolean isLast;
}