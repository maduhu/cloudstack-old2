package com.cloud.network;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network.GuestType;
import com.cloud.resource.ResourceState;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;

public class NetworkHealthMonitor extends AdapterBase {
    @Inject protected NetworkModel _networkModel;
    @Inject protected NetworkService _networkService;
    @Inject protected ConfigurationDao _configDao;
    
    protected static final Logger s_logger = Logger.getLogger(NetworkHealthMonitor.class);
    
    //configurable parameters
    protected long runTime; //default 3 minutes.
    protected ScheduledExecutorService executor; //thread executor.
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration();
        
        runTime = NumbersUtil.parseLong(configs.get("network.healthcheck.interval"), 180000);
        return super.configure(name, params);
    }
    
    @Override
    public boolean start() {
        init();
        return super.start();
    }
    
    private void init() {
        executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NetworkHealthMonitor"));
        executor.scheduleWithFixedDelay(new IpFixer(), 15000L, runTime, TimeUnit.MILLISECONDS);
    }
    
    class IpFixer implements Runnable {
        @Override public void run() {
            s_logger.info("NetworkHealthMonitor is running...");
            List<? extends Network> networks = _networkModel.listAllNetworksInAllZonesByType(GuestType.Isolated);
            
            for (Network network : networks) {
                try {
                    s_logger.info("Checking health for network " + network.getUuid());
                    _networkService.checkHealth(network);
                } 
                catch (Exception e) {
                    s_logger.error("Unable to resync IPs on network", e);
                }
            }
            
            s_logger.info("Finished NetworkHealthMonitor run.");
        }
    }
}
