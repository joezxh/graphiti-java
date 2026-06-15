package com.ontograph.module.graphiti.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.Collection;

/**
 * PostgreSQL JSONB 类型的 TypeHandler。
 *
 * MyBatis-Plus 默认的 JacksonTypeHandler 在写入时将对象序列化为 String 后通过
 * PreparedStatement.setString() 发送，但 PostgreSQL 的 JSONB 列不接受 varchar 的隐式
 * 转换，必须使用 setObject() 并显式指定 Types.OTHER 才能正确写入 JSONB。
 *
 * 该 Handler 接受任意可 JSON 序列化的对象（String / Collection / Map 等），内部使用
 * Jackson 完成序列化/反序列化。
 *
 * 使用方式：在 DO 实体字段上标注：
 * <pre>
 * &#64;TableField(typeHandler = PgJsonbTypeHandler.class)
 * private List&lt;Long&gt; applicableClassIds;
 * </pre>
 */
public class PgJsonbTypeHandler implements TypeHandler<Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.OTHER);
            return;
        }
        try {
            String json = toJson(parameter);
            ps.setObject(i, json, Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new TypeException("PgJsonbTypeHandler: failed to serialize parameter to JSON: " + parameter, e);
        }
    }

    @Override
    public Object getResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getObject(columnName));
    }

    @Override
    public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getObject(columnIndex));
    }

    @Override
    public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getObject(columnIndex));
    }

    /**
     * 将对象序列化为 JSON 字符串。
     * 已经是 String 的直接返回，避免双重序列化。
     */
    private String toJson(Object parameter) throws JsonProcessingException {
        if (parameter instanceof String s) {
            // 验证是合法 JSON 后原样返回
            MAPPER.readTree(s);
            return s;
        }
        return MAPPER.writeValueAsString(parameter);
    }

    /**
     * 从数据库读取的原始值（PGobject / String / null）反序列化为目标对象。
     * 返回 Object，调用方根据字段类型自行 cast。
     */
    private Object parseJson(Object raw) {
        if (raw == null) {
            return null;
        }
        String json = extractString(raw);
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new TypeException("PgJsonbTypeHandler: failed to parse JSON from DB: " + json, e);
        }
    }

    private String extractString(Object raw) {
        if (raw instanceof String) {
            return (String) raw;
        }
        if (raw instanceof PGobject pg) {
            return pg.getValue();
        }
        return raw.toString();
    }
}
