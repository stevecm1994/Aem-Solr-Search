package com.solrsearch.core.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;

@Component(service = SolrService.class)
public class SolrService {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Reference
	private RequestResponseFactory requestResponseFactory;
	
	@Reference
	private SlingRequestProcessor requestProcessor;
	
	@Reference
	private SolrServerConfiguration solrConfigurations;
	
	
	
	public ReplicationResult updateSolrIndex(ResourceResolver resolver, final String actionType, final ReplicationAction replicationAction) {
		
		ReplicationResult result = new ReplicationResult(true, 200, "OK");
		SolrClient solrClient = solrConfigurations.getAuthenticateSolrClient();
		if(null != resolver) {
			if(actionType.equals("DEACTIVATE") || actionType.equals("DELETE")) {
				final Set<String> pagePaths = new HashSet<>(Arrays.asList(replicationAction.getPaths()));
				for(String pagePath : pagePaths) {
					String collectionName = solrConfigurations.getSolrCollection(pagePath);
					logger.info("Collection Name for deactivateion of pgae {} = {}",pagePath,collectionName);
					if(!StringUtils.isEmpty(collectionName)) {
						delete(pagePath,solrClient,collectionName);
					}
				}
				
			}else if(actionType.equals("ACTIVATE")) {
				final String pagePath = replicationAction.getPath();
				String collectionName = solrConfigurations.getSolrCollection(pagePath);
				logger.info("Collection Name for activation of pgae {} = {}",pagePath,collectionName);
				if(!StringUtils.isEmpty(collectionName)) {
					result = index(pagePath,resolver,collectionName) ?  result : new ReplicationResult(false, 400, "Bad Request");
				}				
			}			
		}		
		return result;		
	}
	
	void delete(String pagePath,SolrClient solrClient,String collectionName) {		
			try {
				if(!pagePath.isEmpty() && !StringUtils.isEmpty(collectionName)) {
					solrClient.deleteById(collectionName, pagePath);
					solrClient.commit(collectionName);
				}				
			}catch (SolrServerException | IOException e) {
				logger.error("deleteDocumentFromSolr : Exception occurred {}", e);
			}		
	}
	
	boolean index(String pagePath, ResourceResolver resolver,String collectionName) {
		boolean isIndexed = false;
		logger.info("page path : {}", pagePath);
		final PageManager pageManager = resolver.adaptTo(PageManager.class);
		final Page actionPage = pageManager == null ? null : pageManager.getPage(pagePath);
		try {			
			if(null!=actionPage && !StringUtils.isEmpty(collectionName)) {								
				String requestPath = actionPage.getPath() + ".html";
				logger.debug("The request path is : {}", requestPath);
				HttpServletRequest request = requestResponseFactory.createRequest("GET", requestPath);
				WCMMode.DISABLED.toRequest(request);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				HttpServletResponse response = requestResponseFactory.createResponse(out);
				requestProcessor.processRequest(request, response, resolver);
				String htmlContent = out.toString();
				final SolrInputDocument doc = parseHtml(actionPage, htmlContent, resolver, collectionName,true);
				isIndexed = indexToSolr(actionPage, resolver, collectionName, doc);				
			}			
		}catch (Exception e) {
			logger.error("Error indexing {} to Solr {}", actionPage.getPath(), e);
		}
		
		return isIndexed;
	}
	
	public SolrInputDocument parseHtml(Page page, String htmlContent, ResourceResolver resourceResolver,
			String domainUrl, boolean isReplicateIndexing) {
		
		ValueMap pageProperties = page.getProperties();
		final SolrInputDocument solrDocument = new SolrInputDocument();
		Document document = Jsoup.parse(htmlContent);
		StringBuilder stringBuilder = new StringBuilder(document.head().text());
		String id = page.getPath();
		String title = page.getTitle();
		String description = StringUtils.isBlank(page.getDescription()) ? "Default Description" : page.getDescription();
		for (Element element : document.body().children()) {
			if (!isHtmlElementBlackListed(element)) {
				stringBuilder.append(StringUtils.SPACE).append(element.text());
			}
		}
		solrDocument.addField("id", id);
		solrDocument.addField("title", title);
		solrDocument.addField("description", description);
		solrDocument.addField("content", stringBuilder.toString());
		solrDocument.addField("publishedDate", getPagePublishedDate(pageProperties, isReplicateIndexing));
		return solrDocument;
	}
	
	private boolean isHtmlElementBlackListed(Element element) {
		List<String> htmlTagsBlackList = Arrays.asList("header", "footer");
		boolean isExists = false;
		for (String tag : htmlTagsBlackList) {
			return StringUtils.equals(element.nodeName(), tag);
		}
		return isExists;
	}
	
	private String getPagePublishedDate(ValueMap pageProperties, boolean isReplicateIndexing) {
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

		if (null != pageProperties && pageProperties.containsKey("cq:lastReplicated") && !isReplicateIndexing) {
			GregorianCalendar replicatedDate = (GregorianCalendar) pageProperties.get("cq:lastReplicated");
			return ZonedDateTime.parse(simpleDateFormat.format(replicatedDate.getTime()) + "Z").toInstant().toString();
		} else {
			return ZonedDateTime.parse(simpleDateFormat.format(new Date().getTime()) + "Z").toInstant().toString();
		}
	}
	
	protected boolean indexToSolr(Page actionPage, ResourceResolver resourceResolver, String collectionName,
			SolrInputDocument... docs) {
		boolean isIndexed = false;
		SolrClient solrClient = solrConfigurations.getAuthenticateSolrClient();
		for (SolrInputDocument doc : docs) {
			try {
				String id = null != doc.getFieldValue("id") ? doc.getFieldValue("id").toString() : StringUtils.EMPTY;
				logger.debug("Solr Document id - {} ", id);
				logger.debug("Adding doc {} to Solr collection: {} ", doc, collectionName);
				if (StringUtils.isNotBlank(collectionName) && StringUtils.isNotBlank(id) && null != solrClient) {
					solrClient.add(collectionName, doc);
					solrClient.commit(collectionName);
					isIndexed = true;
				}				
			}catch (SolrServerException | IOException e) {
				logger.error("Error indexing {} to Solr {}", actionPage.getPath(), e);
			}catch (Exception e) {
				logger.error("Error indexing {} to Solr {}", actionPage.getPath(), e);
			}
		}
		return isIndexed;
	}

}
