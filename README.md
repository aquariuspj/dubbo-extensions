# dubbo-extensions

该方案通过扩展在dubbo下扩展出兼容dubbox的DubboxProtocol，从而在dubbo的框架下提供一种兼容dubbox的方案。

详细代码参考：https://github.com/aquariuspj/dubbo-extensions.git

<!-- TOC -->
- [dubbo与dubbox不兼容的原因](#dubbo与dubbox不兼容的原因)
- [解决方案](#解决方案)
- [使用说明](#使用说明)
  - [dubbo项目调用dubbox项目](#dubbo项目调用dubbox项目)
  - [dubbox项目调用dubbo项目](#dubbox项目调用dubbo项目)
- [附录1 dubbo与dubbox协议相互调用的报错信息](#附录1-dubbo与dubbox协议相互调用的报错信息)
  - [1. dubbo consumer 调用 dubbox provider.](#1-dubbo-consumer-调用-dubbox-provider)
  - [2. dubbox consumer 调用 duubo provider.](#dubbox-consumer-调用-duubo-provider)
- [附录2，DecodeableRpcInvocation.decode()方法源码对比](#附录2decodeablerpcinvocationdecode方法源码对比)
  - [dubbo DecodeableRpcInvocation](#dubbo-decodeablerpcinvocation)
  - [dubbox DecodeableRpcInvocation](#dubbox-decodeablerpcinvocation)
- [附录3，DubboxCodec.encodeRequestData()方法源码对比](#附录3dubboxcodecencoderequestdata方法源码对比)
  - [dubbo DubboxCodec](#dubbo-dubboxcodec)
  - [dubbox DubboxCodec](#dubbox-dubboxcodec)
<!-- /TOC -->

## dubbo与dubbox不兼容的原因

测试版本：  
框架  | 源码地址 | branch/tag | version
:---: | --- | --- | ---
*dubbox* | https://github.com/dangdangdotcom/dubbox | branch:master | 2.8.4
*dubbo*  | https://github.com/apache/dubbo | tag:2.7.7 | 2.7.7

经过对两个版本的DecodeableRpcInvocation decode方法进行对比，可以发现二者的主要差异点在于反序列化的元素上【参考《附录2，DecodeableRpcInvocation.decode()方法源码对比》】：

框架  | 反序列化顺序
:---: | ---
*dubbox* | 1.dubboVersion; <br/> 2.path; <br/> 3.version; <br/> 4.methodName; <br/> 5.desc.
*dubbo*  | 1.dubboVersion; <br/> 2.path; <br/> 3.version; <br/> 4.methodName; <br/> 5.argNum; <br/> 6.argNum<-1时，有desc.


我们已知两个版本的反序列化顺序，那么可以猜测两个版本的序列化顺序也一定和反序列化保持一致的，通过代码跟踪发现确实如此。【参考《附录3，DubboxCodec.encodeRequestData()方法源码对比》】。

## 解决方案

1. 通过修改dubbox源码，判断dubboVersion执行不同的序列化/反序列化逻辑来兼容dubbo。
2. 通过修改dubbo源码，判断dubboVersion执行不同的序列化/反序列化逻辑来兼容dubbox。
3. 通过在dubbox和dubbo上同时扩展dubbox协议来相互兼容，同时防止二者的dubbo协议互串。
4. 其他。

本项目采用第三种解决方案。

实现思路为：

1. 通过Dubbo的SPI机制，先在Dubbo中扩展出兼容Dubbox的DubboxProtocol协议，
2. 通过Dubbox的SPI机制，在dubbox中为dubbo协议新增dubbox的别名，从而与(1.)能够相互识别DubboxProtocol。
3. 各自在注册中心上暴露基于dubbox协议的服务。
4. 相互调用。

## 使用说明

详细代码可以参考dubbox-protocol-samples项目。

### dubbo项目调用dubbox项目

1. **在dubbox中扩展dubbox协议：** 在dubbox项目中的resource/META-INF/dubbo目录下新建com.alibaba.dubbo.rpc.Protocol文件，并填入内容`dubbox=com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol`;

2. **在dubbo中扩展dubbox协议：** 引入本项目。
    ```xml
    <dependency>
        <groupId>cn.luckyee.dubbo</groupId>
        <artifactId>dubbox-protocol-all</artifactId>
        <version>1.1-RELEASE</version>
    </dependency>
    ```

3. **将原dubbox项目中的服务注册为dubbox协议** （注意，zkgroup一定要分开，否则dubbox项目的dubbo协议会被dubbo项目误认为dubbo协议从而导致调用失败）
    ```xml
    <dubbo:registry id="zk01" address="zookeeper://127.0.0.1:2181" group="zkg01" />

    <dubbo:registry id="zk02" address="zookeeper://127.0.0.1:2181" group="zkg02" />

    <dubbo:protocol name="dubbo" port="20890" default="false" />

    <dubbo:protocol name="dubbox" port="20891" default="false" />

    <dubbo:service interface="cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService" ref="dubboxProtocolDemoService" registry="zk01" protocol="dubbo" version="1.0.0" />

    <dubbo:service interface="cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService" ref="dubboxProtocolDemoService" registry="zk02" protocol="dubbox" version="1.0.0" />
    ```
4. **将dubbo项目中的consumer配置到同一个group引入该dubbox服务**

   **配置文件**
    ```properties
    dubbo.registries.zk01.address=zookeeper://127.0.0.1:2181
    dubbo.registries.zk01.group=zkg01
    dubbo.registries.zk02.address=zookeeper://127.0.0.1:2181
    dubbo.registries.zk02.group=zkg02

    dubbo.protocols.dubbox.name=dubbox
    dubbo.protocols.dubbo.name=dubbo
    ```

    **引入服务**
    ```java
    @DubboReference(registry = {"zk02"}, version = "1.0.0", check = false)
    DubboxProtocolDemoService dubboxProtocolDemoService;
    ```

### dubbox项目调用dubbo项目

1. **在dubbox中扩展dubbox协议：** 在dubbox项目中的resource/META-INF/dubbo目录下新建com.alibaba.dubbo.rpc.Protocol文件，并填入内容`dubbox=com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol`;

2. **在dubbo中扩展dubbox协议：** 引入本项目。
    ```xml
    <dependency>
        <groupId>cn.luckyee.dubbo</groupId>
        <artifactId>dubbox-protocol-all</artifactId>
        <version>1.1-RELEASE</version>
    </dependency>
    ```

3. **将原dubbo项目中的服务注册为dubbox协议**

    假设原始服务如下：
    ```java
    @Service
    @DubboService(registry = "zk01", protocol = "dubbo", version = "1.0.0")
    public class DubboProtocolDemoServiceImpl implements DubboProtocolDemoService {
        ……
    }
    ```

    建议新增包装接口与实现类：
    ```java
    @DubboService(registry = "zk02", protocol = "dubbox", version = "1.0.0")
    public class DubboProtocolDemoServiceImpl2 implements DubboProtocolDemoService2 {
        
        // 此处是暴露在zk01的dubbo协议的原服务
        @Autowired
        DubboProtocolDemoServiceImpl dubboProtocolDemoService;

        @Override
        public String service(String req1, String req2) {
            return dubboProtocolDemoService.service(req1, req2);
        }
    }
    ```

    **配置文件**
    ```properties   
    dubbo.protocols.dubbo.name=dubbo
    dubbo.protocols.dubbo.port=20892
    dubbo.protocols.dubbox.name=dubbox
    dubbo.protocols.dubbox.port=20893

    dubbo.registries.zk01.address=zookeeper://127.0.0.1:2181
    dubbo.registries.zk01.group=zkg01
    dubbo.registries.zk02.address=zookeeper://127.0.0.1:2181
    dubbo.registries.zk02.group=zkg02
    ```


4. **将dubbox项目中的consumer配置到同一个group并引入该dubbo服务**

    ```xml
    <dubbo:registry id="zk01" address="zookeeper://127.0.0.1:2181" group="zkg01" default="false"/>

    <dubbo:registry id="zk02" address="zookeeper://127.0.0.1:2181" group="zkg02" default="false"/>

    <dubbo:protocol name="dubbox" />

    <dubbo:protocol name="dubbo" />

    <dubbo:reference id="dubboxProtocolDemoService" interface="cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService" protocol="dubbo" registry="zk01" check="false" version="1.0.0" />

    <dubbo:reference id="dubboProtocolDemoService2" interface="cn.luckyee.dubbox.protocol.samples.api.DubboProtocolDemoService2" protocol="dubbox" registry="zk02" check="false" version="1.0.0" />
    ```

经过上述配置，dubbo项目能够通过dubbox协议与dubbox项目互通。

## 附录1 dubbo与dubbox协议相互调用的报错信息

### 1. dubbo consumer 调用 dubbox provider.

*dubbo consumer错误信息*
```
Exception in thread "main" org.apache.dubbo.rpc.RpcException: Failed to invoke the method service in the service cn.luckyee.demo.dubbo.api.S000001. Tried 3 times of the providers [192.168.1.4:20890] (1/2) from the registry 127.0.0.1:2181 on the consumer 192.168.1.4 using the dubbo version 2.7.7. Last error is: Failed to invoke remote method: service, provider: dubbo://192.168.1.4:20890/cn.luckyee.demo.dubbo.api.S000001?anyhost=true&application=dubbo-client&check=false&default=false&dubbo=2.8.4&generic=false&init=false&interface=cn.luckyee.demo.dubbo.api.S000001&methods=service&pid=23144&qos.enable=false&register.ip=192.168.1.4&remote.application=dubbox-server&revision=1.0.0&side=consumer&sticky=false&timestamp=1593945022144&version=1.0.0, cause: org.apache.dubbo.remoting.RemotingException: Fail to decode request due to: RpcInvocation [……]
	at org.apache.dubbo.rpc.cluster.support.FailoverClusterInvoker.doInvoke(FailoverClusterInvoker.java:113)
	at org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker.invoke(AbstractClusterInvoker.java:259)
	at org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor.intercept(ClusterInterceptor.java:47)
	at org.apache.dubbo.rpc.cluster.support.wrapper.AbstractCluster$InterceptorInvokerNode.invoke(AbstractCluster.java:92)
	at org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker.invoke(MockClusterInvoker.java:82)
	at org.apache.dubbo.rpc.proxy.InvokerInvocationHandler.invoke(InvokerInvocationHandler.java:74)
	at org.apache.dubbo.common.bytecode.proxy0.service(proxy0.java)
	……
```

*dubbox provider错误信息*

```
com.alibaba.com.caucho.hessian.io.HessianProtocolException: expected integer at 0x12 java.lang.String (Ljava/lang/Object;)
	at com.alibaba.com.caucho.hessian.io.Hessian2Input.error(Hessian2Input.java:2720)
	at com.alibaba.com.caucho.hessian.io.Hessian2Input.expect(Hessian2Input.java:2691)
	at com.alibaba.com.caucho.hessian.io.Hessian2Input.readInt(Hessian2Input.java:773)
	at com.alibaba.dubbo.common.serialize.support.hessian.Hessian2ObjectInput.readInt(Hessian2ObjectInput.java:58)
	at com.alibaba.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation.decode(DecodeableRpcInvocation.java:106)
	at com.alibaba.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation.decode(DecodeableRpcInvocation.java:74)
	at com.alibaba.dubbo.rpc.protocol.dubbo.DubboCodec.decodeBody(DubboCodec.java:138)
	at com.alibaba.dubbo.remoting.exchange.codec.ExchangeCodec.decode(ExchangeCodec.java:134)
	at com.alibaba.dubbo.remoting.exchange.codec.ExchangeCodec.decode(ExchangeCodec.java:95)
	at com.alibaba.dubbo.rpc.protocol.dubbo.DubboCountCodec.decode(DubboCountCodec.java:46)
  ……
```


### 2. dubbox consumer 调用 duubo provider.

dubbo provider和dubbox consumer错误信息一样，说明provider将错误信息返回给了consumer。

```
java.lang.IllegalArgumentException: Service not found: ……
	at org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation.decode(DecodeableRpcInvocation.java:134) ~[dubbo-2.7.7.jar:2.7.7]
	at org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation.decode(DecodeableRpcInvocation.java:80) ~[dubbo-2.7.7.jar:2.7.7]
	at org.apache.dubbo.remoting.transport.DecodeHandler.decode(DecodeHandler.java:57) [dubbo-2.7.7.jar:2.7.7]
	at org.apache.dubbo.remoting.transport.DecodeHandler.received(DecodeHandler.java:44) [dubbo-2.7.7.jar:2.7.7]
  ……
```

## 附录2，DecodeableRpcInvocation.decode()方法源码对比

### dubbo DecodeableRpcInvocation
```java
@Override
public Object decode(Channel channel, InputStream input) throws IOException {
    ……
    String dubboVersion = in.readUTF();
    ……
    String path = in.readUTF();
    ……
    setAttachment(VERSION_KEY, in.readUTF());
    setMethodName(in.readUTF());
    String desc = in.readUTF();
    ……
                    args[i] = in.readObject(pts[i]);
    ……
        Map<String, Object> map = in.readAttachments();
    ……
    return this;
}
```

### dubbox DecodeableRpcInvocation
```java
public Object decode(Channel channel, InputStream input) throws IOException {
    ……
        setAttachment(Constants.DUBBO_VERSION_KEY, in.readUTF());
        setAttachment(Constants.PATH_KEY, in.readUTF());
        setAttachment(Constants.VERSION_KEY, in.readUTF());
        setMethodName(in.readUTF());
        ……
            int argNum = in.readInt();
            if (argNum >= 0) {
                ……
                            args[i] = in.readObject();
                ……
            } else {
                String desc = in.readUTF();
                ……
                            args[i] = in.readObject(pts[i]);
                ……
            }
            Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
            ……
    return this;
}
```

## 附录3，DubboxCodec.encodeRequestData()方法源码对比

### dubbo DubboxCodec
```java
@Override
protected void encodeRequestData(Channel channel, ObjectOutput out, Object data, String version) throws IOException {
    RpcInvocation inv = (RpcInvocation) data;
    out.writeUTF(version);
    // https://github.com/apache/dubbo/issues/6138
    String serviceName = inv.getAttachment(INTERFACE_KEY);
    if (serviceName == null) {
        serviceName = inv.getAttachment(PATH_KEY);
    }
    out.writeUTF(serviceName);
    out.writeUTF(inv.getAttachment(VERSION_KEY));

    out.writeUTF(inv.getMethodName());
    out.writeUTF(inv.getParameterTypesDesc());
    Object[] args = inv.getArguments();
    if (args != null) {
        for (int i = 0; i < args.length; i++) {
            out.writeObject(encodeInvocationArgument(channel, inv, i));
        }
    }
    out.writeAttachments(inv.getObjectAttachments());
}
```

### dubbox DubboxCodec
```java
@Override
protected void encodeRequestData(Channel channel, ObjectOutput out, Object data) throws IOException {
    RpcInvocation inv = (RpcInvocation) data;

    out.writeUTF(inv.getAttachment(Constants.DUBBO_VERSION_KEY, DUBBO_VERSION));
    out.writeUTF(inv.getAttachment(Constants.PATH_KEY));
    out.writeUTF(inv.getAttachment(Constants.VERSION_KEY));

    out.writeUTF(inv.getMethodName());

    // NOTICE modified by lishen
    // TODO
    if (getSerialization(channel) instanceof OptimizedSerialization && !containComplexArguments(inv)) {
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
```

如有任何问题，欢迎交流指教。

首发于 https://www.cnblogs.com/prpl ，未经许可不允许转载，谢谢合作。
