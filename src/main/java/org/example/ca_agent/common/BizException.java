package org.example.ca_agent.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private Integer code;
    private String message;

    public BizException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
