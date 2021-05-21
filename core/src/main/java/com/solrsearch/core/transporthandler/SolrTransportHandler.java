package com.solrsearch.core.transporthandler;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.ReplicationTransaction;
import com.day.cq.replication.TransportContext;
import com.day.cq.replication.TransportHandler;
import com.solrsearch.core.service.SolrService;


public class SolrTransportHandler implements TransportHandler{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTransportHandler.class);
	
	private static final String SOLR_PROTOCOL = "solr://";
	
	@Reference
	SolrService solrService;

	@Override
	public boolean canHandle(AgentConfig config) {
		boolean handleFlag = false;
		if(config != null) {
			final String uri = config.getTransportURI();
			LOGGER.info("Transport URI : {}", uri);
			handleFlag = uri.startsWith(SOLR_PROTOCOL);
		}
		return handleFlag;
	}

	@Override
	public ReplicationResult deliver(TransportContext ctx, ReplicationTransaction tx) throws ReplicationException {
		LOGGER.info("Start of deliver method");
		ResourceResolver resolver = null;
		ReplicationResult result = null;
		try {
			final ReplicationAction replicationAction = tx.getAction();
			final String actionType = replicationAction.getType().toString();
			result = solrService.updateSolrIndex(resolver, actionType, replicationAction);		
		}finally {
			if (resolver != null) {
                resolver.close();
            }
		}
		return result;
	}

}
