package com.ayushsingh.doc_helper.commons.config.logging;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import jakarta.annotation.PostConstruct;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventThreadNameJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

@Configuration
public class LoggingConfig {

    @PostConstruct
    public void setupLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);

        LoggingEventCompositeJsonEncoder consoleEncoder = new LoggingEventCompositeJsonEncoder();
        consoleEncoder.setContext(context);

        LoggingEventJsonProviders consoleProviders = new LoggingEventJsonProviders();
        consoleProviders.addTimestamp(new LoggingEventFormattedTimestampJsonProvider());
        consoleProviders.addLogLevel(new LogLevelJsonProvider());
        consoleProviders.addLoggerName(new LoggerNameJsonProvider());
        consoleProviders.addThreadName(new LoggingEventThreadNameJsonProvider());
        consoleProviders.addMessage(new MessageJsonProvider());
        consoleProviders.addMdc(new MdcJsonProvider()); // important!
        consoleProviders.addStackTrace(new StackTraceJsonProvider());

        consoleEncoder.setProviders(consoleProviders);
        consoleEncoder.start();

        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();

        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile("logs/app.log");

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern("logs/app-%d{yyyy-MM-dd}.log");
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);

        LoggingEventCompositeJsonEncoder fileEncoder = new LoggingEventCompositeJsonEncoder();
        fileEncoder.setContext(context);

        LoggingEventJsonProviders fileProviders = new LoggingEventJsonProviders();
        fileProviders.addTimestamp(new LoggingEventFormattedTimestampJsonProvider());
        fileProviders.addLogLevel(new LogLevelJsonProvider());
        fileProviders.addLoggerName(new LoggerNameJsonProvider());
        fileProviders.addThreadName(new LoggingEventThreadNameJsonProvider());
        fileProviders.addMessage(new MessageJsonProvider());
        fileProviders.addMdc(new MdcJsonProvider());
        fileProviders.addStackTrace(new StackTraceJsonProvider());

        fileEncoder.setProviders(fileProviders);
        fileEncoder.start();

        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();

        ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(fileAppender);
    }
}
