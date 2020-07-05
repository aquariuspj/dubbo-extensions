package cn.luckyee.dubbo.dubbox.protocol;

import cn.luckyee.dubbo.dubbox.protocol.api.DubboxProtocolDemoService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DubboxProtocolTest.class})
@TestPropertySource(locations = "classpath:/dubbox.properties")
@EnableDubbo
public class DubboxProtocolTest {

    @Reference
    DubboxProtocolDemoService dubboxProtocolDemoService;

    public static void main(String[] args) {
        SpringApplication.run(DubboxProtocolTest.class, args);
    }

    @Test
    public void getUserByIdTest() {
        Map<String, String> dubboxReq = new HashMap<>();
        dubboxReq.put("req", "Hi, I'm dubbox client!");
        Map<String, String> dubboxResp = dubboxProtocolDemoService.service(dubboxReq);
        Assert.assertNotNull(dubboxResp);
        Assert.assertEquals(dubboxReq.get("req"), dubboxResp.get("req"));
        Assert.assertTrue("Hi, I'm dubbox service!".equals(dubboxResp.get("resp")));
    }

}
