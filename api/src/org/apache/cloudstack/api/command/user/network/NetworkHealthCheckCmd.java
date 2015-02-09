// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.network;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.UserContext;

@APICommand(name = "networkHealthCheck", description="Checks the health of a network", responseObject=SuccessResponse.class)
public class NetworkHealthCheckCmd extends BaseAsyncCmd{
    public static final Logger s_logger = Logger.getLogger(NetworkHealthCheckCmd.class.getName());
    private static final String s_name = "networkhealthcheckresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = NetworkResponse.class,
            required=true, description="the ID of the network")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Network Id: " + id);
        boolean result = _networkService.checkHealth(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to check network health");
        }
    }


    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_HEALTH_CHECK;
    }

    @Override
    public String getEventDescription() {
        return  "Check health of network: " + id;
    }

    @Override
    public long getEntityOwnerId() {
        Network network = _networkService.getNetwork(id);
        if (network == null) {
            throw new InvalidParameterValueException("Networkd id=" + id + " doesn't exist");
        } else {
            return _networkService.getNetwork(id).getAccountId();
        }
    }
}
