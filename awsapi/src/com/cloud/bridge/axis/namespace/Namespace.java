package com.cloud.bridge.axis.namespace;

/**
 * Supported namespace versions of the Amazon EC2 WSDL.
 */
public enum Namespace {

    // Supported namespaces, in descending order of age.

    EC2_2014_02_01("http://ec2.amazonaws.com/doc/2014-02-01/", "2014-02-01"),
    EC2_2013_02_01("http://ec2.amazonaws.com/doc/2013-02-01/", "2013-02-01"),
    EC2_2012_12_01("http://ec2.amazonaws.com/doc/2012-12-01/", "2012-12-01"),
    EC2_2012_08_15("http://ec2.amazonaws.com/doc/2012-08-15/", "2012-08-15"),
    EC2_2012_07_20("http://ec2.amazonaws.com/doc/2012-07-20/", "2012-07-20"),
    EC2_2012_06_15("http://ec2.amazonaws.com/doc/2012-06-15/", "2012-06-15"),
    EC2_2012_06_01("http://ec2.amazonaws.com/doc/2012-06-01/", "2012-06-01"),
    EC2_2012_05_01("http://ec2.amazonaws.com/doc/2012-05-01/", "2012-05-01"),
    EC2_2012_04_01("http://ec2.amazonaws.com/doc/2012-04-01/", "2012-04-01"),
    EC2_2012_03_01("http://ec2.amazonaws.com/doc/2012-03-01/", "2012-03-01"),
    EC2_2011_12_15("http://ec2.amazonaws.com/doc/2011-12-15/", "2011-12-15"),
    EC2_2011_11_01("http://ec2.amazonaws.com/doc/2011-11-01/", "2011-11-01"),
    EC2_2011_07_15("http://ec2.amazonaws.com/doc/2011-07-15/", "2011-07-15"),
    EC2_2011_05_15("http://ec2.amazonaws.com/doc/2011-05-15/", "2011-05-15"),
    EC2_2011_02_28("http://ec2.amazonaws.com/doc/2011-02-28/", "2011-02-28"),
    EC2_2011_01_01("http://ec2.amazonaws.com/doc/2011-01-01/", "2011-01-01"),
    EC2_2010_11_15("http://ec2.amazonaws.com/doc/2010-11-15/", "2010-11-15"),
    EC2_2010_08_31("http://ec2.amazonaws.com/doc/2010-08-31/", "2010-08-31"),
    EC2_2010_06_15("http://ec2.amazonaws.com/doc/2010-06-15/", "2010-06-15"),
    EC2_2009_11_30("http://ec2.amazonaws.com/doc/2009-11-30/", "2009-11-30"),
    EC2_2009_08_15("http://ec2.amazonaws.com/doc/2009-08-15/", "2009-08-15"),
    EC2_2008_12_01("http://ec2.amazonaws.com/doc/2008-12-01/", "2008-12-01"),
    EC2_2008_05_05("http://ec2.amazonaws.com/doc/2008-05-05/", "2008-05-05");

    private String namespaceUrl;
    private String namespaceVersion;

    Namespace(String namespaceUrl, String namespaceVersion) {
        this.namespaceUrl = namespaceUrl;
        this.namespaceVersion = namespaceVersion;
    }

    public String getUrl() {
        return namespaceUrl;
    }

    public String getVersion() {
        return namespaceVersion;
    }

    public static Namespace getCurrent() {
        return Namespace.values()[0];
    }

    public boolean isAtLeast(Namespace targetNamespace) {
        return this.ordinal() <= targetNamespace.ordinal();
    }
}
