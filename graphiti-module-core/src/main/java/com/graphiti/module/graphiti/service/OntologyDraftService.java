package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.business.OntDraftVO;
import com.graphiti.module.graphiti.vo.business.GenerateOntologyRespVO;
import java.util.List;

/**
 * 本体草稿管理服务接口
 */
public interface OntologyDraftService {

    List<OntDraftVO> listDrafts(String graphId);

    OntDraftVO getDraft(Long draftId);

    GenerateOntologyRespVO getDraftContent(Long draftId);

    void applyDraft(String graphId, Long draftId);

    void approveDraft(Long draftId);

    void rejectDraft(Long draftId);

    void deleteDraft(String graphId, Long draftId);
}
