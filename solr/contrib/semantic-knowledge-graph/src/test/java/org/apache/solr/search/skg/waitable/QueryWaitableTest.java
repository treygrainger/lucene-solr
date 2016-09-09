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
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(JMockit.class)
public class QueryWaitableTest
{

    NodeContext context;
    @Mocked SolrIndexSearcher searcher;
    @Mocked DocSet docSet;
    Query query;

    @Before
    public void init() throws IOException
    {
        context = new NodeContext();
        query = new TermQuery(new Term("testField1", "testQuery1"));

        new Expectations() {{
            searcher.numDocs(query, docSet); returns(1);
        }};
    }

    @Test
    public void call(){
        QueryWaitable target = new QueryWaitable(searcher, query, docSet, QueryWaitable.QueryType.FG, 1);

        target.call();

        Assert.assertEquals(1, target.result);
        Assert.assertEquals(QueryWaitable.QueryType.FG, target.type);
        Assert.assertEquals(1, target.index);
    }
}
