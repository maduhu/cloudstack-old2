package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2Subnet {
	
	private String subnetIdentifier;
	private String id;
	private String state;
	private String vpcId;
	private String cidr;
	private Integer available_ips; // The number of IP addresses in the subnet that are available
	private String availabilityZone;
	private Boolean defaultForAvailabilityZone; // Default subnet for the Availability Zone (true or false)
	private Boolean publicIPs; // Instances launched in this subnet receive a public IP address (true or false)
	private List<EC2TagKeyValue> tagsSet;
	
	
	public EC2Subnet() {
		subnetIdentifier = "SUBNET"; // TODO: should this be so?
		id = null;
		state = null;
		vpcId = null;
		cidr = null;
		available_ips = null;
		availabilityZone = null;
		defaultForAvailabilityZone = null;
		publicIPs = null;
		tagsSet = new ArrayList<EC2TagKeyValue>();
	}


	public String getSubnetIdentifier() {
		return subnetIdentifier;
	}


	public void setSubnetIdentifier(String subnetIdentifier) {
		this.subnetIdentifier = subnetIdentifier;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getState() {
		return state;
	}


	public void setState(String state) {
		this.state = state;
	}


	public String getVpcId() {
		return vpcId;
	}


	public void setVpcId(String vpcId) {
		this.vpcId = vpcId;
	}


	public String getCidr() {
		return cidr;
	}


	public void setCidr(String cidr) {
		this.cidr = cidr;
	}


	public Integer getAvailable_ips() {
		return available_ips;
	}


	public void setAvailable_ips(Integer available_ips) {
		this.available_ips = available_ips;
	}


	public String getAvailabilityZone() {
		return availabilityZone;
	}


	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}


	public Boolean getDefaultForAvailabilityZone() {
		return defaultForAvailabilityZone;
	}


	public void setDefaultForAvailabilityZone(Boolean defaultForAvailabilityZone) {
		this.defaultForAvailabilityZone = defaultForAvailabilityZone;
	}


	public Boolean getPublicIPs() {
		return publicIPs;
	}


	public void setPublicIPs(Boolean publicIPs) {
		this.publicIPs = publicIPs;
	}
	
    public void addResourceTag( EC2TagKeyValue param ) {
        tagsSet.add( param );
    }

    public EC2TagKeyValue[] getResourceTags() {
        return tagsSet.toArray(new EC2TagKeyValue[0]);
    }

	
}
