package com.cloud.baremetal.networkservice;

import static org.junit.Assert.*;

import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class BaremetalBasicPxeServiceImplTest {

    @Test(expected=CloudRuntimeException.class)
    public void testParseTemplateIdentifierEmptyStr() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        service.parseTemplateIdentifier("");
    }

    @Test(expected=CloudRuntimeException.class)
    public void testParseTemplateIdentifierInvalidId() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        service.parseTemplateIdentifier("invalid:identifier");
    }

    @Test(expected=CloudRuntimeException.class)
    public void testParseTemplateIdentifierInvalidIdUnsupportedParam() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        service.parseTemplateIdentifier("pxe:kernel=some/kernel&unsupported_param=value");
    }

    @Test(expected=CloudRuntimeException.class)
    public void testParseTemplateIdentifierInvalidIdMissingKernelParam() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        service.parseTemplateIdentifier("pxe:append=value");
    }

    @Test(expected=CloudRuntimeException.class)
    public void testParseTemplateIdentifierInvalidIdMissingAppendParam() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        service.parseTemplateIdentifier("pxe:kernel=value");
    }

    @Test
    public void testParseTemplateIdentifier() {
        BaremetalBasicPxeServiceImpl service = new BaremetalBasicPxeServiceImpl();
        PrepareBasicPxeServerCommand cmd = service.parseTemplateIdentifier("pxe:kernel=some/kernel&append=some%20append%20str%20key%3Dvalue");

        assertTrue(cmd.getKernel().equals("some/kernel"));
        assertTrue(cmd.getAppend().equals("some append str key=value"));
    }
}
