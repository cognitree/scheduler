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

package com.cognitree.kronos.queue.producer;

import com.cognitree.kronos.queue.RAMQueueFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RAMProducer extends Producer {
    private static final Logger logger = LoggerFactory.getLogger(RAMProducer.class);

    public RAMProducer(String topic, ObjectNode config) {
        super(topic, config);
    }

    @Override
    public void broadcast(String record) {
        send(record);
    }

    @Override
    public void send(String record) {
        sendInOrder(record, null);
    }

    @Override
    public void sendInOrder(String record, String orderingKey) {
        logger.trace("Received request to send message {} on topic {} with orderingKey {}",
                record, topic, orderingKey);
        RAMQueueFactory.getQueue(topic).add(record);
    }

    @Override
    public void close() {
        // do nothing
    }
}
