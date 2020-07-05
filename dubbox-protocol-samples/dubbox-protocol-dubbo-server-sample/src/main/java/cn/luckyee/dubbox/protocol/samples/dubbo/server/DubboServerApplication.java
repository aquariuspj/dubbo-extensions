package cn.luckyee.dubbox.protocol.samples.dubbo.server;

import cn.luckyee.dubbox.protocol.samples.dubbo.server.embed.ZkServerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DubboServerApplication {

    static Logger logger = LoggerFactory.getLogger(DubboServerApplication.class);

    @Value("${zkHostPort}")
    public String hostPort;

    @Value("${zkFilename}")
    public String filename;

    public static void main(String[] args) {
        ApplicationContext context = new SpringApplicationBuilder(DubboServerApplication.class)
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


}
