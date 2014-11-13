package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2DescribeSubnets {

	
	private List<String> subnetIds = new ArrayList<String>();     // a list of subnet ids we are interested in
	private EC2SubnetFilterSet ifs = null;
	
	public EC2DescribeSubnets() {
		
	}
	
	
	public List<String> getSubnetIds() {
		return subnetIds;
	}
	public void addSubnetIds (String subnetId) {
		this.subnetIds.add(subnetId);
	}
	public EC2SubnetFilterSet getIfs() {
		return ifs;
	}
	public void setIfs(EC2SubnetFilterSet ifs) {
		this.ifs = ifs;
	}
	
	public String[] getSubnetIdsSet() {
		return this.subnetIds.toArray(new String [0]);
	}
}
