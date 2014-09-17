package org.apache.cloudstack.api;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;

import com.cloud.baremetal.networkservice.BaremetalBasicPxeResponse;

@APICommand(name="addBaremetalBasicPxeServer", description="Add a basic baremetal PXE server", responseObject = BaremetalBasicPxeResponse.class)
public class AddBaremetalBasicPxeServerCmd extends AddBaremetalPxeCmd {
    @Parameter(name=ApiConstants.TFTP_DIR, type=CommandType.STRING, required = true, description="Tftp root directory of PXE server")
    private String tftpDir;

    public String getTftpDir() {
        return tftpDir;
    }

    public void setTftpDir(String tftpDir) {
        this.tftpDir = tftpDir;
    }
}
