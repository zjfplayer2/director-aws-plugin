// (c) Copyright 2017 Cloudera, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.director.aws;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import java.util.Map;

/**
 * Provides support for custom tag names for well-known Director tags.
 */
public class CustomTagMappings {

  private final ImmutableMap<String, String> customTagNames;

  /**
   * Creates a new custom tag mappings object from the given configuration. The keys of the
   * configuration are well-known tag names, and the values are the custom tag names.
   *
   * @param config config holding custom tags
   * @throws IllegalArgumentException if any configuration value is not a string or is empty
   */
  public CustomTagMappings(Config config) {
    ImmutableMap.Builder<String, String> b = ImmutableMap.builder();

    if (config != null) {
      for (Map.Entry<String,ConfigValue> e : config.entrySet()) {
        String key = e.getKey();
        ConfigValue value = e.getValue();
        switch (value.valueType()) {
          case STRING:
            String customTagName = (String) value.unwrapped();
            if (customTagName.isEmpty()) {
              throw new IllegalArgumentException("Tag mapping " + key + " is empty");
            }
            b.put(key, customTagName);
            break;
          default:
            throw new IllegalArgumentException("Tag mapping " + key + " is not a string: " + value);
        }
      }
    }

    customTagNames = b.build();
  }

  /**
   * Returns the custom tag name for the specified tag key if present, otherwise the key itself.
   *
   * @param tagKey tag key
   * @return the custom tag name for the specified tag key if present, otherwise the key itself
   */
  public String getCustomTagName(String tagKey) {
    return customTagNames.containsKey(tagKey) ? customTagNames.get(tagKey) : tagKey;
  }

  /**
   * Returns the name of the tag used to record the virtual instance ID as generated by Cloudera
   * Director.
   *
   * @return the name of the tag used to record the virtual instance ID as generated by Cloudera
   * Director
   */
  public String getClouderaDirectorIdTagName() {
    return getCustomTagName(Tags.ResourceTags.CLOUDERA_DIRECTOR_ID.getTagKey());
  }
}
