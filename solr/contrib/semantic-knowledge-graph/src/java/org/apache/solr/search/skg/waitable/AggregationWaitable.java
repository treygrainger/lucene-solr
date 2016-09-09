/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.search.skg.waitable;

import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.generation.FacetFieldAdapter;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.facet.FacetModule;

import java.io.IOException;
import java.util.*;

public class AggregationWaitable extends Waitable{

    private int limit;
    private String field;
    public FacetFieldAdapter adapter;
    public List<SimpleOrderedMap<Object>> buckets;
    public final int index;
    public String facetQuery;
    final NodeContext context;
    public static final String FIELD_FACET_NAME = "fieldFacet";
    public static final String QUERY_FACET_NAME = "queryFacet";

    public AggregationWaitable(NodeContext context, FacetFieldAdapter adapter, String facetQuery, String field, int index, int limit) {
        this(context, adapter, field, index, limit);
        this.facetQuery = facetQuery;
    }

    public AggregationWaitable(NodeContext context, FacetFieldAdapter adapter, String field, int index, int limit) {
        this.context = context;
        this.field = field;
        this.limit = limit;
        this.adapter = adapter;
        this.index = index;
    }

    public Waitable call()
    {
        try {
            aggregate();
        } catch (Exception e) { this.e = e; }
        return this;
    }

    public void aggregate() throws IOException {
        FacetModule mod = new FacetModule();
        SolrQueryResponse resp = new SolrQueryResponse();
        ResponseBuilder rb = getResponseBuilder(resp);
        mod.prepare(rb);
        mod.process(rb);
        parseResponse(resp);
    }

    private ResponseBuilder getResponseBuilder(SolrQueryResponse resp) throws IOException {
        SolrQueryRequest req =  new LocalSolrQueryRequest(context.req.getCore(), buildFacetParams());
        req.setJSON(buildFacetJson());
        ResponseBuilder rb = new ResponseBuilder(req, resp, null);
        rb.setResults(context.queryDomainList);
        return rb;
    }

    // this is kind of awkward, but necessary since JSON faceting is protected in Solr,
    // (we can only send object requests).
    private void parseResponse(SolrQueryResponse resp) {
        SimpleOrderedMap<Object> facet = (SimpleOrderedMap<Object>)
                ((SimpleOrderedMap<Object>)((SimpleOrderedMap<Object>) resp.getValues()).get("facets")).get(QUERY_FACET_NAME);
        if(facet != null) {
            SimpleOrderedMap<Object> innerFacet = (SimpleOrderedMap<Object>)facet.get(FIELD_FACET_NAME);
            if(innerFacet != null)
            {
                buckets = (List<SimpleOrderedMap<Object>>)innerFacet.get("buckets");
            } else {
                buckets = new LinkedList<>();
            }
        } else {
            buckets = new LinkedList<>();
        }
    }

    // see above
    public Map<String, Object> buildFacetJson()
    {
        int limit = 5*Math.max(this.limit, 25);
        LinkedHashMap<String, Object> wrapper = new LinkedHashMap<>();
        LinkedHashMap<String, Object> queryFacetName = new LinkedHashMap<>();
        LinkedHashMap<String, Object> queryFacetWrapper= new LinkedHashMap<>();
        LinkedHashMap<String, Object> queryFacet= new LinkedHashMap<>();
        LinkedHashMap<String, Object> fieldFacetName = new LinkedHashMap<>();
        LinkedHashMap<String, Object> fieldFacetWrapper= new LinkedHashMap<>();
        LinkedHashMap<String, Object> fieldFacet= new LinkedHashMap<>();
        fieldFacet.put("type", "field");
        fieldFacet.put("field", field);
        fieldFacet.put("limit", limit);
        fieldFacetWrapper.put("field", fieldFacet);
        fieldFacetName.put(FIELD_FACET_NAME, fieldFacetWrapper);
        queryFacet.put("facet", fieldFacetName);
        queryFacet.put("q", facetQuery);
        queryFacetWrapper.put("query", queryFacet);
        queryFacetName.put(QUERY_FACET_NAME, queryFacetWrapper);
        wrapper.put("facet", queryFacetName);
        return wrapper;
    }

    // see above
    public SolrParams buildFacetParams()
    {
        LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
        paramMap.put("facet.version", "1");
        paramMap.put("wt", "json");
        paramMap.put(FacetParams.FACET, "true");
        return new MapSolrParams(paramMap);
    }
}
