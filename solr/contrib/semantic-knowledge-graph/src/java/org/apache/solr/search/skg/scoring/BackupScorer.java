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

import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.waitable.QueryWaitable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class BackupScorer
{
    public void runFallback(NodeContext context,
                             ResponseNode response,
                             List<QueryWaitable> qRunners,
                             String fallbackField) {
        List<QueryWaitable> fgQueryWaitables = qRunners.stream().filter(q -> q.type == QueryWaitable.QueryType.FG).collect(Collectors.toList());
        HashSet<Integer> fallbackIndices = getFallbackIndices(fgQueryWaitables, context.request.min_popularity);
        QueryRunnerFactory factory = new QueryRunnerFactory(context, response, fallbackIndices);
        List<QueryWaitable> fallbackRunners = new LinkedList<>();
        fallbackRunners.addAll(factory.getQueryRunners(context.fgDomain, fallbackField, QueryWaitable.QueryType.FG));
        fallbackRunners.addAll(factory.getQueryRunners(context.bgDomain, fallbackField, QueryWaitable.QueryType.BG));
        if(context.request.return_popularity)
        {
            fallbackRunners.addAll(factory.getQueryRunners(context.queryDomain, fallbackField, QueryWaitable.QueryType.Q));
        }
        NodeScorer.parallelQuery(fallbackRunners);
        replaceRunners(qRunners, fallbackRunners);
    }

    protected HashSet<Integer> getFallbackIndices(List<QueryWaitable> values, double minCount)
    {
        HashSet<Integer> indices = new HashSet<>();
        for(QueryWaitable runner : values)
        {
            if(runner.result < minCount || runner.result == 0) {
                indices.add(runner.index);
            }
        }
        return indices;
    }

    protected void replaceRunners(List<QueryWaitable> target, List<QueryWaitable> replacers) {
        target.removeAll(replacers);
        target.addAll(replacers);
    }
}
