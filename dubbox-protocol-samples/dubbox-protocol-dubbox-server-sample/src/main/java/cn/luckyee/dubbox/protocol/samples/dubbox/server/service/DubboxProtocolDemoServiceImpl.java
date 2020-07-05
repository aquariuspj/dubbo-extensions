package cn.luckyee.dubbox.protocol.samples.dubbox.server.service;

import cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DubboxProtocolDemoServiceImpl implements DubboxProtocolDemoService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String service(String req1, String req2) {
        logger.info("req info is [{}] [{}]", req1, req2);
        String resp = "Hi, I'm dubbox server!";
        logger.info("resp info is [{}]", resp);
        return resp;
    }
}
