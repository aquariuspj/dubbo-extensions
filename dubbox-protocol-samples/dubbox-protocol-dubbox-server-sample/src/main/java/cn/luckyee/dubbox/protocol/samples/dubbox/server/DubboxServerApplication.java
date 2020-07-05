package cn.luckyee.dubbox.protocol.samples.dubbox.server;

import cn.luckyee.dubbox.protocol.samples.api.DubboxProtocolDemoService;
import cn.luckyee.dubbox.protocol.samples.dubbox.server.embed.ZkServerRunner;
import cn.luckyee.dubbox.protocol.samples.dubbox.server.service.DubboxProtocolDemoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;

@ImportResource({"dubbox-provider.xml"})
@EnableAsync
@SpringBootApplication
public class DubboxServerApplication {

    static Logger logger = LoggerFactory.getLogger(DubboxServerApplication.class);

    @Value("${zkHostPort}")
    public String hostPort;

    @Value("${zkFilename}")
    public String filename;

    public static void main(String[] args) {
        ApplicationContext context = new SpringApplicationBuilder(DubboxServerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public ZkServerRunner zkServerRunner() {
        try {
            final ZkServerRunner zkServerRunner = new ZkServerRunner(hostPort, filename);
            new Thread(() -> {
                zkServerRunner.initializeAndRun();
            }).start();
            Thread.sleep(5000);
            return zkServerRunner;
        } catch (Exception e) {
            logger.warn("", e);
        }
        return new ZkServerRunner(hostPort, filename);
    }

    @Bean
    public DubboxProtocolDemoService dubboxProtocolDemoService() {
        return new DubboxProtocolDemoServiceImpl();
    }
}
