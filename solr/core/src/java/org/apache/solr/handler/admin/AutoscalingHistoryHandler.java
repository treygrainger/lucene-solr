package org.apache.solr.handler.admin;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.cloud.autoscaling.SystemLogListener;
import org.apache.solr.cloud.autoscaling.TriggerEvent;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.AutoScalingParams;
import org.apache.solr.common.params.CollectionAdminParams;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.PermissionNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler makes it easier to retrieve a history of autoscaling events from the .system
 * collection.
 */
public class AutoscalingHistoryHandler extends RequestHandlerBase implements PermissionNameProvider {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String SYSTEM_COLLECTION_PARAM = "systemCollection";

  public static final String ACTION_PARAM = "action";
  public static final String MESSAGE_PARAM = "message";
  public static final String TRIGGER_PARAM = AutoScalingParams.TRIGGER;
  public static final String TYPE_PARAM = "eventType";
  public static final String NODE_PARAM = "node";
  public static final String COLLECTION_PARAM = CollectionAdminParams.COLLECTION;
  public static final String STAGE_PARAM = AutoScalingParams.STAGE;
  public static final String BEFORE_ACTION_PARAM = AutoScalingParams.BEFORE_ACTION;
  public static final String AFTER_ACTION_PARAM = AutoScalingParams.AFTER_ACTION;

  private static final String EVENTS_FQ = "{!term f=" + CommonParams.TYPE + "}" + SystemLogListener.DOC_TYPE;

  private static final String ACTION_FQ_FORMAT = "{!term f=" + SystemLogListener.ACTION_FIELD + "}%s";
  private static final String MESSAGE_FQ_FORMAT = "{!lucene}" + SystemLogListener.MESSAGE_FIELD + ":%s";
  private static final String TRIGGER_FQ_FORMAT = "{!term f=" + SystemLogListener.EVENT_SOURCE_FIELD + "}%s";
  private static final String STAGE_FQ_FORMAT = "{!term f=" + SystemLogListener.STAGE_FIELD + "}%s";
  private static final String COLLECTION_FQ_FORMAT = "{!term f=" + SystemLogListener.COLLECTIONS_FIELD + "}%s";
  private static final String TYPE_FQ_FORMAT = "{!term f=" + SystemLogListener.EVENT_TYPE_FIELD + "}%s";
  private static final String NODE_FQ_FORMAT = "{!term f=event.property." + TriggerEvent.NODE_NAME + "_s}%s";
  private static final String BEFORE_ACTION_FQ_FORMAT = "{!term f=" + SystemLogListener.BEFORE_ACTIONS_FIELD + "}%s";
  private static final String AFTER_ACTION_FQ_FORMAT = "{!term f=" + SystemLogListener.AFTER_ACTIONS_FIELD + "}%s";

  private static final Map<String, String> formats = new HashMap<String, String>() {{
    put(ACTION_PARAM, ACTION_FQ_FORMAT);
    put(MESSAGE_PARAM, MESSAGE_FQ_FORMAT);
    put(TRIGGER_PARAM, TRIGGER_FQ_FORMAT);
    put(TYPE_PARAM, TYPE_FQ_FORMAT);
    put(STAGE_PARAM, STAGE_FQ_FORMAT);
    put(NODE_PARAM, NODE_FQ_FORMAT);
    put(COLLECTION_PARAM, COLLECTION_FQ_FORMAT);
    put(BEFORE_ACTION_PARAM, BEFORE_ACTION_FQ_FORMAT);
    put(AFTER_ACTION_PARAM, AFTER_ACTION_FQ_FORMAT);
  }};

  private final CoreContainer coreContainer;


  public AutoscalingHistoryHandler(CoreContainer coreContainer) {
    this.coreContainer = coreContainer;
  }

  @Override
  public Name getPermissionName(AuthorizationContext request) {
    return Name.AUTOSCALING_HISTORY_READ_PERM;
  }

  @Override
  public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
    ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());
    String collection = params.get(SYSTEM_COLLECTION_PARAM, CollectionAdminParams.SYSTEM_COLL);
    params.remove(SYSTEM_COLLECTION_PARAM);
    params.remove(CommonParams.QT);
    // check that we have the main query, if not then use *:*
    if (params.get(CommonParams.Q) == null) {
      params.add(CommonParams.Q, "*:*");
    }
    // sort by doc id, which are time-based, unless specified otherwise
    if (params.get(CommonParams.SORT) == null) {
      params.add(CommonParams.SORT, "id asc");
    }
    // filter query to pick only autoscaling events
    params.remove(CommonParams.FQ, EVENTS_FQ);
    params.add(CommonParams.FQ, EVENTS_FQ);
    // add filters translated from simplified parameters
    for (Map.Entry<String, String> e : formats.entrySet()) {
      String[] values = params.remove(e.getKey());
      if (values != null) {
        for (String value : values) {
          params.add(CommonParams.FQ, String.format(e.getValue(), value));
        }
      }
    }
    try (CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder()
        .withZkHost(coreContainer.getZkController().getZkServerAddress())
        .withHttpClient(coreContainer.getUpdateShardHandler().getHttpClient())
        .build()) {
      QueryResponse qr = cloudSolrClient.query(collection, params);
      rsp.getValues().add("response", qr.getResults());
    } catch (Exception e) {
      if ((e instanceof SolrException) && e.getMessage().contains("Collection not found")) {
        // relatively benign
        LOG.info("Collection " + collection + " does not exist.");
      } else {
        throw e;
      }
    }
  }

  @Override
  public String getDescription() {
    return "A handler to return autoscaling event history";
  }

  @Override
  public Category getCategory() {
    return Category.ADMIN;
  }

}
