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
package org.apache.solr.search.skg;

import org.apache.solr.search.skg.model.KnowledgeGraphRequest;
import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.normalization.NodeNormalizer;
import org.apache.solr.search.skg.scoring.NodeScorer;
import org.apache.solr.search.skg.generation.NodeGenerator;
import org.apache.solr.search.skg.utility.ParseUtility;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(JMockit.class)
public class RequestTreeRecurserTest {

    @Mocked
    SolrQueryRequest solrRequest;
    @Mocked
    SolrIndexSearcher searcher;

    @Before
    public void init() throws IOException
    {
        new MockUp<NodeScorer>()
        {
            @Mock
            public ResponseNode [] transform(NodeContext context, RequestNode [] requests, ResponseNode [] responses)
            {
                responses = new ResponseNode[requests.length];
                for(int i = 0; i < requests.length; ++i) {
                    ResponseNode response = new ResponseNode(requests[i].type);
                    response.values = new ResponseValue[2];
                    response.values[0] = new ResponseValue("0");
                    response.values[1] = new ResponseValue("1");
                    responses[i] = response;
                }
                return responses;
            }
        };

        new MockUp<NodeGenerator>()
        {
            @Mock
            public ResponseNode [] transform(NodeContext context, RequestNode [] requests, ResponseNode [] responses)
            {
                return null;
            }
        };

        new MockUp<NodeNormalizer>()
        {
            @Mock
            public ResponseNode [] transform(NodeContext context, RequestNode [] requests, ResponseNode [] responses)
            {
                return null;
            }
        };



        new NonStrictExpectations() {{
            solrRequest.getSearcher(); returns(searcher);
        }};

        new NonStrictExpectations() {{
            try {
                searcher.getDocSet((Query) any, (DocSet) any);
                returns(null);
            } catch (Exception e) {}
        }};



        new NodeContext();
    }

    @Test
    public void recurseComparables_Null() throws IOException {

        KnowledgeGraphRequest request = new KnowledgeGraphRequest();
        NodeContext context = new NodeContext(request, solrRequest, null);
        RequestTreeRecurser target = new RequestTreeRecurser(context);
        ResponseNode[] actual = target.score();

        Assert.assertArrayEquals(null, actual);
    }


    @Test
    public void recurseComparables_One() throws IOException {

        KnowledgeGraphRequest request = new KnowledgeGraphRequest(
                new RequestNode[1]);
        request.compare[0] = new RequestNode(null, "testType");
        ResponseNode[] expected = new ResponseNode[1];
        expected[0] = new ResponseNode("testType");
        setTwoValues(expected[0]);
        NodeContext context = new NodeContext(request, solrRequest, null);
        RequestTreeRecurser target = new RequestTreeRecurser(context);

        ResponseNode[] actual = target.score();

        checkComparableTree(expected, actual);
    }

    @Test
    public void recurseComparables_TwoTrunkTree() throws IOException {
        new MockUp<ParseUtility>() {
            @Mock
            private Query parseQueryString(String qString, SolrQueryRequest req)
            {
                return new MatchAllDocsQuery();
            }
        };

        KnowledgeGraphRequest request = new KnowledgeGraphRequest(
                new RequestNode[2]);
        request.normalize=true;
        request.compare[0] = new RequestNode(null, "testType0");
        request.compare[1] = new RequestNode(null, "testType1");
        request.compare[0].compare = new RequestNode[2];
        request.compare[0].compare[0] = new RequestNode(null, "testType00");
        request.compare[0].compare[1] = new RequestNode(null, "testType01");
        ResponseNode[] expected = new ResponseNode[2];
        expected[0] = new ResponseNode("testType0");
        setTwoValues(expected[0]);
        setTwoCompares(expected[0].values[0]);
        setTwoCompares(expected[0].values[1]);
        setTwoValues(expected[0].values[0].compare[0]);
        setTwoValues(expected[0].values[0].compare[1]);
        setTwoValues(expected[0].values[1].compare[0]);
        setTwoValues(expected[0].values[1].compare[1]);
        expected[1] = new ResponseNode("testType1");
        setTwoValues(expected[1]);
        NodeContext context = new NodeContext(request, solrRequest, null);
        RequestTreeRecurser target = new RequestTreeRecurser(context);

        ResponseNode[] actual = target.score();

        checkComparableTree(expected, actual);
    }

    private void setTwoCompares(ResponseValue value) {
        value.compare = new ResponseNode[2];
        value.compare[0] = new ResponseNode("testType00");
        value.compare[1] = new ResponseNode("testType01");
    }

    private void setTwoValues(ResponseNode expected) {
        expected.values = new ResponseValue[2];
        expected.values[0] = new ResponseValue("0");
        expected.values[1] = new ResponseValue("1");
    }

    private void checkComparableTree(ResponseNode[] expected, ResponseNode[] actual)
    {
        if(actual!= null) {
            for(int i = 0; i < actual.length; ++i) {
                Assert.assertTrue(expected[i].type.compareTo(actual[i].type) == 0);
                for(int k = 0; k < actual[i].values.length; ++k) {
                    Assert.assertTrue(expected[i].values[k].value.compareTo(actual[i].values[k].value) == 0);
                    checkComparableTree(expected[i].values[k].compare, actual[i].values[k].compare);
                }
            }
        }
    }

}
