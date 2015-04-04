/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monasca.persister.repository.influxdb;

import monasca.common.model.metric.Metric;
import monasca.common.model.metric.MetricEnvelope;
import monasca.persister.repository.MetricRepo;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import io.dropwizard.setup.Environment;

public abstract class InfluxMetricRepo implements MetricRepo {

  private static final Logger logger = LoggerFactory.getLogger(InfluxMetricRepo.class);

  protected final MeasurementBuffer measurementBuffer = new MeasurementBuffer();

  public final com.codahale.metrics.Timer flushTimer;
  public final Meter measurementMeter;

  protected abstract void write() throws Exception;

  public InfluxMetricRepo(final Environment env) {

    this.flushTimer = env.metrics().timer(this.getClass().getName() + ".flush-timer");
    this.measurementMeter = env.metrics().meter(this.getClass().getName() + ".measurement-meter");
  }

  @Override
  public void addToBatch(MetricEnvelope metricEnvelope) {

    Metric metric = metricEnvelope.metric;
    Map<String, Object> meta = metricEnvelope.meta;

    Definition
        definition =
        new Definition(metric.getName(), (String) meta.get("tenantId"),
                       (String) meta.get("region"));

    Dimensions dimensions = new Dimensions(metric.getDimensions());

    Measurement
        measurement =
        new Measurement(metric.getTimestamp(), metric.getValue(), metric.getValueMeta());

    this.measurementBuffer.put(definition, dimensions, measurement);
    this.measurementMeter.mark();

  }


  @Override
  public void flush() {

    try {
      final long startTime = System.currentTimeMillis();
      final Timer.Context context = flushTimer.time();

      write();

      final long endTime = System.currentTimeMillis();
      context.stop();

      logger.debug("Writing measurements, definitions, and dimensions to InfluxDB took {} seconds",
                   (endTime - startTime) / 1000);

    } catch (Exception e) {
      logger.error("Failed to write measurements to InfluxDB", e);
    }

    clearBuffers();
  }


  private void clearBuffers() {

    this.measurementBuffer.clear();

  }

}
