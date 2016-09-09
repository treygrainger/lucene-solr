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

import org.apache.lucene.search.Query;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;

public class QueryWaitable extends Waitable {

    public enum QueryType {Q, FG, BG};
    public QueryType type;
    public int result = 0;
    public final int index;

    protected Query query;
    SolrIndexSearcher searcher;
    DocSet filter;
    Exception e;

    public QueryWaitable(SolrIndexSearcher searcher,
                         Query query, DocSet filter,
                         QueryType type,
                         int index) {
        this.query = query;
        this.searcher = searcher;
        this.filter = filter;
        this.type = type;
        this.index = index;
    }

    public Waitable call() {
        try {
            result = searcher.numDocs(query, filter);
        } catch (Exception e) {this.e = e; }
        return this;
    }

    public @Override boolean equals(Object other)
    {
        if(other instanceof QueryWaitable)
        {
            QueryWaitable instance = (QueryWaitable)other;
            return this.index == instance.index && this.type == instance.type;
        }
        return false;
    }
}
