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

package monasca.persister.pipeline.event;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.codahale.metrics.Counter;

import io.dropwizard.setup.Environment;
import monasca.common.model.metric.MetricEnvelope;
import monasca.persister.configuration.PipelineConfig;
import monasca.persister.repository.MetricRepo;

public class MetricHandler extends FlushableHandler<MetricEnvelope[]> {

  private final int ordinal;

  private final MetricRepo metricRepo;

  private final Counter metricCounter;

  @Inject
  public MetricHandler(MetricRepo metricRepo, @Assisted PipelineConfig configuration,
                       Environment environment, @Assisted("ordinal") int ordinal,
                       @Assisted("batchSize") int batchSize) {

    super(configuration, environment, ordinal, batchSize, MetricHandler.class.getName());
    this.metricRepo = metricRepo;

    final String handlerName = String.format("%s[%d]", MetricHandler.class.getName(), ordinal);
    this.metricCounter =
        environment.metrics().counter(handlerName + "." + "metrics-added-to-batch-counter");

    this.ordinal = ordinal;

  }

  @Override
  public int process(MetricEnvelope[] metricEnvelopes) throws Exception {

    for (final MetricEnvelope metricEnvelope : metricEnvelopes) {
      processEnvelope(metricEnvelope);
    }

    return metricEnvelopes.length;
  }

  private void processEnvelope(MetricEnvelope metricEnvelope) {

    this.metricRepo.addToBatch(metricEnvelope);

    metricCounter.inc();

  }

  @Override
  public void flushRepository() {
    metricRepo.flush();
  }

}