package io.brahmaos.wallet.brahmawallet.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Unified API response object
 * The response structure returned by all owned services is unified
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Web3jRespResult {

    @JsonProperty("id")
    private int id;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("error")
    private Object error;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "Web3jRespResult{" +
                "id=" + id +
                ", result=" + result +
                ", jsonrpc='" + jsonrpc + '\'' +
                ", error=" + error +
                '}';
    }
}
