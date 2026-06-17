package com.ontograph.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontograph.module.system.dal.dataobject.SearchHistoryDO;
import com.ontograph.module.system.dal.dataobject.UserDO;
import com.ontograph.module.system.dal.mysql.SearchHistoryMapper;
import com.ontograph.module.system.dal.mysql.UserMapper;
import com.ontograph.module.system.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> listHistory(Integer pageNo, Integer pageSize) {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<SearchHistoryDO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SearchHistoryDO::getUserId, userId);
        }
        wrapper.orderByDesc(SearchHistoryDO::getCreateTime);
        // 最多保留 200 条
        wrapper.last("LIMIT 200");
        Page<SearchHistoryDO> page = new Page<>(pageNo, pageSize);
        Page<SearchHistoryDO> result = searchHistoryMapper.selectPage(page, wrapper);
        Map<String, Object> resp = new HashMap<>();
        resp.put("list", result.getRecords());
        resp.put("total", result.getTotal());
        resp.put("pageNum", pageNo);
        resp.put("pageSize", pageSize);
        return resp;
    }

    @Override
    public Long saveHistory(String query, String mode, Integer resultCount) {
        Long userId = getCurrentUserId();
        SearchHistoryDO history = new SearchHistoryDO();
        history.setUserId(userId);
        history.setQuery(query);
        history.setMode(mode);
        history.setResultCount(resultCount != null ? resultCount : 0);
        history.setCreateTime(LocalDateTime.now());
        searchHistoryMapper.insert(history);
        log.debug("保存搜索历史：userId={}, query={}", userId, query);
        return history.getId();
    }

    @Override
    public void clearHistory() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            LambdaQueryWrapper<SearchHistoryDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SearchHistoryDO::getUserId, userId);
            searchHistoryMapper.delete(wrapper);
            log.info("清空搜索历史：userId={}", userId);
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        String username = auth.getName();
        try {
            UserDO user = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getUsername, username)
                    .eq(UserDO::getDeleted, false)
            );
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败：username={}", username, e);
            return null;
        }
    }
}
