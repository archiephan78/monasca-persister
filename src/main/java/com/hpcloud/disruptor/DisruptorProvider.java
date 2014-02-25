package com.hpcloud.disruptor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hpcloud.configuration.MonPersisterConfiguration;
import com.hpcloud.event.StringEvent;
import com.hpcloud.event.StringEventFactory;
import com.hpcloud.event.StringEventHandler;
import com.hpcloud.event.StringEventHandlerFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DisruptorProvider implements Provider<Disruptor> {

    private static Logger logger = LoggerFactory.getLogger(DisruptorProvider.class);

    private MonPersisterConfiguration configuration;
    private StringEventHandlerFactory stringEventHandlerFactory;
    private Disruptor instance;

    @Inject
    public DisruptorProvider(MonPersisterConfiguration configuration,
                             StringEventHandlerFactory stringEventHandlerFactory) {
        this.configuration = configuration;
        this.stringEventHandlerFactory = stringEventHandlerFactory;
    }

    public synchronized Disruptor<StringEvent> get() {

        logger.debug("Requesting instance of disruptor");

        if (instance == null) {

            logger.debug("Instance of disruptor is null. Creating disruptor...");

            Executor executor = Executors.newCachedThreadPool();
            StringEventFactory stringEventFactory = new StringEventFactory();

            int bufferSize = configuration.getDisruptorConfiguration().getBufferSize();
            logger.debug("Buffer size for instance of disruptor [" + bufferSize + "]");

            Disruptor<StringEvent> disruptor = new Disruptor(stringEventFactory, bufferSize, executor);

            int batchSize = configuration.getVerticaOutputProcessorConfiguration().getBatchSize();
            logger.debug("Batch size for each output processor [" + batchSize + "]");

            int numOutputProcessors = configuration.getDisruptorConfiguration().getNumProcessors();
            logger.debug("Number of output processors [" + numOutputProcessors + "]");

            EventHandler[] stringEventHandlers = new StringEventHandler[numOutputProcessors];

            for (int i = 0; i < numOutputProcessors; ++i) {

                stringEventHandlers[i] = stringEventHandlerFactory.create(i, numOutputProcessors, batchSize);

            }

            disruptor.handleEventsWith(stringEventHandlers);

            disruptor.start();
            logger.debug("Instance of disruptor successfully started");

            logger.debug("Instance of disruptor fully created");

            instance = disruptor;
        }

        logger.debug("Returning instance of disruptor");

        return instance;
    }
}
