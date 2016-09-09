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

import org.apache.solr.search.skg.model.KnowledgeGraphRequest;
import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.waitable.QueryWaitable;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.apache.solr.search.DocSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@RunWith(JMockit.class)
public class NodeScorerTest {

    QueryWaitable[] fgRunners = new QueryWaitable[2];
    QueryWaitable[] bgRunners = new QueryWaitable[2];
    QueryWaitable[] qRunners = new QueryWaitable[2];
    ResponseNode response = new ResponseNode();
    ResponseNode pResponse = new ResponseNode();
    RequestNode request = new RequestNode();
    NodeContext context = new NodeContext(new KnowledgeGraphRequest());
    List<QueryWaitable> runners = new LinkedList<>();
    @Mocked DocSet fgDomain;
    @Mocked DocSet bgDomain;
    @Mocked ScoreNormalizer normalizer;

    @Before
    public void init() {
        response.type = "keywords.v1";
        response.values = new ResponseValue[0];
        fgRunners[0] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 0);
        fgRunners[1] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 1);
        bgRunners[0] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.BG, 0);
        bgRunners[1] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.BG, 1);
        qRunners[0] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.Q, 0);
        qRunners[1] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.Q, 1);
        fgRunners[0].result = 0;
        fgRunners[1].result = 1;
        bgRunners[0].result = 0;
        bgRunners[1].result = 1;
        qRunners[0].result = 0;
        qRunners[1].result = 1;

        request.limit = 2;

        runners.addAll(Arrays.asList(fgRunners));
        runners.addAll(Arrays.asList(bgRunners));

        context.bgDomain = bgDomain;
        context.fgDomain = fgDomain;

        new NonStrictExpectations(){{
            ScoreNormalizer.normalize((NodeContext) any, (ResponseValue[]) any);
            fgDomain.size(); result = 100;
            bgDomain.size(); result = 1000;
        }};

    }

    @Test
    public void buildResponse() throws IOException
    {
        response.values = new ResponseValue[2];
        response.values[0] = new ResponseValue("test");
        response.values[1] = new ResponseValue("test");
        NodeScorer target = new NodeScorer();
        Deencapsulation.invoke(target, "addQueryResults", response, runners);

        Assert.assertEquals(0, response.values[0].foreground_popularity, 1e-4);
        Assert.assertEquals(0, response.values[0].background_popularity, 1e-4);
        Assert.assertEquals(1, response.values[1].foreground_popularity, 1e-4);
        Assert.assertEquals(1, response.values[1].background_popularity, 1e-4);

    }

    @Test
    public void processResponse()
    {
        pResponse.values = new ResponseValue[2];
        pResponse.values[0] = new ResponseValue("test1");
        pResponse.values[0].popularity= 50;
        pResponse.values[0].foreground_popularity = 50;
        pResponse.values[0].background_popularity = 500;
        pResponse.values[1] = new ResponseValue("test2");
        pResponse.values[1].popularity= 50;
        pResponse.values[1].foreground_popularity = 50;
        pResponse.values[1].background_popularity = 500;
        NodeScorer target = new NodeScorer();
        Deencapsulation.invoke(target, "processResponse", context, pResponse, request);

        Assert.assertEquals(0, pResponse.values[0].relatedness, 1e-4);
        Assert.assertEquals(0, pResponse.values[1].relatedness, 1e-4);
    }


}
