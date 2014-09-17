package com.cloud.baremetal.networkservice;

import com.cloud.agent.api.Command;

public class PrepareBasicPxeServerCommand extends Command {
    private String kernel;
    private String append;
    private String templateUuid;
    private String mac;

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getKernel() {
        return kernel;
    }

    public void setKernel(String kernel) {
        this.kernel = kernel;
    }

    public String getAppend() {
        return append;
    }

    public void setAppend(String append) {
        this.append = append;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

}
