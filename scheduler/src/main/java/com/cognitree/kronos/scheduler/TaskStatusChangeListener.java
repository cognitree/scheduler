/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognitree.kronos.scheduler;

import com.cognitree.kronos.model.Task;
import com.cognitree.kronos.model.Task.Status;

/**
 * An interface implemented by services interested in task status change. To receive notification on task status change
 * register the listener with {@link TaskSchedulerService#registerListener(TaskStatusChangeListener)}
 */
public interface TaskStatusChangeListener {

    /**
     * called on task status change.
     * <p>
     * attempts to modify the task instance, result in an UnsupportedOperationException
     *
     * @param task an unmodifiable instance of task.
     * @param from
     * @param to
     */
    void statusChanged(Task task, Status from, Status to);
}