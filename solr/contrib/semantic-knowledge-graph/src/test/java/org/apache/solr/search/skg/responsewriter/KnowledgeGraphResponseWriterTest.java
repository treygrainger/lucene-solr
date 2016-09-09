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
package org.apache.solr.search.skg.responsewriter;

import org.apache.solr.search.skg.model.KnowledgeGraphResponse;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.model.ResponseValue;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

@RunWith(JMockit.class)
public class KnowledgeGraphResponseWriterTest
{

    HashMap<String, String[]> dummy = new HashMap<>();
    SolrQueryRequest request;
    SolrQueryResponse response;

    @Before
    public void init()
    {

        new MockUp<SolrQueryResponse>()
        {
            @Mock public NamedList<Object> getResponseHeader()
            {
                NamedList<Object> headers = new NamedList<>();
                headers.add("status", 400);
                return headers;
            }
        };
        request = new LocalSolrQueryRequest(null, dummy);
        response = new SolrQueryResponse();
        response.setHttpHeader("status", "400");
    }

    @Test
    public void write() throws IOException
    {

        ResponseNode[] responses = new ResponseNode[1];
        responses[0] = new ResponseNode("testType");
        responses[0].values = new ResponseValue[1];
        responses[0].values[0] = new ResponseValue("testValue", 1.0);
        KnowledgeGraphResponse relResponse = new KnowledgeGraphResponse();
        relResponse.data = responses;
        response.add("relatednessResponse", relResponse);
        KnowledgeGraphResponseWriter target = new KnowledgeGraphResponseWriter();
        Writer writer = new StringWriter();

        target.write(writer, request, response);

        Assert.assertEquals("{\"data\":[{\"type\":\"testType\",\"values\":[{\"name\":\"testValue\",\"relatedness\":0.0,\"popularity\":0.0,\"foreground_popularity\":1.0,\"background_popularity\":0.0}]}]}"
                , writer.toString());
    }

    @Test
    public void write_Exception() throws IOException
    {

        response.setException(new SolrException(SolrException.ErrorCode.BAD_REQUEST, "This is a test of the emergency alert system"));
        KnowledgeGraphResponseWriter target = new KnowledgeGraphResponseWriter();
        Writer writer = new StringWriter();

        target.write(writer, request, response);

        Assert.assertEquals("{\"error\":{\"msg\":\"This is a test of the emergency alert system\",\"code\":400}}"
                , writer.toString());
    }
}
