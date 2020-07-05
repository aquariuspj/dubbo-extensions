package cn.luckyee.dubbo.dubbox.protocol.impl;

import cn.luckyee.dubbo.dubbox.protocol.api.DubboxProtocolDemoService;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class DubboxProtocolDemoServiceImpl implements DubboxProtocolDemoService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Map<String, String> service(Map<String, String> req) {
        logger.info("req is : [{}]", req);
        Map<String, String> resp = null;
        if(req == null) {
            resp = new HashMap<>();
        } else {
            resp = req;
        }
        resp.put("resp", "Hi, I'm dubbox service!");
        logger.info("resp is : [{}]", resp);
        return resp;
    }
}
