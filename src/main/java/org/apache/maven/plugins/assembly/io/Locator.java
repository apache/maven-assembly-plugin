/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.assembly.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Locator.
 *
 */
final class Locator {

    private List<LocatorStrategy> strategies;
    private final MessageHolder messageHolder;

    /**
     * @param strategies List of strategies
     * @param messageHolder {@link MessageHolder}
     */
    Locator(List<LocatorStrategy> strategies, MessageHolder messageHolder) {
        this.messageHolder = messageHolder;
        this.strategies = new ArrayList<LocatorStrategy>(strategies);
    }

    /**
     * Create instance.
     */
    Locator() {
        this.messageHolder = new DefaultMessageHolder();
        this.strategies = new ArrayList<LocatorStrategy>();
    }

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder getMessageHolder() {
        return messageHolder;
    }

    /**
     * @param strategy The strategy to be added.
     */
    void addStrategy(LocatorStrategy strategy) {
        this.strategies.add(strategy);
    }

    /**
     * @param strategy the strategy to remove.
     */
    void removeStrategy(LocatorStrategy strategy) {
        this.strategies.remove(strategy);
    }

    /**
     * @param strategies the strategies to be set.
     */
    void setStrategies(List<LocatorStrategy> strategies) {
        this.strategies.clear();
        this.strategies.addAll(strategies);
    }

    /**
     * @return list of strategies.
     */
    List<LocatorStrategy> getStrategies() {
        return strategies;
    }

    /**
     * @param locationSpecification location spec
     * @return {@link Location}
     */
    Location resolve(String locationSpecification) {
        Location location = null;

        for (Iterator<LocatorStrategy> it = strategies.iterator(); location == null && it.hasNext(); ) {
            LocatorStrategy strategy = (LocatorStrategy) it.next();

            location = strategy.resolve(locationSpecification, messageHolder);
        }

        return location;
    }
}
