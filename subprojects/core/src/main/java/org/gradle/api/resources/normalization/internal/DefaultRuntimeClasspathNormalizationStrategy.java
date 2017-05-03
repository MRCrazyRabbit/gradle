/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.resources.normalization.internal;

import com.google.common.collect.Sets;
import org.gradle.api.GradleException;
import org.gradle.api.internal.changedetection.state.IgnoreResourceFilter;
import org.gradle.api.internal.changedetection.state.MetadataFilter;

import java.util.Set;

public class DefaultRuntimeClasspathNormalizationStrategy implements RuntimeClasspathNormalizationStrategyInternal {
    private final Set<String> ignores = Sets.newHashSet();
    private MetadataFilter metadataFilter;

    @Override
    public synchronized void ignore(String pattern) {
        if (metadataFilter != null) {
            throw new GradleException("Cannot configure runtimeClasspath normalization after execution started.");
        }
        ignores.add(pattern);
    }

    private synchronized void finalizeConfiguration() {
        if (metadataFilter == null) {
            metadataFilter = new IgnoreResourceFilter(ignores);
        }
    }

    @Override
    public MetadataFilter getMetadataFilter() {
        finalizeConfiguration();
        return metadataFilter;
    }
}
