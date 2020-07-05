package cn.luckyee.dubbo.dubbox.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeClient;
import org.apache.dubbo.remoting.transport.ClientDelegate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.remoting.Constants.SENT_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.CALLBACK_SERVICE_KEY;

/**
 * Server push uses this Invoker to continuously push data to client.
 * Wrap the existing invoker on the channel.
 */
class ChannelWrappedInvoker<T> extends AbstractInvoker<T> {

    private final Channel channel;
    private final String serviceKey;
    private final ExchangeClient currentClient;

    ChannelWrappedInvoker(Class<T> serviceType, Channel channel, URL url, String serviceKey) {
        super(serviceType, url, new String[]{GROUP_KEY, TOKEN_KEY, TIMEOUT_KEY});
        this.channel = channel;
        this.serviceKey = serviceKey;
        this.currentClient = new HeaderExchangeClient(new ChannelWrappedInvoker.ChannelWrapper(this.channel), false);
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        // use interface's name as service path to export if it's not found on client side
        inv.setAttachment(PATH_KEY, getInterface().getName());
        inv.setAttachment(CALLBACK_SERVICE_KEY, serviceKey);

        try {
            if (RpcUtils.isOneway(getUrl(), inv)) { // may have concurrency issue
                currentClient.send(inv, getUrl().getMethodParameter(invocation.getMethodName(), SENT_KEY, false));
                return AsyncRpcResult.newDefaultAsyncResult(invocation);
            } else {
                CompletableFuture<AppResponse> appResponseFuture = currentClient.request(inv).thenApply(obj -> (AppResponse) obj);
                return new AsyncRpcResult(appResponseFuture, inv);
            }
        } catch (RpcException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage(), e);
        } catch (Throwable e) { // here is non-biz exception, wrap it.
            throw new RpcException(e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
//        super.destroy();
//        try {
//            channel.close();
//        } catch (Throwable t) {
//            logger.warn(t.getMessage(), t);
//        }
    }

    public static class ChannelWrapper extends ClientDelegate {

        private final Channel channel;
        private final URL url;

        ChannelWrapper(Channel channel) {
            this.channel = channel;
            this.url = channel.getUrl().addParameter("codec", DubboxCodec.NAME);
        }

        @Override
        public URL getUrl() {
            return url;
        }

        @Override
        public ChannelHandler getChannelHandler() {
            return channel.getChannelHandler();
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return channel.getLocalAddress();
        }

        @Override
        public void close() {
            channel.close();
        }

        @Override
        public boolean isClosed() {
            return channel == null || channel.isClosed();
        }

        @Override
        public void reset(URL url) {
            throw new RpcException("ChannelInvoker can not reset.");
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return channel.getLocalAddress();
        }

        @Override
        public boolean isConnected() {
            return channel != null && channel.isConnected();
        }

        @Override
        public boolean hasAttribute(String key) {
            return channel.hasAttribute(key);
        }

        @Override
        public Object getAttribute(String key) {
            return channel.getAttribute(key);
        }

        @Override
        public void setAttribute(String key, Object value) {
            channel.setAttribute(key, value);
        }

        @Override
        public void removeAttribute(String key) {
            channel.removeAttribute(key);
        }

        @Override
        public void reconnect() throws RemotingException {

        }

        @Override
        public void send(Object message) throws RemotingException {
            channel.send(message);
        }

        @Override
        public void send(Object message, boolean sent) throws RemotingException {
            channel.send(message, sent);
        }
    }
}
