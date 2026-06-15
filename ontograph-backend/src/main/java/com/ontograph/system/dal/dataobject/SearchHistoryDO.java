package com.ontograph.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索历史 DO
 */
@Data
@TableName("sys_search_history")
public class SearchHistoryDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long userId;

    private String query;

    private String mode;

    private Integer resultCount;

    private LocalDateTime createTime;
}
