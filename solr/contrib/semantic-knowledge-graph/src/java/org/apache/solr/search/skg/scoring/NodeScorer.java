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
package org.apache.solr.search.skg.scoring;

import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.RecursionOp;
import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.waitable.QueryWaitable;
import org.apache.solr.search.skg.waitable.Waitable;
import org.apache.solr.search.skg.threadpool.ThreadPool;
import org.apache.solr.search.skg.utility.ResponseUtility;
import org.apache.solr.common.SolrException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

public class NodeScorer implements RecursionOp {

    public ResponseNode[] transform(NodeContext context, RequestNode[] requests, ResponseNode[] responses) {
        for(int i = 0; i < responses.length; ++i) {
            QueryRunnerFactory factory = new QueryRunnerFactory(context, responses[i], null);

            List<QueryWaitable> qRunners = new LinkedList<>();
            qRunners.addAll(factory.getQueryRunners(context.fgDomain,
                    responses[i].type, QueryWaitable.QueryType.FG));
            qRunners.addAll(factory.getQueryRunners(context.bgDomain,
                    responses[i].type, QueryWaitable.QueryType.BG));
            if(context.request.return_popularity)
                qRunners.addAll(factory.getQueryRunners(context.queryDomain, responses[i].type, QueryWaitable.QueryType.Q));

            parallelQuery(qRunners);

            String fallbackField = context.parameterSet.invariants.get(responses[i].type + ".fallback");
            if(fallbackField != null)
            {
                new BackupScorer().runFallback(context, responses[i], qRunners, fallbackField);
            }
            addQueryResults(responses[i], qRunners);
            processResponse(context, responses[i], requests[i]);
        }
        return responses;
    }

    protected static void parallelQuery(List<QueryWaitable> qRunners)
    {
        List<Future<Waitable>> futures = new LinkedList<>();
        futures.addAll(ThreadPool.multiplex(qRunners.toArray(new Waitable[0])));
        ThreadPool.demultiplex(futures);
    }

    private void addQueryResults(ResponseNode response, List<QueryWaitable> qRunners) {
        for(QueryWaitable queryWaitable : qRunners )
        {
            if (queryWaitable != null)
            {
                setResultValue(response.values[queryWaitable.index], queryWaitable.result, queryWaitable.type);
            }
        }
    }

    private void setResultValue(ResponseValue toSet, double result, QueryWaitable.QueryType type)
    {
        switch(type)
        {
            case FG:
                toSet.foreground_popularity = result;
                break;
            case BG:
                toSet.background_popularity = result;
                break;
            case Q:
                toSet.popularity = result;
                break;
            default:
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unknown QueryType.");
        }
    }

    private void processResponse(NodeContext context, ResponseNode response, RequestNode request)
    {
        if(response.values != null && response.values.length > 0) {
            int fgTotal = context.fgDomain.size();
            int bgTotal = context.bgDomain.size();
            relatednessScore(response, fgTotal, bgTotal);
            ScoreNormalizer.normalize(context, response.values);
            response.values = ResponseUtility.filterAndSortValues(response.values, request, context.request);
        }
    }

    private void relatednessScore(ResponseNode response, int fgTotal, int bgTotal) {
        for (int k = 0; k < response.values.length; ++k) {
            response.values[k].relatedness = BinomialStrategy.score(fgTotal,
                    bgTotal, response.values[k].foreground_popularity, response.values[k].background_popularity);
        }
    }
}
