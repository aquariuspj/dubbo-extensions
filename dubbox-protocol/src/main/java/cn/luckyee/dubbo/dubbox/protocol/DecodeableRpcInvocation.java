package cn.luckyee.dubbo.dubbox.protocol;

import com.alibaba.dubbo.common.Constants;
import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec;
import org.apache.dubbo.remoting.Decodeable;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static cn.luckyee.dubbo.dubbox.protocol.CallbackServiceCodec.decodeInvocationArgument;

public class DecodeableRpcInvocation extends RpcInvocation implements Codec, Decodeable {

    private static final Logger log = LoggerFactory.getLogger(DecodeableRpcInvocation.class);

    private Channel channel;

    private byte        serializationType;

    private InputStream inputStream;

    private Request request;

    private volatile boolean hasDecoded;

    public DecodeableRpcInvocation(Channel channel, Request request, InputStream is, byte id) {
        Assert.notNull(channel, "channel == null");
        Assert.notNull(request, "request == null");
        Assert.notNull(is, "inputStream == null");
        this.channel = channel;
        this.request = request;
        this.inputStream = is;
        this.serializationType = id;
    }

    public void decode() throws Exception {
        if (!hasDecoded && channel != null && inputStream != null) {
            try {
                decode(channel, inputStream);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode rpc invocation failed: " + e.getMessage(), e);
                }
                request.setBroken(true);
                request.setData(e);
            } finally {
                hasDecoded = true;
            }
        }
    }

    public void encode(Channel channel, OutputStream output, Object message) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Object decode(Channel channel, InputStream input) throws IOException {
        ObjectInput in = CodecSupport.getSerialization(channel.getUrl(), serializationType)
                .deserialize(channel.getUrl(), input);

        try {
            setAttachment(Constants.DUBBO_VERSION_KEY, in.readUTF());
            setAttachment(Constants.PATH_KEY, in.readUTF());
            setAttachment(Constants.VERSION_KEY, in.readUTF());

            setMethodName(in.readUTF());
            try {
                Object[] args;
                Class<?>[] pts;

                // NOTICE modified by lishen
                int argNum = in.readInt();
                if (argNum >= 0) {
                    if (argNum == 0) {
                        pts = DubboxCodec.EMPTY_CLASS_ARRAY;
                        args = DubboxCodec.EMPTY_OBJECT_ARRAY;
                    } else {
                        args = new Object[argNum];
                        pts = new Class[argNum];
                        for (int i = 0; i < args.length; i++) {
                            try {
                                args[i] = in.readObject();
                                pts[i] = args[i].getClass();
                            } catch (Exception e) {
                                if (log.isWarnEnabled()) {
                                    log.warn("Decode argument failed: " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                } else {
                    String desc = in.readUTF();
                    if (desc.length() == 0) {
                        pts = DubboxCodec.EMPTY_CLASS_ARRAY;
                        args = DubboxCodec.EMPTY_OBJECT_ARRAY;
                    } else {
                        pts = ReflectUtils.desc2classArray(desc);
                        args = new Object[pts.length];
                        for (int i = 0; i < args.length; i++) {
                            try {
                                args[i] = in.readObject(pts[i]);
                            } catch (Exception e) {
                                if (log.isWarnEnabled()) {
                                    log.warn("Decode argument failed: " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
                setParameterTypes(pts);

                Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
                if (map != null && map.size() > 0) {
                    Map<String, String> attachment = getAttachments();
                    if (attachment == null) {
                        attachment = new HashMap<String, String>();
                    }
                    attachment.putAll(map);
                    setAttachments(attachment);
                }
                //decode argument ,may be callback
                for (int i = 0; i < args.length; i++) {
                    args[i] = decodeInvocationArgument(channel, this, pts, i, args[i]);
                }

                setArguments(args);

            } catch (ClassNotFoundException e) {
                throw new IOException(StringUtils.toString("Read invocation data failed.", e));
            }
        } finally {
            // modified by lishen
            if (in instanceof Cleanable) {
                ((Cleanable) in).cleanup();
            }
        }
        return this;
    }

}
