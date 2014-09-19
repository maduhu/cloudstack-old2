package com.cloud.baremetal.networkservice;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.AddBaremetalBasicPxeServerCmd;
import org.apache.cloudstack.api.AddBaremetalPxeCmd;
import org.apache.cloudstack.api.ListBaremetalPxeServersCmd;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand.BootDev;
import com.cloud.baremetal.database.BaremetalPxeDao;
import com.cloud.baremetal.database.BaremetalPxeVO;
import com.cloud.baremetal.networkservice.BaremetalPxeManager.BaremetalPxeType;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = BaremetalPxeService.class)
public class BaremetalBasicPxeServiceImpl extends BareMetalPxeServiceBase implements BaremetalPxeService {
    private static final Logger s_logger = Logger.getLogger(BaremetalBasicPxeServiceImpl.class);

    @Inject ResourceManager _resourceMgr;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject HostDetailsDao _hostDetailsDao;
    @Inject BaremetalPxeDao _pxeDao;
    @Inject NetworkDao _nwDao;
    @Inject VMTemplateDao _tmpDao;

    @Override
    public boolean prepare(VirtualMachineProfile<UserVmVO> profile, NicProfile nic, DeployDestination dest, ReservationContext context) {
        NetworkVO nwVO = _nwDao.findById(nic.getNetworkId());
        SearchCriteriaService<BaremetalPxeVO, BaremetalPxeVO> sc = SearchCriteria2.create(BaremetalPxeVO.class);
        sc.addAnd(sc.getEntity().getDeviceType(), Op.EQ, BaremetalPxeType.BASIC_PXE.toString());
        sc.addAnd(sc.getEntity().getPhysicalNetworkId(), Op.EQ, nwVO.getPhysicalNetworkId());

        BaremetalPxeVO pxeVo = sc.find();
        if (pxeVo == null)
            throw new CloudRuntimeException("No Basic PXE server found in pod: " + dest.getPod().getId() + ", you need to add it before starting a VM");

        VMTemplateVO template = _tmpDao.findById(profile.getTemplateId());

        try {
            String tpl = profile.getTemplate().getUrl();
            PrepareBasicPxeServerCommand cmd = parseTemplateIdentifier(tpl);

            cmd.setMac(nic.getMacAddress());
            cmd.setTemplateUuid(template.getUuid());

            Answer answer = _agentMgr.send(pxeVo.getHostId(), cmd);
            if (!answer.getResult()) {
                s_logger.warn("Unable to set host: " + dest.getHost().getId() + " to PXE boot because " + answer.getDetails());
                return answer.getResult();
            }

            answer = _agentMgr.send(dest.getHost().getId(), new IpmISetBootDevCommand(BootDev.pxe));
            if (!answer.getResult()) {
                s_logger.warn("Unable to set host: " + dest.getHost().getId() + " to PXE boot because " + answer.getDetails());
            }

            return answer.getResult();
        } catch (Exception e) {
            s_logger.warn("Cannot prepare PXE server", e);
            return false;
        }
    }

    @Override
    public boolean prepareCreateTemplate(Long pxeServerId, UserVm vm, String templateUrl) {
        return false;
    }

    @Override
    @DB
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public BaremetalPxeVO addPxeServer(AddBaremetalPxeCmd cmd) {
        AddBaremetalBasicPxeServerCmd bcmd = (AddBaremetalBasicPxeServerCmd)cmd;

        if (cmd.getPhysicalNetworkId() == null || cmd.getUrl() == null || cmd.getUsername() == null || cmd.getPassword() == null)
            throw new IllegalArgumentException("At least one of the required parameters(physical network id, url, username, password) is null");

        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(cmd.getPhysicalNetworkId());
        if (pNetwork == null)
            throw new IllegalArgumentException("Could not find phyical network with ID: " + cmd.getPhysicalNetworkId());

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), BaremetalPxeManager.BAREMETAL_PXE_SERVICE_PROVIDER.getName());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + BaremetalPxeManager.BAREMETAL_PXE_SERVICE_PROVIDER.getName() +
                    " is not enabled in the physical network: " + cmd.getPhysicalNetworkId() + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() +
                    " is in shutdown state in the physical network: " + cmd.getPhysicalNetworkId() + "to add this device");
        }

        if (bcmd.getTftpDir() == null)
            throw new IllegalArgumentException("No TFTP directory specified");

        URI uri;
        try {
            uri = new URI(cmd.getUrl());
        } catch (Exception e) {
            s_logger.debug(e);
            throw new IllegalArgumentException(e.getMessage());
        }
        String ipAddress = uri.getHost();
        if (ipAddress == null)
            ipAddress = cmd.getUrl();

        long zoneId = pNetwork.getDataCenterId();
        String guid = getPxeServerGuid(Long.toString(zoneId), BaremetalPxeType.BASIC_PXE.toString(), ipAddress);

        Map params = new HashMap<String, String>();
        params.put(BaremetalPxeService.PXE_PARAM_ZONE, Long.toString(zoneId));
        params.put(BaremetalPxeService.PXE_PARAM_IP, ipAddress);
        params.put(BaremetalPxeService.PXE_PARAM_USERNAME, cmd.getUsername());
        params.put(BaremetalPxeService.PXE_PARAM_PASSWORD, cmd.getPassword());
        params.put(BaremetalPxeService.PXE_PARAM_GUID, guid);
        params.put(BaremetalPxeService.PXE_PARAM_TFTP_DIR, bcmd.getTftpDir() );

        ServerResource resource = new BaremetalBasicPxeResource();
        try {
            resource.configure("BasicPXE resource", params);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }

        Host pxeServer = _resourceMgr.addHost(zoneId, resource, Host.Type.BaremetalPxe, params);
        if (pxeServer == null)
            throw new CloudRuntimeException("Unable to add PXE server as a host");

        Transaction txn = Transaction.currentTxn();
        BaremetalPxeVO vo = new BaremetalPxeVO();

        vo.setHostId(pxeServer.getId());
        vo.setNetworkServiceProviderId(ntwkSvcProvider.getId());
        vo.setPhysicalNetworkId(cmd.getPhysicalNetworkId());
        vo.setDeviceType(BaremetalPxeType.BASIC_PXE.toString());

        txn.start();
        _pxeDao.persist(vo);
        txn.commit();

        return vo;
    }

    @Override
    public BaremetalPxeResponse getApiResponse(BaremetalPxeVO vo) {
        BaremetalPxeResponse response = new BaremetalPxeResponse();
        response.setId(vo.getUuid());
        HostVO host = _hostDao.findById(vo.getHostId());
        response.setUrl(host.getPrivateIpAddress());
        PhysicalNetworkServiceProviderVO providerVO = _physicalNetworkServiceProviderDao.findById(vo.getNetworkServiceProviderId());
        response.setPhysicalNetworkId(providerVO.getUuid());
        PhysicalNetworkVO nwVO = _physicalNetworkDao.findById(vo.getPhysicalNetworkId());
        response.setPhysicalNetworkId(nwVO.getUuid());
        response.setObjectName("baremetalpxeserver");
        return response;
    }

    @Override
    public List<BaremetalPxeResponse> listPxeServers(ListBaremetalPxeServersCmd cmd) {
        return null;
    }

    @Override
    public String getPxeServiceType() {
        return BaremetalPxeManager.BaremetalPxeType.BASIC_PXE.toString();
    }

    protected PrepareBasicPxeServerCommand parseTemplateIdentifier(String identifier) {
        PrepareBasicPxeServerCommand cmd = new PrepareBasicPxeServerCommand();
        String errStr = String.format("Template identifier [%s] is not correctly encoded. It must be in format of pxe:kernel=<str>&append=<str>", identifier);
        CloudRuntimeException err = new CloudRuntimeException(errStr);

        if (!identifier.startsWith("pxe:")) throw err;
        String[] tpls = identifier.substring("pxe:".length()).split("&");

        if (tpls.length != 2)
            throw err;

        try {
            for (String t : tpls) {
                String[] kv = t.split("=");

                if (kv.length != 2) throw err;
                else if (kv[0].equals("kernel")) cmd.setKernel(URLDecoder.decode(kv[1], "UTF-8"));
                else if (kv[0].equals("append")) cmd.setAppend(URLDecoder.decode(kv[1], "UTF-8"));
                else throw err;
            }
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        }

        if (StringUtils.isEmpty(cmd.getKernel())) throw err;
        if (StringUtils.isEmpty(cmd.getAppend())) throw err;

        return cmd;
    }
}
