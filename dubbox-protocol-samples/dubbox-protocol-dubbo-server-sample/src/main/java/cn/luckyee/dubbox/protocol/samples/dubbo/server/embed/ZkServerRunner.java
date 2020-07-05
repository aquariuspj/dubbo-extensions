package cn.luckyee.dubbox.protocol.samples.dubbo.server.embed;

import org.apache.zookeeper.audit.ZKAuditProvider;
import org.apache.zookeeper.jmx.ManagedUtil;
import org.apache.zookeeper.server.ExitCode;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;

/**
 * 内嵌的zk服务器
 */
public class ZkServerRunner {

    Logger log = LoggerFactory.getLogger(getClass());

    public String hostPort;

    public String filename;

    ZooKeeperServerMain zkMain;

    public ZkServerRunner(String hostPort, String filename) {
        this.hostPort = hostPort;
        this.filename = filename;
        zkMain = new ZooKeeperServerMain();
    }

    /**
     * 启动嵌入的zk实例
     */
    public void initializeAndRun() {
        try {
            try {
                ManagedUtil.registerLog4jMBeans();
            } catch (JMException e) {
                log.warn("Unable to register log4j JMX control", e);
            }

            ServerConfig config = new ServerConfig();
            config.parse(new String[]{hostPort, filename});

            zkMain.runFromConfig(config);
        } catch (IllegalArgumentException var3) {
            log.error("Invalid arguments, exiting abnormally", var3);
            log.info("Usage: ZooKeeperServerMain configfile | port datadir [ticktime] [maxcnxns]");
            System.err.println("Usage: ZooKeeperServerMain configfile | port datadir [ticktime] [maxcnxns]");
            ZKAuditProvider.addServerStartFailureAuditLog();
        } catch (FileTxnSnapLog.DatadirException var5) {
            log.error("Unable to access datadir, exiting abnormally", var5);
            System.err.println("Unable to access datadir, exiting abnormally");
            ZKAuditProvider.addServerStartFailureAuditLog();
        } catch (AdminServer.AdminServerException var6) {
            log.error("Unable to start AdminServer, exiting abnormally", var6);
            System.err.println("Unable to start AdminServer, exiting abnormally");
            ZKAuditProvider.addServerStartFailureAuditLog();
        } catch (Exception var7) {
            log.error("Unexpected exception, exiting abnormally", var7);
            ZKAuditProvider.addServerStartFailureAuditLog();
        }

        log.info("Exiting normally");
    }


}
