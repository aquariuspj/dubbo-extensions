package cn.luckyee.dubbox.protocol.samples.dubbo.server.service;

import cn.luckyee.dubbox.protocol.samples.api.DubboProtocolDemoService2;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService(registry = "zk02", protocol = "dubbox", version = "1.0.0")
public class DubboProtocolDemoServiceImpl2 implements DubboProtocolDemoService2 {

    @Autowired
    DubboProtocolDemoServiceImpl dubboProtocolDemoService;

    @Override
    public String service(String req1, String req2) {
        return dubboProtocolDemoService.service(req1, req2);
    }
}
