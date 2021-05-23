package com.solrsearch.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.MapSolrParams;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solrsearch.core.service.SolrResponseService;
import com.solrsearch.core.service.SolrServerConfiguration;


@Component(service=Servlet.class,
property={
        Constants.SERVICE_DESCRIPTION + "=Servlet to handle Solr requests from Front end Layer",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes="+ "/apps/solrsearchApi"
})
public class SolrSearchServlet extends SlingSafeMethodsServlet {
	

	private static final long serialVersionUID = -8945765762366876444L;

	private final Logger logger = LoggerFactory.getLogger(getClass());
		
	@Reference
	private SolrResponseService solrResponseService;
	
	@Reference
	private SolrServerConfiguration solrConfigurations;
	
	
	@Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
		
		resp.setContentType("application/json;charset=utf-8");		
		String searchType = req.getParameter("searchType");
		String searchInput = req.getParameter("q");
		String collection = req.getParameter("collectionName");
		logger.info(collection);
		MapSolrParams mapSolrParams = null;
		String queryJsonResponse = "";		
		if(searchType.equalsIgnoreCase("search") && !searchInput.isEmpty() && !collection.isEmpty()) {
			mapSolrParams =  setQueryParams("/search",searchInput);
			queryJsonResponse = solrResponseService.getSolrResponse(new QueryRequest(mapSolrParams), collection,solrConfigurations);
			resp.getWriter().write(queryJsonResponse == null ? "" : queryJsonResponse);
		}		
	}
	
	private MapSolrParams setQueryParams(String searchType,String searchInput) {
		Map<String, String> queryParamMap = new HashMap<>();
		queryParamMap.put("qt", searchType);
		queryParamMap.put("q",searchInput);
		return new MapSolrParams(queryParamMap);		
	}

}
