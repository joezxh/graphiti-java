package com.ontograph.common.constants;

/**
 * 结果码常量接口
 * 定义系统返回码：200=成功，4xx=客户端错误，5xx=服务端错误，1xxx=业务错误
 */
public interface ResultCode {
    int SUCCESS = 200;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int INTERNAL_SERVER_ERROR = 500;
    
    // Graphiti 业务错误码 (1001-1099)
    int GRAPH_NOT_FOUND = 1001;
    int ONTOLOGY_NOT_DEFINED = 1002;
    int NODE_NOT_FOUND = 1003;
    int EDGE_NOT_FOUND = 1004;
    int EPISODE_NOT_FOUND = 1005;
    int INVALID_PARAMETER = 1006;

    // 业务信息模块错误码 (2000-2099)
    int ONT_DRAFT_NOT_FOUND = 2000;
    int ONT_DRAFT_ALREADY_APPLIED = 2001;
    int ONT_GENERATION_FAILED = 2002;
    int DATA_GENERATION_FAILED = 2003;
    int DESCRIPTION_OPTIMIZATION_FAILED = 2004;
    int INVALID_DRAFT_TYPE = 2005;
    int GRAPH_METADATA_NOT_FOUND = 2006;
    int MOCK_DATA_NOT_FOUND = 2007;
    int ONT_DRAFT_INVALID_STATUS = 2008;
}
