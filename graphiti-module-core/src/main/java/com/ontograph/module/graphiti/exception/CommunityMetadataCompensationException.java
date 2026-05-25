package com.ontograph.module.graphiti.exception;

import com.ontograph.common.exception.BusinessException;
import lombok.Getter;

/**
 * 社区元数据双写补偿异常。
 * 当 Neo4j 图数据库写入失败时抛出，用于触发 MySQL 元数据表的事务回滚。
 *
 * <p>跨库事务补偿策略：
 * <ul>
 *   <li>MySQL 元数据写操作用 Spring @Transactional 保证原子性</li>
 *   <li>Neo4j 写入失败时抛出本异常，Spring 事务管理器捕获后自动回滚 MySQL</li>
 *   <li>Neo4j 自身的写入失败由 Neo4j Driver 事务机制保证</li>
 * </ul>
 */
@Getter
public class CommunityMetadataCompensationException extends BusinessException {

    private final String graphId;
    private final String communityUuid;
    private final String operation;

    public CommunityMetadataCompensationException(String message, String graphId, String communityUuid) {
        this(message, graphId, communityUuid, null);
    }

    public CommunityMetadataCompensationException(String message, String graphId, String communityUuid, String operation) {
        super(2101, buildMessage(message, graphId, communityUuid, operation));
        this.graphId = graphId;
        this.communityUuid = communityUuid;
        this.operation = operation;
    }

    private static String buildMessage(String message, String graphId, String communityUuid, String operation) {
        StringBuilder sb = new StringBuilder("社区元数据双写失败: ").append(message);
        sb.append(" | graphId=").append(graphId);
        if (communityUuid != null) {
            sb.append(" | communityUuid=").append(communityUuid);
        }
        if (operation != null) {
            sb.append(" | operation=").append(operation);
        }
        return sb.toString();
    }
}
