package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public class DocumentParseRequest {

    @Schema(description = "是否强制重新解析已成功的文档", defaultValue = "false")
    private Boolean force = false;

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public boolean forceEnabled() {
        return Boolean.TRUE.equals(force);
    }
}
