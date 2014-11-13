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
package com.cloud.bridge.service.core.ec2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;

public class EC2SubnetFilterSet {
    protected final static Logger logger = Logger.getLogger(EC2ImageFilterSet.class);

    protected List<EC2Filter> filterSet = new ArrayList<EC2Filter>();
    private Map<String,String> filterTypes = new HashMap<String,String>();

	public EC2SubnetFilterSet() {
        // -> supported filters
        filterTypes.put( "availabilityZone", "string" );
        filterTypes.put("availability-zone", "string");
        
        filterTypes.put( "available-ip-address-count",  "string" );
        
        filterTypes.put( "cidrBlock",   "string" );
        filterTypes.put( "cidr",   "string" );
        filterTypes.put( "cidr-block",   "string" );
        
        filterTypes.put( "defaultForAz",   "boolean" );
        filterTypes.put( "default-for-az",   "boolean" );
        
        filterTypes.put( "state",   "string" );
        
        filterTypes.put( "subnet-id",   "string" );
        
        filterTypes.put( "vpc-id",   "string" );
        
        filterTypes.put( "tag-key", "string" );
        filterTypes.put( "tag-value", "string" );
	}

    public void addFilter( EC2Filter param ) {
        String filterName = param.getName();
        if ( !filterName.startsWith("tag:") ) {
            String value = (String) filterTypes.get( filterName );
            if ( value == null || value.equalsIgnoreCase("null")) {
                throw new EC2ServiceException( ClientError.InvalidFilter, "Filter '" + filterName + "' is invalid");
            }
        }

        filterSet.add( param );
    }

    public EC2Filter[] getFilterSet() {
        return filterSet.toArray(new EC2Filter[0]);
    }

    public EC2DescribeSubnetsResponse evaluate( EC2DescribeSubnetsResponse sampleList) throws ParseException	{
        EC2DescribeSubnetsResponse resultList = new EC2DescribeSubnetsResponse();

        boolean matched;

        EC2Subnet[] subnetSet = sampleList.getSubnetSet();
        EC2Filter[] filterSet = getFilterSet();
        for (EC2Subnet subnet : subnetSet) {
            matched = true;
            for (EC2Filter filter : filterSet) {
                if (!filterMatched(subnet, filter)) {
                    matched = false;
                    break;
                }
            }
            if (matched == true)
                resultList.addSubnet(subnet);
        }
        return resultList;
  
    }

private boolean filterMatched( EC2Subnet subnet, EC2Filter filter ) throws ParseException {
        String filterName = filter.getName();
        String[] valueSet = filter.getValueSet();

        if ( filterName.equalsIgnoreCase( "availabilityZone" ) || filterName.equalsIgnoreCase("availability-zone"))
            return containsString( subnet.getAvailabilityZone(), valueSet );
        if ( filterName.equalsIgnoreCase( "available-ip-address-count" ))
            return containsString( Integer.toString(subnet.getAvailable_ips()), valueSet );
        if ( filterName.equalsIgnoreCase( "cidrBlock" ) || filterName.equalsIgnoreCase( "cidr" ) 
        		|| filterName.equalsIgnoreCase( "cidr-block" ))
            return containsString( subnet.getCidr(), valueSet );
        if ( filterName.equalsIgnoreCase( "defaultForAz" ) || filterName.equalsIgnoreCase( "default-for-az" ))
            return containsString( Boolean.toString(subnet.getDefaultForAvailabilityZone()), valueSet);
        if ( filterName.equalsIgnoreCase( "state" ))
            return containsString( subnet.getState(), valueSet );
        if ( filterName.equalsIgnoreCase( "subnet-id" ))
            return containsString(subnet.getId(), valueSet);
        if ( filterName.equalsIgnoreCase( "vpc-id" ))
            return containsString( subnet.getVpcId(), valueSet );
      
        
        else if (filterName.equalsIgnoreCase("tag-key"))
        {
            EC2TagKeyValue[] tagSet = subnet.getResourceTags();
            for (EC2TagKeyValue tag : tagSet)
                if (containsString(tag.getKey(), valueSet)) return true;
            return false;
        }
        else if (filterName.equalsIgnoreCase("tag-value"))
        {
            EC2TagKeyValue[] tagSet = subnet.getResourceTags();
            for (EC2TagKeyValue tag : tagSet){
                if (tag.getValue() == null) {
                    if (containsEmptyValue(valueSet)) return true;
                }
                else {
                    if (containsString(tag.getValue(), valueSet)) return true;
                }
            }
            return false;
        }
        else if (filterName.startsWith("tag:"))
        {
            String key = filterName.split(":")[1];
            EC2TagKeyValue[] tagSet = subnet.getResourceTags();
            for (EC2TagKeyValue tag : tagSet){
                if (tag.getKey().equalsIgnoreCase(key)) {
                    if (tag.getValue() == null) {
                        if (containsEmptyValue(valueSet)) return true;
                    }
                    else {
                        if (containsString(tag.getValue(), valueSet)) return true;
                    }
                }
            }
            return false;
        }
        else return false;
    }

    private boolean containsString( String lookingFor, String[] set ) {
        if (lookingFor == null)
            return false;

        for (String filter: set) {
            if (lookingFor.matches( filter )) return true;
        }
        return false;
    }

    private boolean containsEmptyValue( String[] set ) {
        for( int i=0; i < set.length; i++ ) {
            if (set[i].isEmpty()) return true;
        }
        return false;
    }
    

}
