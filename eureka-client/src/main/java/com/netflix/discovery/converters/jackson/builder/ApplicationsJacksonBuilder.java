/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.discovery.converters.jackson.builder;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.discovery.converters.KeyFormatter;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support custom formatting of {@link Applications#appsHashCode} and {@link Applications#versionDelta}. The
 * serialized property name is generated by {@link KeyFormatter} according to provided configuration. We can
 * depend here on fixed prefix to distinguish between property values, and map them correctly.
 */
public class ApplicationsJacksonBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsJacksonBuilder.class);

    private List<Application> applications;
    private long version;
    private String appsHashCode;

    @JsonProperty("application")
    public void withApplication(List<Application> applications) {
        this.applications = applications;
    }

    @JsonAnySetter
    public void with(String fieldName, Object value) {
        if (fieldName == null || value == null) {
            return;
        }
        if (fieldName.startsWith("version")) {
            try {
                version = value instanceof Number n ? n.longValue() : Long.parseLong((String) value);
            } catch (Exception e) {
                version = -1;
                logger.warn("Cannot parse version number {}; setting it to default == -1", value);
            }
        } else if (fieldName.startsWith("apps")) {
            if (value instanceof String string) {
                appsHashCode = string;
            } else {
                logger.warn("appsHashCode field is not a string, but {}", value.getClass());
            }
        }
    }

    public Applications build() {
        return new Applications(appsHashCode, version, applications);
    }
}
