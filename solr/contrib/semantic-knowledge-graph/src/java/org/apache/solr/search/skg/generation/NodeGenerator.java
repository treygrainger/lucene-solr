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
package org.apache.solr.search.skg.generation;

import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.RecursionOp;
import org.apache.solr.search.skg.waitable.AggregationWaitable;
import org.apache.solr.search.skg.waitable.Waitable;
import org.apache.solr.search.skg.threadpool.ThreadPool;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.util.SimpleOrderedMap;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

public class NodeGenerator implements RecursionOp {

    public ResponseNode [] transform(NodeContext context, RequestNode [] requests, ResponseNode [] responses) throws IOException {
        ResponseNode [] resps = new ResponseNode[requests.length];
        AggregationWaitable[] runners = buildWaitables(context, requests);
        List<Future<Waitable>> futures = ThreadPool.multiplex(runners);
        ThreadPool.demultiplex(futures);
        for(int i = 0; i < resps.length; ++i) {
            resps[i] = new ResponseNode(requests[i].type);
            mergeResponseValues(requests[i], resps[i], runners[i]);
        }
        return resps;
    }

    private void mergeResponseValues(RequestNode request, ResponseNode resp, AggregationWaitable runner) {
        int genLength = runner == null ? 0 : runner.buckets == null ? 0 :runner.buckets.size();
        int requestValsLength = request.values == null ? 0 : request.values.length;
        resp.values = new ResponseValue[requestValsLength + genLength];
        int k = addPassedInValues(request, resp);
        if(runner != null) {
            addGeneratedValues(resp, runner, k);
        }
    }

    private int addPassedInValues(RequestNode request, ResponseNode resp) {
        int k = 0;
        if(request.values != null) {
            for (; k < request.values.length; ++k) {
                resp.values[k] = new ResponseValue(request.values[k]);
                resp.values[k].normalizedValue = request.normalizedValues == null ? null : request.normalizedValues.get(k);
            }
        }
        return k;
    }

    private void addGeneratedValues(ResponseNode resp, AggregationWaitable runner, int k) {
        for (SimpleOrderedMap<Object> bucket: runner.buckets) {
            ResponseValue respValue = new ResponseValue(runner.adapter.getStringValue(bucket));
            respValue.normalizedValue = runner.adapter.getMapValue(bucket);
            resp.values[k++] = respValue;
        }
    }

    private AggregationWaitable[] buildWaitables(NodeContext context,
                                               RequestNode [] requests) throws IOException {
        AggregationWaitable[] runners = new AggregationWaitable[requests.length];
        for(int i = 0; i < requests.length; ++i) {
            if(requests[i].discover_values) {
                // populate required docListAndSet once and only if necessary
                if(context.queryDomainList == null) {
                   context.queryDomainList =
                           context.req.getSearcher().getDocListAndSet(new MatchAllDocsQuery(),
                                   context.queryDomain, Sort.INDEXORDER, 0, 0);
                }
                FacetFieldAdapter adapter = new FacetFieldAdapter(context, requests[i].type);
                runners[i] = new AggregationWaitable(context,
                        adapter,
                        adapter.field,
                        0,
                        requests[i].limit);
            }
        }
        return runners;
    }
}
