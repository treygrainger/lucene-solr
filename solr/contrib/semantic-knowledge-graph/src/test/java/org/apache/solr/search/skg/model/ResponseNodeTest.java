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
package org.apache.solr.search.skg.model;

import org.apache.solr.search.skg.utility.SortUtility;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ResponseNodeTest {

    private ResponseNode node;
    private List<ResponseValue> values;

    @Before
    public void init()
    {
        node = new ResponseNode();
        ResponseValue v1 = new ResponseValue("v1", 0.75);
        v1.relatedness = 0.75;
        v1.background_popularity = 0.75;
        v1.foreground_popularity = 0.75;
        v1.popularity = 0.75;
        ResponseValue v2 = new ResponseValue("v2", 0.5);
        v2.relatedness = 0.5;
        v2.background_popularity = 0.5;
        v2.foreground_popularity = 0.5;
        v2.popularity = 0.5;
        ResponseValue v3 = new ResponseValue("v3", 0.25);
        v3.relatedness = 0.25;
        v3.background_popularity = 0.25;
        v3.foreground_popularity = 0.25;
        v3.popularity = 0.25;
        values = Arrays.asList(new ResponseValue[]{v3, v2, v1});
    }
    @Test
    public void testSortByFGPop()
    {
        SortUtility.sortResponseValues(values, SortType.foreground_popularity);
        assertEquals(0.75, values.get(0).foreground_popularity);
        assertEquals(0.5, values.get(1).foreground_popularity);
        assertEquals(0.25, values.get(2).foreground_popularity);
    }
    @Test
    public void testSortByRelatedness()
    {
        SortUtility.sortResponseValues(values, SortType.relatedness);
        assertEquals(0.75, values.get(0).relatedness);
        assertEquals(0.5, values.get(1).relatedness);
        assertEquals(0.25, values.get(2).relatedness);
    }
    @Test
    public void testSortByBGPop()
    {
        SortUtility.sortResponseValues(values, SortType.background_popularity);
        assertEquals(0.75, values.get(0).background_popularity);
        assertEquals(0.5, values.get(1).background_popularity);
        assertEquals(0.25, values.get(2).background_popularity);
    }

    @Test
    public void testSortByPopularity()
    {
        SortUtility.sortResponseValues(values, SortType.popularity);
        assertEquals(0.75, values.get(0).popularity);
        assertEquals(0.5, values.get(1).popularity);
        assertEquals(0.25, values.get(2).popularity);
    }
}
