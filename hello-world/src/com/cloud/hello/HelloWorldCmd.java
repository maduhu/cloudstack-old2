package com.cloud.hello;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ResponseObject.ResponseView;

import com.cloud.user.Account;

@APICommand(name = "helloWorld", description = "Say hello.", responseObject = HelloWorldResponse.class, responseView = ResponseView.Restricted)
public class HelloWorldCmd extends BaseCmd {

    private static final String s_name = "helloworldresponse";

    @Override
    public void execute() {
        HelloWorldResponse resp = new HelloWorldResponse();
        resp.setObjectName("hellomessage");
        resp.setResponseName(getCommandName());
        setResponseObject(resp);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
