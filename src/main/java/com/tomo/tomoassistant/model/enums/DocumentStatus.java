package com.tomo.tomoassistant.model.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public enum DocumentStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "处理失败");

    private final int code;
    private final String description;

    public static DocumentStatus fromCode(int code) {
        for (DocumentStatus status : values()) {
            if (status.code == code) return status;
        }
        return PENDING;
    }
}
