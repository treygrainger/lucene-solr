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

import org.apache.solr.search.skg.model.*;
import org.apache.solr.search.skg.model.Error;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;

import java.io.IOException;
import java.io.Writer;

public class KnowledgeGraphResponseWriter implements QueryResponseWriter{

    public void init(NamedList args)
    {

    }
    public String getContentType(SolrQueryRequest request, SolrQueryResponse response)
    {
        return "application/json";
    }

    public void write(Writer writer, SolrQueryRequest request, SolrQueryResponse response) throws IOException
    {
        Gson gson = new GsonBuilder().registerTypeAdapter(ResponseValue.class, new ResponseValueSerializer()).create();
        Exception e = response.getException();
        int status = (int)response.getResponseHeader().get("status");
        KnowledgeGraphResponse model = (KnowledgeGraphResponse)response.getValues().get("relatednessResponse");
        if(e != null) {
            if(model == null) {
                model = new KnowledgeGraphResponse();
            }
            model.error = new Error(e.getMessage(), status);
        }
        writer.write(gson.toJson(model, KnowledgeGraphResponse.class));
    }

}
