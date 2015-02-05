package com.cloud.agent.api.routing;

import com.cloud.agent.api.Answer;

public class IpListAnswer extends Answer {
    String[] ips;
    
    public IpListAnswer(IpListCommand cmd, String[] ips) {
        this.ips = ips;
        this.result = true;
        this.details = null;
    }
    
    public String[] getIps() {
        return ips;
    }
}
