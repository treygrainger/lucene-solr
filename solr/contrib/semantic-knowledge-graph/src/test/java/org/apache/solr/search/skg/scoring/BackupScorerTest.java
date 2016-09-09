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

import org.apache.solr.search.skg.model.RequestNode;
import org.apache.solr.search.skg.model.ResponseNode;
import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.waitable.QueryWaitable;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.solr.search.DocSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.*;

@RunWith(JMockit.class)
public class BackupScorerTest
{

    QueryWaitable[] fgRunners = new QueryWaitable[4];
    QueryWaitable[] replacements = new QueryWaitable[2];

    ResponseNode response = new ResponseNode();
    RequestNode request = new RequestNode();
    List<QueryWaitable> runners = new LinkedList<>();
    List<QueryWaitable> replacementRunners = new LinkedList<>();
    @Mocked
    DocSet fgDomain;
    @Mocked
    DocSet bgDomain;
    @Mocked
    ScoreNormalizer normalizer;

    @Before
    public void init()
    {
        response.type = "keywords.v1";
        response.values = new ResponseValue[0];
        fgRunners[0] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 0);
        fgRunners[1] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 1);
        fgRunners[2] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 2);
        fgRunners[3] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 3);
        fgRunners[0].result = 0;
        fgRunners[1].result = 3;
        fgRunners[2].result = 1;
        fgRunners[3].result = 3;

        replacements[0] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 0);
        replacements[1] = new QueryWaitable(null, null, null, QueryWaitable.QueryType.FG, 2);

        replacements[0].result = 271;
        replacements[1].result = 271;

        request.limit = 2;

        runners.addAll(Arrays.asList(fgRunners));
        replacementRunners.addAll(Arrays.asList(replacements));
    }

    @Test
    public void getFallbackIndices0Min() throws IOException
    {
        BackupScorer target = new BackupScorer();
        Set<Integer> actual = target.getFallbackIndices(runners, 0.0);

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(new Integer(0), actual.iterator().next());
    }

    @Test
    public void getFallbackIndices1point5Min() throws IOException
    {
        BackupScorer target = new BackupScorer();
        Set<Integer> actual = target.getFallbackIndices(runners, 1.5);

        Iterator<Integer> resultIt = actual.iterator();
        Assert.assertEquals(2, actual.size());
        Assert.assertEquals(new Integer(0), resultIt.next());
        Assert.assertEquals(new Integer(2), resultIt.next());
    }


    @Test
    public void replaceRunners() throws IOException
    {
        BackupScorer target = new BackupScorer();
        List<QueryWaitable> original = new LinkedList<>(runners);
        target.replaceRunners(original, replacementRunners);

        Assert.assertEquals(4, original.size());
        Assert.assertEquals(271, original.stream().filter(q->q.index == 0).findFirst().get().result, 1e-4);
        Assert.assertEquals(3, original.stream().filter(q->q.index == 1).findFirst().get().result, 1e-4);
        Assert.assertEquals(271, original.stream().filter(q->q.index == 2).findFirst().get().result, 1e-4);
        Assert.assertEquals(3, original.stream().filter(q->q.index == 3).findFirst().get().result, 1e-4);
    }
}



