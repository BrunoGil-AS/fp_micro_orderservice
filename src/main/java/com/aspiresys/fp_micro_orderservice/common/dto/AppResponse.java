package com.aspiresys.fp_micro_orderservice.common.dto;

import lombok.ToString;

/**
 * A generic response wrapper class used to standardize API responses.
 *
 * @param <T> the type of the response data
 */
@ToString
public class AppResponse<T> {
    private String message;
    private T data;

    public AppResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
