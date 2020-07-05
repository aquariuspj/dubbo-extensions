package cn.luckyee.dubbox.protocol.samples.dubbo.server.service;

import cn.luckyee.dubbox.protocol.samples.api.DubboProtocolDemoService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@DubboService(registry = "zk01", protocol = "dubbo", version = "1.0.0")
public class DubboProtocolDemoServiceImpl implements DubboProtocolDemoService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String service(String req1, String req2) {
        logger.info("req info is [{}] [{}]", req1, req2);
        String resp = "Hi, I'm dubbo server!";
        logger.info("resp info is [{}]", resp);
        return resp;
    }
}
