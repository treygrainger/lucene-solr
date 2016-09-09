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
package org.apache.solr.search.skg.utility;

import org.apache.solr.search.skg.model.ResponseValue;
import org.apache.solr.search.skg.model.SortType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortUtility
{
    public static void sortResponseValues(List<ResponseValue> responseValues, SortType sortedBy)
    {

        if(sortedBy == null)
            sortedBy = SortType.relatedness;
        switch(sortedBy) {
            case foreground_popularity:
                sortByFG(responseValues);
                break;
            case popularity:
                sortByPopularity(responseValues);
                break;
            case background_popularity:
                sortByBG(responseValues);
                break;
            case relatedness:
                sortByRelatedness(responseValues);
                break;
        }
    }

    private static void sortByRelatedness(List<ResponseValue> values)
    {
        Collections.sort(values, Comparator.comparing((ResponseValue val) -> -1 * val.relatedness).thenComparing((ResponseValue val) -> val.value.toLowerCase(Locale.ROOT)));
    }

    private static void sortByFG(List<ResponseValue> values)
    {
        Collections.sort(values, Comparator.comparing((ResponseValue val) -> -1 * val.foreground_popularity).thenComparing((ResponseValue val) -> val.value.toLowerCase(Locale.ROOT)));
    }

    private static void sortByBG(List<ResponseValue> values)
    {
        Collections.sort(values, Comparator.comparing((ResponseValue val) -> -1 * val.background_popularity).thenComparing((ResponseValue val) -> val.value.toLowerCase(Locale.ROOT)));
    }

    private static void sortByPopularity(List<ResponseValue> values)
    {
        Collections.sort(values, Comparator.comparing((ResponseValue val) -> -1 * val.popularity).thenComparing((ResponseValue val) -> val.value.toLowerCase(Locale.ROOT)));
    }
}
