/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.ims;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;


/**
 * An interface for querying IMS, and return {@code Future<Boolean>}
 */
public interface ImsQuery {

    /**
     * Interface for performing IMS status/configuration query through ExecutorService
     *
     * @param executors {@code ExecutorService} which allows to submit {@code ImsQuery} when
     * required
     * @return result of query in format of {@code Future<Boolean>}
     */
    Future<Boolean> query(ExecutorService executors) throws RejectedExecutionException;

}
