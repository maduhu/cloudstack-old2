package com.cloud.hello;

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.component.PluggableService;

public class HelloWorldManagerImpl implements PluggableService {

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<>();
        cmds.add(HelloWorldCmd.class);
        return cmds;
    }

}
