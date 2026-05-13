package com.graphiti.common.constants;

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
}
