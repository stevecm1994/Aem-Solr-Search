package com.solrsearch.core.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SolrResponseService.class)
public class SolrResponseService {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	
	public  String getSolrResponse(QueryRequest queryRequest, String collection , SolrServerConfiguration solrConfigurations) {
		NamedList<Object> namedList = null;
		String jsonResponse = "";
		SolrClient solrClient =  null;
		try {
			solrClient = solrConfigurations.getSolrClient();
			NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
			rawJsonResponseParser.setWriterType("json");
			queryRequest.setResponseParser(rawJsonResponseParser);
			namedList = solrClient.request(queryRequest, collection);
			jsonResponse = (String) namedList.get("response");
		} catch (SolrServerException e) {
			logger.info("SolrServerException in SearchServlet", e);
		} catch (Exception e) {
			logger.info("Exception in SearchServlet", e);
		} finally {
			try {
				solrClient.close();
			} catch (Exception e) {
				logger.info("Exception in SearchServlet", e);
			}
		}

		return jsonResponse;
	}

}
