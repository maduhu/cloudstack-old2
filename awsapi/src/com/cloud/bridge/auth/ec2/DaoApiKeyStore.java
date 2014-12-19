package com.cloud.bridge.auth.ec2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.cloud.bridge.persist.dao.CloudStackUserDao;

public class DaoApiKeyStore implements ApiKeyStore {
	private CloudStackUserDao userDao;
	
	public DaoApiKeyStore(CloudStackUserDao userInfo) {
		userDao = userInfo;
	}

	@Override
	public String getSecretApiKey(String publicApiKey) {	
		return userDao.getSecretKeyByAccessKey(publicApiKey);
	}

	@Override
	public Map<String, String> getRegionSecretApiKey(String publicApiKey, String region) {
		// Ignore region
		Map<String, String> result = new HashMap<String, String>(1);
		result.put(region, getSecretApiKey(publicApiKey));
		return result;
	}

	@Override
	public Map<String, String> getSecretApiKeySet(String publicApiKey, Set<String> regions) {
		// Ignore regions
		if (regions == null) {
			return null;
		}
		Map<String, String> result = new HashMap<String, String>(regions.size());
		for (String region : regions) {
			result.put(region, getSecretApiKey(publicApiKey));	
		}	
		return result;
	}

}
