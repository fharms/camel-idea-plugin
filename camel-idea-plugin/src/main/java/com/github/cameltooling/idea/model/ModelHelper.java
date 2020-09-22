/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.idea.model;

import org.apache.camel.tooling.model.ComponentModel.ComponentOptionModel;
import org.apache.camel.tooling.model.ComponentModel.EndpointOptionModel;
import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.cameltooling.idea.util.StringUtils.getSafeDefaultValue;
import static com.github.cameltooling.idea.util.StringUtils.getSafeValue;

public final class ModelHelper {

    private ModelHelper() {
        // utility class
    }

    public static ComponentModel generateComponentModel(String json, boolean includeOptions) {
        try {
            JsonObject obj = (JsonObject) Jsoner.deserialize(json);
            Map<String, Object> rows = obj.getMap("component");
            ComponentModel component = new ComponentModel();
            component.setScheme(getSafeValue("scheme", rows, ""));
            component.setSyntax(getSafeValue("syntax", rows, ""));
            component.setAlternativeSyntax(getSafeValue("alternativeSyntax", rows, ""));
            component.setAlternativeSchemes(getSafeValue("alternativeSchemes", rows, ""));
            component.setTitle(getSafeValue("title", rows, ""));
            component.setDescription(getSafeValue("description", rows, ""));
            component.setLabel(getSafeValue("label", rows, ""));
            component.setDeprecated(getSafeValue("deprecated", rows, false));
            component.setConsumerOnly(getSafeValue("consumerOnly", rows, ""));
            component.setProducerOnly(getSafeValue("producerOnly", rows, ""));
            component.setJavaType(getSafeValue("javaType", rows, ""));
            component.setGroupId(getSafeValue("groupId", rows, ""));
            component.setArtifactId(getSafeValue("artifactId", rows, ""));
            component.setVersion(getSafeValue("version", rows, ""));

            if (includeOptions) {
                Map<String, Map<String, Object>> modelComponentProperties = obj.getMap("componentProperties");
                for (Map.Entry<String, Map<String, Object>> modelComponentProperty : modelComponentProperties.entrySet()) {
                    ComponentOptionModel option = new ComponentOptionModel();
                    Map<String, Object>options = modelComponentProperty.getValue();
                    option.setName(modelComponentProperty.getKey());
                    option.setKind(getSafeValue("kind", options, ""));
                    option.setGroup(getSafeValue("group", options, ""));
                    option.setRequired(getSafeValue("required", options, false));
                    option.setType(getSafeValue("type", options, ""));
                    option.setJavaType(getSafeValue("javaType", options, ""));
                    option.setEnums((List<String>)options.getOrDefault("enum", Collections.emptyList()));
                    option.setDeprecated(getSafeValue("deprecated", options, false));
                    option.setSecret(getSafeValue("secret", options, false));
                    option.setDefaultValue(getSafeDefaultValue("defaultValue", options));
                    option.setDescription(getSafeValue("description", options, ""));
                    component.addComponentOption(option);
                }

                Map<String, Map<String, Object>> modelProperties = obj.getMap("properties");
                for (Map.Entry<String, Map<String, Object>> modelProperty : modelProperties.entrySet()) {
                    EndpointOptionModel option = new EndpointOptionModel();
                    Map<String, Object> options = modelProperty.getValue();
                    option.setName(modelProperty.getKey());
                    option.setKind(getSafeValue("kind", options, ""));
                    option.setGroup(getSafeValue("group", options, ""));
                    option.setLabel(getSafeValue("label", options, ""));
                    option.setRequired(getSafeValue("required", options, false));
                    option.setType(getSafeValue("type", options, ""));
                    option.setJavaType(getSafeValue("javaType", options, ""));
                    option.setEnums((List<String>)options.getOrDefault("enum", Collections.emptyList()));
                    option.setPrefix(getSafeValue("prefix", options, ""));
                    option.setMultiValue(getSafeValue("multiValue", options, false));
                    option.setDeprecated(getSafeValue("deprecated", options, false));
                    option.setSecret(getSafeValue("secret", options, false));
                    option.setDefaultValue(getSafeDefaultValue("defaultValue", options));
                    option.setDescription(getSafeValue("description", options, ""));
                    component.addEndpointOption(option);
                }
            }
            return component;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse component json structure");
        }
    }
}
