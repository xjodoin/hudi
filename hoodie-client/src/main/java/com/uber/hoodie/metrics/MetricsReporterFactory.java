/*
 * Copyright (c) 2016 Uber Technologies, Inc. (hoodie-dev-group@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.hoodie.metrics;

import com.codahale.metrics.MetricRegistry;
import com.uber.hoodie.config.HoodieWriteConfig;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Factory class for creating MetricsReporter.
 */
public class MetricsReporterFactory {

  private static Logger logger = LogManager.getLogger(MetricsReporterFactory.class);

  public static MetricsReporter createReporter(HoodieWriteConfig config,
      MetricRegistry registry) {
    MetricsReporterType type = config.getMetricsReporterType();
    MetricsReporter reporter = null;
    switch (type) {
      case GRAPHITE:
        reporter = new MetricsGraphiteReporter(config, registry);
        break;
      case INMEMORY:
        reporter = new InMemoryMetricsReporter();
        break;
      default:
        logger.error("Reporter type[" + type + "] is not supported.");
        break;
    }
    return reporter;
  }
}
