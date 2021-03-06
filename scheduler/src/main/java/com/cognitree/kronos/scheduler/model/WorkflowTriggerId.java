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

package com.cognitree.kronos.scheduler.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

@JsonSerialize(as = WorkflowTriggerId.class)
@JsonDeserialize(as = WorkflowTriggerId.class)
public class WorkflowTriggerId {

    private String namespace;
    private String name;
    private String workflow;

    public static WorkflowTriggerId build(String namespace, String name, String workflowName) {
        final WorkflowTriggerId workflowTriggerId = new WorkflowTriggerId();
        workflowTriggerId.setNamespace(namespace);
        workflowTriggerId.setName(name);
        workflowTriggerId.setWorkflow(workflowName);
        return workflowTriggerId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowTriggerId)) return false;
        WorkflowTriggerId that = (WorkflowTriggerId) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(name, that.name) &&
                Objects.equals(workflow, that.workflow);
    }

    @Override
    public int hashCode() {

        return Objects.hash(namespace, name, workflow);
    }

    @Override
    public String toString() {
        return "WorkflowTriggerId{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", workflow='" + workflow + '\'' +
                '}';
    }
}
