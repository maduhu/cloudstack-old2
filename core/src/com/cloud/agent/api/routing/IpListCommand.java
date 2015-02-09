package com.cloud.agent.api.routing;

/**
 * Requests a list of IPs present on the network element.
 *
 */
public class IpListCommand extends NetworkElementCommand {
    String[] vlanIds;
    
    public IpListCommand() {}
    
    public IpListCommand(String ... macAddresses) {
        this.vlanIds = macAddresses;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
    
    public String[] getVlanIds() {
        return vlanIds;
    }
    
    public void setVlanIds(String[] vifMacAddresses) {
        this.vlanIds = vifMacAddresses;
    }
}
