/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.co.spudspoft.vertx.rest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class TimerContainerResponseFilter implements ContainerResponseFilter {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(TimerContainerResponseFilter.class);
  
  private final MeterRegistry meterRegistry;

  /**
   * Constructor.
   * @param meterRegistry The MeterRegistry to use for recording timings.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "MeterRegistry may be changed in handlers")
  public TimerContainerResponseFilter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
    try {
      Sample sample = TimerContainerRequestFilter.getSample(requestContext);
      if (sample != null) {
        
        Timed timed = TimerContainerRequestFilter.getAnnotation(requestContext);
        
        String name = getMeterName(requestContext, timed);

        Timer.Builder timerBuilder = Timer.builder(name)
                  .tags(buildTags(requestContext, responseContext, timed.extraTags()))
                  .description(timed.description().isEmpty() ? null : timed.description())
                  .publishPercentileHistogram(timed.histogram())
                  .publishPercentiles(timed.percentiles().length > 0 ? timed.percentiles() : null)
                ;
        sample.stop(timerBuilder.register(meterRegistry));
      }
    } catch (Throwable ex) {
      logger.error("Failed to record timer: ", ex);
    }
  }
  
  private List<Tag> buildTags(ContainerRequestContext requestContext, ContainerResponseContext responseContext, String[] extraTags) {
    List<Tag> tags = new ArrayList<>();
    tags.add(Tag.of("response-code", Integer.toString(responseContext.getStatus())));
    for (int i = 0; i < extraTags.length; i += 2) {
      try {
        String value = getTagValue(requestContext, extraTags[i + 1]);
        tags.add(Tag.of(extraTags[i], value));
      } catch (Throwable ex) {
        logger.error("Failed to get tag value {}={}: ", extraTags[i], extraTags[i + 1], ex);
      }
    }
    return tags;
  }

  private String getTagValue(ContainerRequestContext requestContext, String spec) {
    if (spec.startsWith("$")) {
      int dotIndex = spec.indexOf(".");

      String fieldType = spec;
      String fieldArg = "";
      if (dotIndex > 0) {
        fieldType = spec.substring(0, dotIndex);
        fieldArg = spec.substring(dotIndex + 1);
      }
      switch (fieldType) {
        case "$query":
          return getQueryArg(requestContext, fieldArg);
        case "$method":
          return requestContext.getMethod();
        case "$header":
          return requestContext.getHeaderString(fieldArg);
        case "$path":
          return getPathArg(requestContext, fieldArg);
        default:
          return getQueryArg(requestContext, spec.substring(1));
      }
    } else {
      return spec;
    }
  }
  
  private String getQueryArg(ContainerRequestContext requestContext, String arg) {
    List<String> values = requestContext.getUriInfo().getQueryParameters().get(arg);
    return valuesListToTag(values);
  }
  
  private String getPathArg(ContainerRequestContext requestContext, String arg) {
    List<String> values = requestContext.getUriInfo().getPathParameters().get(arg);
    return valuesListToTag(values);
  }

  static String valuesListToTag(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    } else if (values.size() == 1) {
      return values.get(0);
    } else {
      return values.toString();
    }
  }
  
  private String getMeterName(ContainerRequestContext crc, Timed timed) {
    String name = TimerContainerRequestFilter.getName(crc);
    if (name == null) {
      name = timed.value();
    }
    if (name == null) {
      name = crc.getMethod();
    }
    return name;
  }
  
}
