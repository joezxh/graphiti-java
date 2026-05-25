package com.ontograph.system.service;

import com.ontograph.system.dal.dataobject.SearchHistoryDO;
import java.util.List;
import java.util.Map;

/**
 * 搜索历史服务接口
 */
public interface SearchHistoryService {

    /**
     * 获取当前用户搜索历史（分页）
     */
    Map<String, Object> listHistory(Integer pageNo, Integer pageSize);

    /**
     * 保存搜索记录
     */
    Long saveHistory(String query, String mode, Integer resultCount);

    /**
     * 清空当前用户搜索历史
     */
    void clearHistory();
}
