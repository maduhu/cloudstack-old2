package com.cloud.agent.api.routing;

/**
 * Requests a list of IPs present on the network element.
 *
 */
public class IpListCommand extends NetworkElementCommand {
    String[] vifMacAddresses;
    
    public IpListCommand() {}
    
    public IpListCommand(String ... macAddresses) {
        this.vifMacAddresses = macAddresses;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
    
    public String[] getVifMacAddresses() {
        return vifMacAddresses;
    }
    
    public void setVifMacAddresses(String[] vifMacAddresses) {
        this.vifMacAddresses = vifMacAddresses;
    }
}
