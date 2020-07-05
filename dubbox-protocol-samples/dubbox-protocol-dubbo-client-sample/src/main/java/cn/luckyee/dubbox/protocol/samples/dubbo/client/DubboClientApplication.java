package cn.luckyee.dubbox.protocol.samples.dubbo.client;

import cn.luckyee.dubbox.protocol.samples.api.DubboProtocolDemoService;
import cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DubboClientApplication {

    public static final Logger logger = LoggerFactory.getLogger(DubboClientApplication.class);

    @DubboReference(registry = {"zk01"}, version = "1.0.0", check = false)
    DubboProtocolDemoService dubboProtocolDemoService;

    @DubboReference(registry = {"zk02"}, version = "1.0.0", check = false)
    DubboxProtocolDemoService dubboxProtocolDemoService;

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new SpringApplicationBuilder(DubboClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        DubboClientApplication application = context.getBean(DubboClientApplication.class);

        // dubbox's dubbox protocol
        String req1 = "hi";
        String req2 = "I'm dubbo client";
        logger.info("DubboxProtocolDemoService req1 [{}] req2 [{}]", req1, req2);
        String resp = null;
        try {
            resp = application.dubboxProtocolDemoService.service(req1, req2);
        } catch (Exception e) {
            logger.info("", e);
        }
        logger.info("DubboxProtocolDemoService resp [{}]", resp);


        // dubbo's dubbo protocol
        logger.info("DubboProtocolDemoService req1 [{}] req2 [{}]", req1, req2);
        resp = null;
        try {
            resp = application.dubboProtocolDemoService.service(req1, req2);
        } catch (Exception e) {
            logger.info("", e);
        }
        logger.info("DubboProtocolDemoService resp [{}]", resp);
    }

}
