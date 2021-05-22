package com.solrsearch.core.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd=SolrServerConfiguration.Config.class)
@Component(service=SolrServerConfiguration.class)
public class SolrServerConfiguration {
	
	@ObjectClassDefinition(name="Solr Search - Solr Configuration Service",
            description = "Service for configuring solr server configurations")
	public static @interface Config {
		@AttributeDefinition(name = "Solr Core URL",description = "Specify Solr server url")
		String solr_core_url() default StringUtils.EMPTY;
		
		@AttributeDefinition(name = "Solr Collections",
		              description = "Specify the collection name corresponding to the root content paths in the "
		              		+ "format {root content path : collection name}. Ex : content/solrsearch/en=solrsearch_en_collection ")
		String[] collection_names() default StringUtils.EMPTY;
		
		@AttributeDefinition(name = "Solr Credentials",
		              description = "Please specify the solr credentials in the format as '{username}:{password}'")
		String solr_credentials() default StringUtils.EMPTY;
		
		@AttributeDefinition(name = "HTTP Default Max Connection Per Route",
	              description = "Specify default max connections per route for HTTP Client")
	    int http_max_connection_per_route() default 2;
		
		@AttributeDefinition(name = "HTTP Max Connection",
	              description = "Specify total connections for HTTP Client")
	    int http_max_total_connections() default 10;
		
		
		}
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String solrCoreUrl;
		
	private String solrCredentials;
	
	private Map<String,String> solrCollections;
	
	private CloseableHttpClient httpClient;
	
	@Activate
    protected void activate(final Config config) {
		solrCoreUrl = config.solr_core_url();
		solrCredentials = config.solr_credentials();
		setSolrCollectionsHashMap(config.collection_names());
		final int defaultMaxPerRoute = config.http_max_connection_per_route();
		final int maxTotal = config.http_max_total_connections();
		final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connManager.setMaxTotal(maxTotal);
		httpClient = HttpClients.custom().setConnectionManager(connManager).build();
		
    }
	
	public SolrClient getSolrClient() {
		return new HttpSolrClient(solrCoreUrl, httpClient);
	}
	
	public SolrClient getAuthenticateSolrClient() {
		return new AuthenticateSolrClient(solrCoreUrl, solrCredentials);
	}
	
	/**
	 * @param pagePath
	 * @return Collection matching for the page path
	 * Determines the collection name from the saved configuration and the incoming page path
	 */
	public String getSolrCollection(String pagePath) {
		String[] splits = pagePath.split("/");
		String key = splits[1]+"/"+splits[2]+ "/" + splits[3];
		logger.info("Key for pagePath {} ==>  {} ",pagePath,key);
		return solrCollections.get(key);
	}
		
	/**
	 * @param collectionNames
	 * Converting collection array in the configuration to HashMap
	 * Key : root page path , Value : Collection Name
	 * Example : key : content/solrsearch/en , Value : solrsearch_en_collection
	 */
	private void setSolrCollectionsHashMap(String[] collectionNames) {
		this.solrCollections = new HashMap<>();
		for(String collectionName : collectionNames) {
			String values [] = collectionName.split("=");
			solrCollections.put(values[0], values[1]);		
		}
	}
	
	
	@Deactivate
	protected void deActivate() throws IOException {
		if (httpClient != null) {
			httpClient.close();
		}
	}

}
