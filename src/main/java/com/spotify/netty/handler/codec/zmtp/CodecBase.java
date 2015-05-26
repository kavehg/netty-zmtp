package com.spotify.netty.handler.codec.zmtp;

//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.ChannelStateEvent;
//import org.jboss.netty.channel.Channels;
//import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
//import org.jboss.netty.handler.codec.replay.VoidEnum;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.TimeUnit;

/**
 * An abstract base class for common functionality to the ZMTP codecs.
 */
abstract class CodecBase extends ReplayingDecoder<Void> {

    private volatile ChannelHandlerContext ctx;

    protected final ZMTPSession session;
    protected HandshakeListener listener;

    CodecBase(ZMTPSession session) {
        this.session = session;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {

            }
        }, 1000l, TimeUnit.MILLISECONDS);

        super.channelActive(ctx);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e)
            throws Exception {

        setListener(new HandshakeListener() {
            @Override
            public void handshakeDone(int protocolVersion, byte[] remoteIdentity) {
                session.setRemoteIdentity(remoteIdentity);
                session.setActualVersion(protocolVersion);
                updatePipeline(ctx.getPipeline(), session);
                ctx.sendUpstream(e);
            }
        });

        Channels.write(ctx, Channels.future(ctx.getChannel()), onConnect());
        this.session.setChannel(ctx.channel());
    }

    abstract ByteBuf onConnect();

    abstract boolean inputOutput(final ByteBuf buffer, final MessageWriter out) throws ZMTPException;

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buffer,
                            Void v) throws ZMTPException {
        buffer.markReaderIndex();
        boolean done = inputOutput(buffer, new MessageWriter(ctx));
        if (!done) {
            return null;
        }

        // This follows the pattern for dynamic pipelines documented in
        // http://netty.io/3.6/api/org/jboss/netty/handler/codec/replay/ReplayingDecoder.html
        if (actualReadableBytes() > 0) {
            return buffer.readBytes(actualReadableBytes());
        }

        return null;
    }

    void setListener(HandshakeListener listener) {
        this.listener = listener;
    }


    private void updatePipeline(ChannelPipeline pipeline,
                                ZMTPSession session) {
        pipeline.addAfter(pipeline.getContext(this).getName(), "zmtpEncoder",
                new ZMTPFramingEncoder(session));
        pipeline.addAfter("zmtpEncoder", "zmtpDecoder",
                new ZMTPFramingDecoder(session));
        pipeline.remove(this);
    }

    /**
     * Parse and return the remote identity octets from a ZMTP/1.0 greeting.
     */
    static byte[] readZMTP1RemoteIdentity(final ByteBuf buffer) throws ZMTPException {
        final long len = ZMTPUtils.decodeLength(buffer);
        if (len > 256) {
            // spec says the ident string can be up to 255 chars
            throw new ZMTPException("Remote identity longer than the allowed 255 octets");
        }

        // skip the flags byte
        buffer.skipBytes(1);

        if (len == 1) {
            return null;
        }
        final byte[] identity = new byte[(int) len - 1];
        buffer.readBytes(identity);
        return identity;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.handlerAdded(ctx);
    }

    private final class LazyChannelPromise extends DefaultPromise<Channel> {

        @Override
        protected EventExecutor executor() {
            if (ctx == null) {
                throw new IllegalStateException();
            }
            return ctx.executor();
        }
    }
}
