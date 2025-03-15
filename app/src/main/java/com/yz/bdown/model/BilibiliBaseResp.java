package com.yz.bdown.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class BilibiliBaseResp implements Serializable {

    private static final long serialVersionUID = -5527049310820041311L;

    private int code;

    private String message;

    private int ttl;

    private Object data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @NonNull
    @Override
    public String toString() {
        return "BaseResp{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", ttl=" + ttl +
                ", data=" + data +
                '}';
    }
}
