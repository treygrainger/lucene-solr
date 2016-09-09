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
import org.apache.solr.search.skg.utility.ParseUtility;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(JMockit.class)
public class RequestValidatorTest {

    @Mocked
    SolrQueryRequest solrRequest;
    @Mocked
    SolrIndexSearcher searcher;

    @Before
    public void init() throws IOException
    {
        new NonStrictExpectations() {{
            solrRequest.getSearcher(); returns(searcher);
        }};

        new MockUp<FieldChecker>()
        {
            @Mock void checkField(SolrQueryRequest req, String field, String facetField) {}
        };

        new NodeContext();
    }

    @Test(expected = SolrException.class)
    public void validate_NullQuery() throws IOException {
        KnowledgeGraphRequest request = new KnowledgeGraphRequest();
        RequestValidator target = new RequestValidator(solrRequest, request);
        target.validate();
    }

    @Test(expected = SolrException.class)
    public void validate_NullCompare() throws IOException {
        KnowledgeGraphRequest request = new KnowledgeGraphRequest();
        request.queries = new String[] { "test" };
        RequestValidator target = new RequestValidator(solrRequest, request);
        target.validate();
    }

    @Test
    public void validate_OneCompare() throws IOException {
        KnowledgeGraphRequest request = new KnowledgeGraphRequest(
                new RequestNode[1]);
        request.queries = new String[] { "test" };
        request.compare[0] = new RequestNode(null, "testType");
        RequestValidator target = new RequestValidator(solrRequest, request);
        target.validate();
    }

    @Test
    public void validate_TwoTrunkTreeCompare() throws IOException {
        new MockUp<ParseUtility>() {
            @Mock
            private Query parseQueryString(String qString, SolrQueryRequest req)
            {
                return new MatchAllDocsQuery();
            }
        };
        KnowledgeGraphRequest request = new KnowledgeGraphRequest(
                new RequestNode[2]);
        request.queries = new String[] { "test" };
        request.normalize=true;
        request.compare[0] = new RequestNode(null, "testType0");
        request.compare[1] = new RequestNode(null, "testType1");
        request.compare[0].compare = new RequestNode[2];
        request.compare[0].compare[0] = new RequestNode(null, "testType00");
        request.compare[0].compare[1] = new RequestNode(null, "testType01");
        ResponseNode[] expected = new ResponseNode[2];
        RequestValidator target = new RequestValidator(solrRequest, request);
        target.validate();
    }



    @Test(expected = SolrException.class)
    public void validate_TwoTrunkTreeCompareException() throws IOException {
        new MockUp<ParseUtility>() {
            @Mock
            private Query parseQueryString(String qString, SolrQueryRequest req)
            {
                return new MatchAllDocsQuery();
            }
        };
        KnowledgeGraphRequest request = new KnowledgeGraphRequest(
                new RequestNode[2]);
        request.queries = new String[] { "test" };
        request.normalize=true;
        request.compare[0] = new RequestNode(null, "testType0");
        request.compare[1] = new RequestNode(null, "testType1");
        request.compare[0].compare = new RequestNode[2];
        request.compare[0].compare[0] = new RequestNode();
        request.compare[0].compare[1] = new RequestNode(null, "testType01");
        RequestValidator target = new RequestValidator(solrRequest, request);
        target.validate();
    }
}
