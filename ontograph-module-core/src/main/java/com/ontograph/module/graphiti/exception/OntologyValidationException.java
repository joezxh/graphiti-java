package com.ontograph.module.graphiti.exception;

import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.graphiti.vo.ontology.ValidationResultVO;
import lombok.Getter;

@Getter
public class OntologyValidationException extends BusinessException {
    private final ValidationResultVO validationResult;

    public OntologyValidationException(ValidationResultVO validationResult) {
        super(2001, buildMessage(validationResult));
        this.validationResult = validationResult;
    }

    private static String buildMessage(ValidationResultVO result) {
        if (result.getErrors() == null || result.getErrors().isEmpty()) {
            return "本体校验失败";
        }
        StringBuilder sb = new StringBuilder("本体校验失败: ");
        for (int i = 0; i < Math.min(3, result.getErrors().size()); i++) {
            if (i > 0) sb.append("; ");
            sb.append(result.getErrors().get(i).getMessage());
        }
        if (result.getErrors().size() > 3) {
            sb.append(" (共 ").append(result.getErrors().size()).append(" 条错误)");
        }
        return sb.toString();
    }
}
