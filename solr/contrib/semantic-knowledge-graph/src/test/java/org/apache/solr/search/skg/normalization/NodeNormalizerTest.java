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
package org.apache.solr.search.skg.normalization;

import org.apache.solr.search.skg.generation.FacetFieldAdapter;
import org.apache.solr.search.skg.model.KnowledgeGraphRequest;
import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.NodeContext;
import org.apache.solr.search.skg.waitable.AggregationWaitable;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RunWith(JMockit.class)
public class NodeNormalizerTest {

    ResponseNode response;
    RequestNode request;
    @Mocked
    AggregationWaitable runner;
    @Cascading final NodeContext context = new NodeContext(new KnowledgeGraphRequest());
    @Mocked SolrQueryRequest unused;
    @Mocked SolrIndexSearcher unusedSearcher;

    @Before
    public void init() {
        runner.buckets= new LinkedList<>();

        request = new RequestNode(null, "testType");
        request.values = new String[] {"passedInValue1", "passedInValue2"};

        response = new ResponseNode("testType");
        context.req = unused;

        new MockUp<SolrIndexSearcher>()
        {
            @Mock
            public DocListAndSet getDocListAndSet(Query q, DocSet d, Sort s, int one, int two)
            {
                return null;
            }
        };

        new NonStrictExpectations() {{
            context.req.getSearcher(); returns(unusedSearcher);
        }};

        new MockUp<FacetFieldAdapter>() {
            @Mock public void $init(NodeContext context, String field) {}
            @Mock public String getStringValue(SimpleOrderedMap<Object> bucket) {
                return (String)bucket.get("val");
            }
            @Mock public SimpleOrderedMap<String> getMapValue(SimpleOrderedMap<Object> bucket) {
                SimpleOrderedMap<String> map = new SimpleOrderedMap<>();
                map.add("name", (String)bucket.get("val"));
                map.add("id", (String)bucket.get("id"));
                return map;
            }
        };

    }

    @Test
    public void buildRunners_noValues()
    {
        RequestNode [] requests = new RequestNode[1];
        FacetFieldAdapter [] adapters = new FacetFieldAdapter[1];
        adapters[0] = new FacetFieldAdapter("testField");
        Deencapsulation.setField(adapters[0], "facetFieldExtension", ".cs");
        requests[0] = new RequestNode(null, "testField");
        NodeNormalizer target = new NodeNormalizer();

        Map<String, List<AggregationWaitable>> runners  = Deencapsulation.invoke(target, "buildRunners", context, requests);

        Assert.assertEquals(0, runners.get("testField").size());
    }

    @Test
    public void buildRunners_noExtension()
    {
        RequestNode [] requests = new RequestNode[1];
        FacetFieldAdapter [] adapters = new FacetFieldAdapter[1];
        adapters[0] = new FacetFieldAdapter("testField");
        requests[0] = new RequestNode(null, "testField");
        requests[0].values = new String[] {"testValue"};
        NodeNormalizer target = new NodeNormalizer();

        Map<String, List<AggregationWaitable>> runners  = Deencapsulation.invoke(target, "buildRunners", context, requests);

        Assert.assertEquals(0, runners.get("testField").size());
    }

    @Test
    public void buildRunners_Normalize(@Mocked FacetFieldAdapter adapter) throws IOException
    {
        RequestNode [] requests = new RequestNode[1];
        Deencapsulation.setField(adapter, "facetFieldExtension", ".cs");
        requests[0] = new RequestNode(null, "testField");
        requests[0].values = new String[] {"testValue"};
        NodeNormalizer target = new NodeNormalizer();

        new Expectations() {{
            new FacetFieldAdapter(context, "testField");
            adapter.hasExtension(); returns(true);
            new AggregationWaitable(context, adapter, "null:\"testvalue\"", null, 0, 100);
        }};

        Map<String, List<AggregationWaitable>> runners = Deencapsulation.invoke(target, "buildRunners", context, requests);
    }

    @Test
    public void populateNorms()
    {
        NodeNormalizer target = new NodeNormalizer();
        FacetFieldAdapter adapter = new FacetFieldAdapter("testField");
        AggregationWaitable runner = new AggregationWaitable(context, adapter, "testField", 0, 1);
        runner.buckets = new LinkedList<>();
        runner.adapter = adapter;
        SimpleOrderedMap<Object> bucket1 = new SimpleOrderedMap<>();
        bucket1.add("val", "testValue1");
        bucket1.add("id", "1");
        SimpleOrderedMap<Object> bucket2 = new SimpleOrderedMap<>();
        bucket2.add("val", "testValue2");
        bucket2.add("id", "2");
        SimpleOrderedMap<Object> bucket3 = new SimpleOrderedMap<>();
        bucket3.add("val", "value3");
        bucket3.add("id", "3");
        runner.buckets.add(bucket1);
        runner.buckets.add(bucket2);
        runner.buckets.add(bucket3);
        String requestValue1 = "testValue1";
        String requestValue2 = "testValue2";
        LinkedList<String> normalizedStrings = new LinkedList<>();
        LinkedList<SimpleOrderedMap<String>> normalizedMaps = new LinkedList<>();
        String [] expectedStrings = new String [] {"testValue1", "testValue2"};
        LinkedList<SimpleOrderedMap<String>> expectedMaps = new LinkedList<>();
        SimpleOrderedMap<String> map1 = new SimpleOrderedMap<>();
        map1.add("name", "testValue1");
        map1.add("id", "1");
        SimpleOrderedMap<String> map2 = new SimpleOrderedMap<>();
        map2.add("name", "testValue2");
        map2.add("id", "2");
        expectedMaps.add(map1);
        expectedMaps.add(map2);

        Deencapsulation.invoke(target, "populateNorms", runner, requestValue1, normalizedStrings, normalizedMaps);
        Deencapsulation.invoke(target, "populateNorms", runner, requestValue2, normalizedStrings, normalizedMaps);

        Assert.assertEquals(2, normalizedStrings.size());
        Assert.assertEquals(2, normalizedMaps.size());
        for(int i = 0; i < expectedStrings.length ; ++i) {
            Assert.assertEquals(expectedMaps.get(i),normalizedMaps.get(i));
            Assert.assertEquals(expectedStrings[i],normalizedStrings.get(i));
        }
    }

    @Test
    public void populateNorms_Id()
    {
        NodeNormalizer target = new NodeNormalizer();
        FacetFieldAdapter adapter = new FacetFieldAdapter("testField");
        AggregationWaitable runner = new AggregationWaitable(context, adapter, "testField", 0, 1);
        runner.buckets = new LinkedList<>();
        runner.adapter = adapter;
        SimpleOrderedMap<Object> bucket1 = new SimpleOrderedMap<>();
        bucket1.add("val", "testValue1");
        bucket1.add("id", "1");
        SimpleOrderedMap<Object> bucket2 = new SimpleOrderedMap<>();
        bucket2.add("val", "testValue2");
        bucket2.add("id", "2");
        SimpleOrderedMap<Object> bucket3 = new SimpleOrderedMap<>();
        bucket3.add("val", "value3");
        bucket3.add("id", "3");
        runner.buckets.add(bucket1);
        runner.buckets.add(bucket2);
        runner.buckets.add(bucket3);
        String requestValue1 = "1";
        String requestValue2 = "2";
        LinkedList<String> normalizedStrings = new LinkedList<>();
        LinkedList<SimpleOrderedMap<String>> normalizedMaps = new LinkedList<>();
        String [] expectedStrings = new String [] {"testValue1", "testValue2"};
        LinkedList<SimpleOrderedMap<String>> expectedMaps = new LinkedList<>();
        SimpleOrderedMap<String> map1 = new SimpleOrderedMap<>();
        map1.add("name", "testValue1");
        map1.add("id", "1");
        SimpleOrderedMap<String> map2 = new SimpleOrderedMap<>();
        map2.add("name", "testValue2");
        map2.add("id", "2");
        expectedMaps.add(map1);
        expectedMaps.add(map2);


        Deencapsulation.invoke(target, "populateNorms", runner, requestValue1, normalizedStrings, normalizedMaps);
        Deencapsulation.invoke(target, "populateNorms", runner, requestValue2, normalizedStrings, normalizedMaps);

        Assert.assertEquals(2, normalizedStrings.size());
        Assert.assertEquals(2, normalizedMaps.size());
        for(int i = 0; i < expectedStrings.length ; ++i) {
            Assert.assertEquals(expectedMaps.get(i),normalizedMaps.get(i));
            Assert.assertEquals(expectedStrings[i],normalizedStrings.get(i));
        }
    }

    @Test
    public void normalizeRequests() {
        RequestNode [] requests = new RequestNode[1];
        requests[0] = new RequestNode(null, "testField");
        requests[0].values = new String[] {"testValue1"};
        NodeNormalizer target = new NodeNormalizer();
        FacetFieldAdapter adapter = new FacetFieldAdapter("testField");
        Deencapsulation.setField(adapter, "facetFieldExtension", ".cs");
        Map<String, List<AggregationWaitable>> runners = new TreeMap<>();
        List<AggregationWaitable> runnerList = new LinkedList<>();
        runnerList.add(new AggregationWaitable(context, adapter, "testField", 0, 1));
        runnerList.get(0).buckets = new LinkedList<>();
        runnerList.get(0).adapter = adapter;
        SimpleOrderedMap<Object> bucket1 = new SimpleOrderedMap<>();
        bucket1.add("val", "testValue1");
        SimpleOrderedMap<Object> bucket2 = new SimpleOrderedMap<>();
        bucket2.add("val", "testValue2");
        SimpleOrderedMap<Object> bucket3 = new SimpleOrderedMap<>();
        bucket3.add("val", "value3");
        runnerList.get(0).buckets.add(bucket1);
        runnerList.get(0).buckets.add(bucket2);
        runnerList.get(0).buckets.add(bucket3);
        runners.put("testField", runnerList);
        String [] expectedStrings = new String [] {"testValue1"};
        LinkedList<SimpleOrderedMap<String>> expectedMaps = new LinkedList<>();
        SimpleOrderedMap<String> map1 = new SimpleOrderedMap<>();
        map1.add("name", "testValue1");
        map1.add("id", null);
        SimpleOrderedMap<String> map2 = new SimpleOrderedMap<>();
        map2.add("name", "testValue2");
        map2.add("id", null);
        expectedMaps.add(map1);
        expectedMaps.add(map2);

        target.normalizeRequests(requests, runners);

        Assert.assertEquals(1, requests[0].values.length);
        Assert.assertEquals(1, requests[0].normalizedValues.size());
        for(int i = 0; i < expectedStrings.length ; ++i) {
            Assert.assertEquals(expectedMaps.get(i),requests[0].normalizedValues.get(i));
            Assert.assertEquals(expectedStrings[i],requests[0].values[i]);
        }
    }
}
