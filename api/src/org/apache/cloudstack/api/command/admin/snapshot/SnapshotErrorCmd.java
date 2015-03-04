package org.apache.cloudstack.api.command.admin.snapshot;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.Snapshot;

@APICommand(name = "forceSnapshotError", description = "Forces a snapshot into error state.", responseObject = SnapshotResponse.class)
public class SnapshotErrorCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(SnapshotErrorCmd.class.getName());
    private static final String s_name = "forcesnapshoterrorresponse";
    
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = SnapshotResponse.class,
            description="lists snapshot by snapshot ID")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    
    public Long getSnapshotId() {
        return id;
    }

    // ///////////////////////////////////null////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "snapshot";
    }

    @Override
    public long getEntityOwnerId() {
        Snapshot snapshot = _entityMgr.findById(Snapshot.class, getSnapshotId());
        if (snapshot == null) {
            throw new InvalidParameterValueException("Unable to find snapshot by id=" + getSnapshotId());
        }
        return snapshot.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SNAPSHOT_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "forcing snapshot to error state: " + getSnapshotId();
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Snapshot;
    }

    @Override
    public void execute() {        
        Snapshot snapshot = _snapshotService.forceErrorState(getSnapshotId());
        SnapshotResponse response = _responseGenerator.createSnapshotResponse(snapshot);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
