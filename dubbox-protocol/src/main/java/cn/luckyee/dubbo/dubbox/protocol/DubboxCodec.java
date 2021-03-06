package cn.luckyee.dubbo.dubbox.protocol;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.serialize.fst.FstSerialization;
import org.apache.dubbo.common.serialize.kryo.KryoSerialization;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.codec.ExchangeCodec;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcResult;

import java.io.IOException;
import java.io.InputStream;

import static cn.luckyee.dubbo.dubbox.protocol.CallbackServiceCodec.encodeInvocationArgument;
import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.DECODE_IN_IO_THREAD_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.DEFAULT_DECODE_IN_IO_THREAD;

/**
 * Dubbo codec.
 */
public class DubboxCodec extends ExchangeCodec {

    public static final String NAME = "dubbox";
    public static final String DUBBO_VERSION = Version.getProtocolVersion();
    public static final byte RESPONSE_WITH_EXCEPTION = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_NULL_VALUE = 2;
    public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;
    public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;
    public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Logger log = LoggerFactory.getLogger(DubboxCodec.class);

    @Override
    protected Object decodeBody(Channel channel, InputStream is, byte[] header) throws IOException {
        byte flag = header[2], proto = (byte) (flag & ExchangeCodec.SERIALIZATION_MASK);
        // get request id.
        long id = Bytes.bytes2long(header, 4);
        if ((flag & ExchangeCodec.FLAG_REQUEST) == 0) {
            // decode response.
            Response res = new Response(id);
            if ((flag & ExchangeCodec.FLAG_EVENT) != 0) {
                res.setEvent(true);
            }
            // get status.
            byte status = header[3];
            res.setStatus(status);
            try {
                if (status == Response.OK) {
                    Object data;
                    if (res.isEvent()) {
                        ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                        data = decodeEventData(channel, in);
                    } else {
                        DecodeableRpcResult result;
                        if (channel.getUrl().getParameter(DECODE_IN_IO_THREAD_KEY, DEFAULT_DECODE_IN_IO_THREAD)) {
                            result = new DecodeableRpcResult(channel, res, is,
                                    (Invocation) getRequestData(id), proto);
                            result.decode();
                        } else {
                            result = new DecodeableRpcResult(channel, res,
                                    new UnsafeByteArrayInputStream(readMessageData(is)),
                                    (Invocation) getRequestData(id), proto);
                        }
                        data = result;
                    }
                    res.setResult(data);
                } else {
                    ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                    res.setErrorMessage(in.readUTF());
                }
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode response failed: " + t.getMessage(), t);
                }
                res.setStatus(Response.CLIENT_ERROR);
                res.setErrorMessage(StringUtils.toString(t));
            }
            return res;
        } else {
            // decode request.
            Request req = new Request(id);
            req.setVersion(Version.getProtocolVersion());
            req.setTwoWay((flag & ExchangeCodec.FLAG_TWOWAY) != 0);
            if ((flag & ExchangeCodec.FLAG_EVENT) != 0) {
                req.setEvent(true);
            }
            try {
                Object data;
                if (req.isEvent()) {
                    ObjectInput in = CodecSupport.deserialize(channel.getUrl(), is, proto);
                    data = decodeEventData(channel, in);
                } else {
                    DecodeableRpcInvocation inv;
                    if (channel.getUrl().getParameter(DECODE_IN_IO_THREAD_KEY, DEFAULT_DECODE_IN_IO_THREAD)) {
                        inv = new DecodeableRpcInvocation(channel, req, is, proto);
                        inv.decode();
                    } else {
                        inv = new DecodeableRpcInvocation(channel, req,
                                new UnsafeByteArrayInputStream(readMessageData(is)), proto);
                    }
                    data = inv;
                }
                req.setData(data);
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode request failed: " + t.getMessage(), t);
                }
                // bad request
                req.setBroken(true);
                req.setData(t);
            }

            return req;
        }
    }

    private byte[] readMessageData(InputStream is) throws IOException {
        if (is.available() > 0) {
            byte[] result = new byte[is.available()];
            is.read(result);
            return result;
        }
        return new byte[]{};
    }

    @Override
    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(channel, out, data, DUBBO_VERSION);
    }

    @Override
    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(channel, out, data, DUBBO_VERSION);
    }

    @Override
    protected void encodeRequestData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
//        RpcInvocation inv = (RpcInvocation) data;
//
//        out.writeUTF(version);
//        out.writeUTF(inv.getAttachment(PATH_KEY));
//        out.writeUTF(inv.getAttachment(VERSION_KEY));
//
//        out.writeUTF(inv.getMethodName());
//        out.writeUTF(inv.getParameterTypesDesc());
//        Object[] args = inv.getArguments();
//        if (args != null) {
//            for (int i = 0; i < args.length; i++) {
//                out.writeObject(encodeInvocationArgument(channel, inv, i));
//            }
//        }
//        out.writeAttachments(inv.getObjectAttachments());

        RpcInvocation inv = (RpcInvocation) data;

        out.writeUTF(inv.getAttachment(DUBBO_VERSION_KEY, DUBBO_VERSION));
        out.writeUTF(inv.getAttachment(PATH_KEY));
        out.writeUTF(inv.getAttachment(VERSION_KEY));

        out.writeUTF(inv.getMethodName());

        // NOTICE modified by lishen
        // TODO
        Serialization serialization = this.getSerialization(channel);
        boolean isKryoSerialization = serialization instanceof KryoSerialization;
        boolean isFstSerialization = serialization instanceof FstSerialization;
        if ((isKryoSerialization || isFstSerialization) && !containComplexArguments(inv)) {
            out.writeInt(inv.getParameterTypes().length);
        } else {
            out.writeInt(-1);
            out.writeUTF(ReflectUtils.getDesc(inv.getParameterTypes()));
        }

        Object[] args = inv.getArguments();
        if (args != null)
            for (int i = 0; i < args.length; i++){
                out.writeObject(encodeInvocationArgument(channel, inv, i));
            }
        out.writeObject(inv.getAttachments());
    }

    @Override
    protected void encodeResponseData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
        Result result = (Result) data;
        // currently, the version value in Response records the version of Request
        boolean attach = Version.isSupportResponseAttachment(version);
        // dubbox兼容逻辑
        if("2.0.2".equals(version)) {
            attach = false;
        }
        Throwable th = result.getException();
        if (th == null) {
            Object ret = result.getValue();
            if (ret == null) {
                out.writeByte(attach ? RESPONSE_NULL_VALUE_WITH_ATTACHMENTS : RESPONSE_NULL_VALUE);
            } else {
                out.writeByte(attach ? RESPONSE_VALUE_WITH_ATTACHMENTS : RESPONSE_VALUE);
                out.writeObject(ret);
            }
        } else {
            out.writeByte(attach ? RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS : RESPONSE_WITH_EXCEPTION);
            out.writeThrowable(th);
        }

        if (attach) {
            // returns current version of Response to consumer side.
            result.getObjectAttachments().put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
            out.writeAttachments(result.getObjectAttachments());
        }
    }

    private boolean containComplexArguments(RpcInvocation invocation) {
        for (int i = 0; i < invocation.getParameterTypes().length; ++i) {
            if (invocation.getArguments()[i] == null || invocation.getParameterTypes()[i] != invocation.getArguments()[i].getClass()) {
                return true;
            }
        }

        return false;
    }
}
