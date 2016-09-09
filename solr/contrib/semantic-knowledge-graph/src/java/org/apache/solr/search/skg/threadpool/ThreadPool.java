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
package org.apache.solr.search.skg.threadpool;

import org.apache.solr.search.skg.waitable.Waitable;
import org.apache.solr.common.SolrException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPool
{
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public static ThreadPool getInstance()
    {
        return ThreadPoolHolder.pool;
    }

    public static synchronized Future execute(Waitable w) {
        return getInstance().pool.submit(w);
    }

    public static List<Waitable> demultiplex(List<Future<Waitable>> futures)
    {
        List<Waitable> result = new LinkedList<Waitable>();
        try
        {
            for (Future<Waitable> future : futures)
            {
                Waitable w = future.get();
                if(w.e != null)
                {
                    throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
                            "Error executing thread. ", w.e);
                }
                result.add(w);
            }
        }
        catch (InterruptedException e)
        {
            throw new SolrException(
                    SolrException.ErrorCode.SERVER_ERROR, "Parallel Operation interrupted", e);
        }
        catch (ExecutionException e)
        {
            throw new SolrException(
                    SolrException.ErrorCode.SERVER_ERROR, "Execution exception. ", e);
        }
        return result;
    }

    public static List<Future<Waitable>> multiplex(Waitable [] array)
    {
        LinkedList<Future<Waitable>> futures = new LinkedList<>();
        for(int i = 0; i < array.length; ++i) {
            if(array[i] != null) {
                futures.addLast(execute(array[i]));
            }
        }
        return futures;
    }
}