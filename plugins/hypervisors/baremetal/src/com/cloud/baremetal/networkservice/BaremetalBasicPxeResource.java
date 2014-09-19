package com.cloud.baremetal.networkservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VirtualMachine.State;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;

public class BaremetalBasicPxeResource extends BaremetalPxeResourceBase {
    private static final Logger logger = Logger.getLogger(BaremetalBasicPxeResource.class);
    private String tftpDir;

    public interface Constants {
        String PREPARE_SCRIPT  = "scripts/network/basicpxe/prepare_basicpxe_bootfile.py";
        String USERDATA_SCRIPT = "scripts/network/ping/baremetal_user_data.py";

        String REMOTE_SCRIPT_PATH = "/usr/bin/";
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) return false;

        tftpDir = (String)params.get(BaremetalPxeService.PXE_PARAM_TFTP_DIR);
        if (tftpDir == null)
            throw new ConfigurationException("No tftp directory specified");

        Connection sshConnection = null;

        try {
            sshConnection = openSSHConnection();
            SCPClient scp = new SCPClient(sshConnection);

            String cmd = String.format("[ -f /%1$s/pxelinux.0 ]", tftpDir);
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, cmd)) {
                throw new ConfigurationException("Missing files in TFTP directory: " + tftpDir + ". Check if pxelinux.0 are here");
            }

            String prepareScriptPath = Script.findScript("", Constants.PREPARE_SCRIPT);
            if (prepareScriptPath == null)
                throw new ConfigurationException("Unable to find prepare_basic_pxe_bootfile.py at " + Constants.PREPARE_SCRIPT);

            String userDataScriptPath = Script.findScript("", Constants.USERDATA_SCRIPT);
            if (userDataScriptPath == null)
                throw new ConfigurationException("Can not find baremetal_user_data.py at " + Constants.USERDATA_SCRIPT);

            scp.put(prepareScriptPath, Constants.REMOTE_SCRIPT_PATH, "0755");
            scp.put(userDataScriptPath, Constants.REMOTE_SCRIPT_PATH, "0755");

            return true;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        } finally {
            if (sshConnection != null) {
                sshConnection.close();
            }
        }
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        com.trilead.ssh2.Connection sshConnection = SSHCmdHelper.acquireAuthorizedConnection(_ip, _username, _password);
        if (sshConnection == null) {
            return null;
        } else {
            SSHCmdHelper.releaseSshConnection(sshConnection);
            return new PingRoutingCommand(getType(), id, new HashMap<String, State>());
        }
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof PrepareBasicPxeServerCommand) {
            return execute((PrepareBasicPxeServerCommand) cmd);
        } else if (cmd instanceof VmDataCommand) {
            return execute((VmDataCommand)cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    private Connection openSSHConnection() throws IOException {
        Connection sshConnection = new Connection(_ip, 22);

        sshConnection.connect(null, 60000, 60000);
        if (!sshConnection.authenticateWithPassword(_username, _password)) {
            logger.debug("SSH Failed to authenticate");
            throw new IOException(String.format("Cannot connect to Basic PXE server(IP=%1$s, username=%2$s, password=%3$s", _ip, _username, "******"));
        }

        return sshConnection;
    }

    private Answer execute(VmDataCommand cmd) {
        Connection sshConnection = null;

        try {
            List<String[]> vmData = cmd.getVmData();
            StringBuilder sb = new StringBuilder();
            for (String[] data : vmData) {
                String folder = data[0];
                String file = data[1];
                String contents = (data[2] == null) ? "none" : data[2];
                sb.append(cmd.getVmIpAddress());
                sb.append(",");
                sb.append(folder);
                sb.append(",");
                sb.append(file);
                sb.append(",");
                sb.append(contents);
                sb.append(";");
            }
            String arg = StringUtils.stripEnd(sb.toString(), ";");

            sshConnection = openSSHConnection();

            String script = String.format("python /usr/bin/baremetal_user_data.py '%s'", arg);
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, script)) {
                return new Answer(cmd, false, "Failed to add user data, command:" + script);
            }

            return new Answer(cmd, true, "Success");
        }  catch (IOException e){
            logger.debug("Preparing VM data failed", e);
            return new Answer(cmd, false, e.getMessage());
        } finally {
            if (sshConnection != null) {
                sshConnection.close();
            }
        }
    }

    private Answer execute(PrepareBasicPxeServerCommand cmd) {
        Connection sshConnection = null;
        try {
            sshConnection = openSSHConnection();

            String script = String.format(
                "python /usr/bin/prepare_basic_pxe_bootfile.py %s %s %s %s",
                tftpDir,
                cmd.getMac(),
                Base64.encodeBase64String(cmd.getKernel().getBytes()),
                Base64.encodeBase64String(cmd.getAppend().getBytes())
            );

            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, script)) {
                logger.debug("Prepare for Basic PXE server failed, cmd: " + script);
                return new Answer(cmd, false, "Prepare basic pxe server failed, command:" + script);
            }

            logger.debug("Prepare for Basic PXE server successful");
            return new Answer(cmd, true, "Success");
        } catch (IOException e) {
            logger.debug("Prepare for Basic PXE server failed", e);
            return new Answer(cmd, false, e.getMessage());
        } finally {
            if (sshConnection != null) {
                sshConnection.close();
            }
        }
    }

}
