package com.cloud.hello;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class HelloWorldResponse extends BaseResponse {
    @SerializedName("message")
    @Param(description = "the message from the heavens")
    private String message = "Hello world!";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
