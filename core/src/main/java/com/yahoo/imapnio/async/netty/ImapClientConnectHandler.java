package com.yahoo.imapnio.async.netty;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncCreateSessionResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.client.ImapFuture;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.async.internal.ImapAsyncSessionImpl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class handles the business logic of how to process messages and handle events.
 */
public class ImapClientConnectHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** Literal for the name registered in pipeline. */
    public static final String HANDLER_NAME = "ImapClientConnectHandler";

    /** Future for the created session. */
    private ImapFuture<ImapAsyncCreateSessionResponse> sessionCreatedFuture;

    /** Logger instance. */
    private Logger logger;

    /** Logging option for this session. */
    private DebugMode logOpt;

    /** Session Id. */
    private int sessionId;

    /**
     * Initializes @{code ImapClientConnectHandler} to process ok greeting after connection.
     *
     * @param sessionFuture imap session future, should be set to done once ok is received
     * @param logger the @{code Logger} instance for @{ImapAsyncSessionImpl}
     * @param logOpt logging option for the session to be created
     * @param sessionId the session id
     */
    public ImapClientConnectHandler(@Nonnull final ImapFuture<ImapAsyncCreateSessionResponse> sessionFuture, @Nonnull final Logger logger,
            @Nonnull final DebugMode logOpt, final int sessionId) {
        this.sessionCreatedFuture = sessionFuture;
        this.logger = logger;
        this.logOpt = logOpt;
        this.sessionId = sessionId;
    }

    @Override
    public void decode(final ChannelHandlerContext ctx, final IMAPResponse serverResponse, final List<Object> out) {
        final ChannelPipeline pipeline = ctx.pipeline();
        // this handler is solely used to detect connect greeting from server, job done, removing it
        pipeline.remove(HANDLER_NAME);

        if (serverResponse.isOK()) { // we can call it successful only when response is ok
            // add the command response handler
            final ImapAsyncSessionImpl session = new ImapAsyncSessionImpl(ctx.channel(), logger, logOpt, sessionId, pipeline);
            final ImapAsyncCreateSessionResponse response = new ImapAsyncCreateSessionResponse(session, serverResponse);
            sessionCreatedFuture.done(response);

        } else {
            logger.error("[{}] Server response without OK:{}", sessionId, serverResponse.toString());
            sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_WITHOUT_OK_RESPONSE));
        }
        cleanup();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        logger.error("[{}] Connection failed due to encountering exception:{}.", sessionId, cause);
        sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_EXCEPTION, cause));
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof IdleStateEvent) { // Handle the IdleState if needed
            final IdleStateEvent event = (IdleStateEvent) msg;
            if (event.state() == IdleState.READER_IDLE) {
                logger.error("[{}] Connection failed due to taking longer than configured allowed time.", sessionId);
                sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_EXCEED_IDLE_MAX));
                // closing the channel if server is not responding with OK response for max read timeout limit
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (sessionCreatedFuture == null) {
            return; // cleanup() has been called, leave
        }
        sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_INACTIVE));
        cleanup();
    }

    /**
     * Avoids loitering.
     */
    private void cleanup() {
        sessionCreatedFuture = null;
        logger = null;
        logOpt = null;
    }
}
