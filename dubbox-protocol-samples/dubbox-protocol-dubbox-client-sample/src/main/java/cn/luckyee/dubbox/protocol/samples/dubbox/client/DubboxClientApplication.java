package cn.luckyee.dubbox.protocol.samples.dubbox.client;

import cn.luckyee.dubbox.protocol.samples.api.DubboProtocolDemoService2;
import cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;

@ImportResource({"dubbox-consumer.xml"})
@SpringBootApplication
public class DubboxClientApplication {

    public static final Logger logger = LoggerFactory.getLogger(DubboxClientApplication.class);

    public static void main(String[] args) {
        ApplicationContext context = new SpringApplicationBuilder(DubboxClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        // dubbox's dubbo protocol
        DubboxProtocolDemoService dubboxProtocolDemoService = context.getBean(DubboxProtocolDemoService.class);
        String req1 = "hi";
        String req2 = "I'm dubbox client";
        logger.info("DubboxProtocolDemoService req1 [{}] req2 [{}]", req1, req2);
        String resp = null;
        try {
            resp = dubboxProtocolDemoService.service(req1, req2);
        } catch (Exception e) {
            logger.info("", e);
        }
        logger.info("DubboxProtocolDemoService resp [{}]", resp);


        // dubbo's dubbox protocol
        DubboProtocolDemoService2 dubboProtocolDemoService = context.getBean(DubboProtocolDemoService2.class);
        logger.info("DubboProtocolDemoService req1 [{}] req2 [{}]", req1, req2);
        resp = null;
        try {
            resp = dubboProtocolDemoService.service(req1, req2);
        } catch (Exception e) {
            logger.info("", e);
        }
        logger.info("DubboProtocolDemoService resp [{}]", resp);
    }

}
