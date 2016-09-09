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

import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.waitable.QueryWaitable;
import org.apache.solr.search.skg.utility.ParseUtility;
import org.apache.lucene.search.Query;
import org.apache.solr.search.DocSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class QueryRunnerFactory
{
    private NodeContext context;
    private ResponseNode response;
    HashSet<Integer> fallbackIndices;

    public QueryRunnerFactory(NodeContext context, ResponseNode response, HashSet<Integer> fallbackIndices)
    {
        this.context = context;
        this.response = response;
        this.fallbackIndices = fallbackIndices;
    }

    protected List<QueryWaitable> getQueryRunners(DocSet domain, String field, QueryWaitable.QueryType type) {
        List<QueryWaitable> runners = new LinkedList<>();
        if(fallbackIndices == null)
        {
            for (int k = 0; k < response.values.length; ++k)
            {
                runners.add(buildRunner(domain, type,
                        field, response.values[k].value.toLowerCase(Locale.ROOT).trim(), k));
            }
        }
        else
        {
            for (Integer k : fallbackIndices)
            {
                runners.add(buildRunner(domain, type,
                        field, response.values[k].value.toLowerCase(Locale.ROOT).trim(), k));
            }
        }
        return runners;
    }

    private QueryWaitable buildRunner(DocSet domain, QueryWaitable.QueryType type, String field, String value, int index)
    {
        Query query = ParseUtility.parseQueryString(
                field + ":\"" + value  + "\"",
                context.req);
        return new QueryWaitable(context.req.getSearcher(), query, domain, type, index);
    }
}
