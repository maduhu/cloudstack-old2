package com.cloud.baremetal.networkservice;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.baremetal.database.BaremetalPxeVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=BaremetalPxeVO.class)
public class BaremetalBasicPxeResponse extends BaremetalPxeResponse {
    @SerializedName(ApiConstants.TFTP_DIR) @Param(description="TFTP root directory of the PXE server")
    private String tftpDir;

    public String getTftpDir() {
        return tftpDir;
    }

    public void setTftpDir(String tftpDir) {
        this.tftpDir = tftpDir;
    }
}
