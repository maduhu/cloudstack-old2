package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2DescribeSubnetsResponse {
	
	private List<EC2Subnet> subnetSet = new ArrayList<EC2Subnet>();    

	public EC2DescribeSubnetsResponse() {
	}
	
	public void addSubnet( EC2Subnet param ) {
		subnetSet.add( param );
	}
	
	public EC2Subnet[] getSubnetSet() {
		return subnetSet.toArray(new EC2Subnet[0]);
	}

}
