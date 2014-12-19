package com.cloud.bridge.auth.ec2;

import java.util.Map;
import java.util.Set;

/**
 * ApiKeyStore
 * 
 * Interface for API key store methods 
 *
 */
public interface ApiKeyStore {
	/**
	 * getSecretApiKey
	 * 
	 * Retrieve the unique secret key associated with the given public API key.
	 * 
	 * @param publicApiKey	The public API key
	 * @return 				The secret API key
	 */
	public String getSecretApiKey(String publicApiKey);
	
	/**
	 * getRegionSecretApiKey
	 * 
	 * Retrieve a unique secret key associated with the given public API key
	 * and the given region. Note that the given public key may be associated
	 * with a different region than the given, the key store implementation must 
	 * associate public keys with users.
	 *  
	 * 
	 * @param publiApiKey 	The public API key
	 * @param region		The cloud region, e.g. https://cloud.google.com/compute/docs/zones
	 * @return				The
	 */
	public Map<String, String> getRegionSecretApiKey(String publiApiKey, String region);
	
	/**
	 * getSecretApiKeySet
	 * 
	 * Retrieve the complete set of secret keys associated with the given public API key
	 * for the given regions, or all regions in the key store if none are specified.
	 * The key store implementation must associate public keys with users.
	 * 
	 * @param publiApiKey	The public API key
	 * @param regions		The regions to be queried, or empty to query all regions.
	 * @return				A <region, secret> map.
	 */
	public Map<String, String> getSecretApiKeySet(String publiApiKey, Set<String> regions);	

}
