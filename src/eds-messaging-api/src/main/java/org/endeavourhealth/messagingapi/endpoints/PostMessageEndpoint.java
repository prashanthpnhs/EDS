package org.endeavourhealth.messagingapi.endpoints;

import com.codahale.metrics.annotation.Timed;
import org.apache.http.HttpStatus;
import org.endeavourhealth.common.utility.MetricsHelper;
import org.endeavourhealth.core.configuration.ConfigWrapper;
import org.endeavourhealth.core.configuration.Pipeline;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.audit.ExchangeGeneralErrorDalI;
import org.endeavourhealth.core.database.dal.audit.models.Exchange;
import org.endeavourhealth.core.database.dal.audit.models.HeaderKeys;
import org.endeavourhealth.core.messaging.pipeline.PipelineProcessor;
import org.endeavourhealth.transform.common.AuditWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@Path("/")
public class PostMessageEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(PostMessageEndpoint.class);

    private static final ExchangeGeneralErrorDalI errorDal = DalProvider.factoryExchangeGeneralErrorDal();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(absolute = true, name="PostMessageEndpoint.PostMessage")
    @Path("/PostMessage")
    @RolesAllowed({"eds_messaging_post"})
    public Response postMessage(@Context HttpHeaders headers, String body) throws Throwable {
        MetricsHelper.recordEvent("post-message");

        UUID exchangeId = UUID.randomUUID();

        Pipeline pipeline = ConfigWrapper.getInstance().getPostMessage().getPipeline();

        Response response = null;
        try {
            response = process(headers, body, pipeline, exchangeId);

            if (response.getStatus() != HttpStatus.SC_OK
                    && response.getStatus() != HttpStatus.SC_ACCEPTED) {

                errorDal.save(exchangeId, response.getEntity().toString());
            }
        } catch (Throwable throwable) {
            // save the error message in the DB
            errorDal.save(exchangeId, throwable.getMessage());
            throw throwable;
        }

        return response;
    }

    @POST
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    @Path("/PostMessageAsync")
    @Timed(absolute = true, name="PostMessageEndpoint.PostMessageAsync")
    @RolesAllowed({"eds_messaging_post"})
    public Response postMessageAsync(@Context HttpHeaders headers, String body) throws Throwable {
        MetricsHelper.recordEvent("post-message-async");

        UUID exchangeId = UUID.randomUUID();

        Pipeline pipeline = ConfigWrapper.getInstance().getPostMessageAsync().getPipeline();

        Response response = null;
        try {
            response = process(headers, body, pipeline, exchangeId);

            if (response.getStatus() != HttpStatus.SC_OK
                    && response.getStatus() != HttpStatus.SC_ACCEPTED) {

                errorDal.save(exchangeId, response.getEntity().toString());
            }
        } catch (Throwable throwable) {
            // save the error message in the DB
            errorDal.save(exchangeId, throwable.getMessage());
            throw throwable;
        }

        return response;
    }


    private Response process(HttpHeaders headers, String body, Pipeline pipeline, UUID exchangeId) throws Exception {

        Exchange exchange = new Exchange();
        exchange.setId(exchangeId);
        exchange.setBody(body);
        exchange.setTimestamp(new Date());
        exchange.setHeaders(new HashMap<>());

        //until we pass all validation, this exchange should not be allowed to be queued
        exchange.setHeaderAsBoolean(HeaderKeys.AllowQueueing, Boolean.FALSE);

        for (String key : headers.getRequestHeaders().keySet()) {
            //skip the authorization header, since that's comparatively huge and there's no need to carry it through RabbbitMQ
            if (key.equalsIgnoreCase("authorization")) {
                continue;
            }
            exchange.setHeader(key, headers.getHeaderString(key));
        }

        //commit what we've just received to the DB
        try {
            AuditWriter.writeExchange(exchange);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write exchange to database", ex);
        }

        PipelineProcessor processor = new PipelineProcessor(pipeline);
        if (processor.execute(exchange)) {

            return Response
                    .ok()
                    .entity(exchangeId.toString())
                    .build();

        } else {

            return Response
                    .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .entity(exchange.getException().getMessage())
                    .build();
        }
    }
}
