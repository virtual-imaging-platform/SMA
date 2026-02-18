package fr.insalyon.creatis.sma.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.insalyon.creatis.sma.common.Communication;
import fr.insalyon.creatis.sma.server.business.MessagePoolBusiness;
import fr.insalyon.creatis.sma.server.dao.MessagePoolDAO;
import fr.insalyon.creatis.sma.server.dao.h2.MessagePoolData;
import fr.insalyon.creatis.sma.server.execution.ScheduledTasksCreator;
import fr.insalyon.creatis.sma.server.execution.executors.CommunicationExecutor;
import fr.insalyon.creatis.sma.server.utils.Configuration;
import fr.insalyon.creatis.sma.server.utils.Constants;

public class SmaServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final ScheduledExecutorService tasksExecutor;
    private final ExecutorService socketExecutor;
    private final ExecutorService sendMessageExecutor;
    private final Configuration config;

    private final MessagePoolDAO messagePoolDAO;
    private final MessagePoolBusiness messagePoolBusiness;
    private boolean started = false;

    public SmaServer() {
        config = Configuration.getInstance();
        tasksExecutor = Executors.newSingleThreadScheduledExecutor();
        socketExecutor = Executors.newCachedThreadPool();
        sendMessageExecutor = Executors.newFixedThreadPool(config.getMailMaxRuns());

        messagePoolDAO = new MessagePoolData();
        messagePoolBusiness = new MessagePoolBusiness(messagePoolDAO);

        schedule();
    }

    public synchronized void waitToBeReady() throws InterruptedException {
        while (started == false) {
            Thread.sleep(1000);
        }
    }

    public final void schedule() {
        ScheduledTasksCreator creator = new ScheduledTasksCreator();

        tasksExecutor.scheduleWithFixedDelay(
            creator.getPoolCleanerTask(messagePoolDAO), 0, Constants.CLEANER_POOL_SLEEP_HOURS, TimeUnit.HOURS);
        tasksExecutor.scheduleWithFixedDelay(
            creator.getMessagePoolTask(sendMessageExecutor, messagePoolDAO, messagePoolBusiness), 0,  Constants.MESSAGE_POOL_SLEEP_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        LOG.info("Starting SMA Server on port " + config.getPort());

        try (ServerSocket serverSocket = new ServerSocket(config.getPort(), 50, InetAddress.getByName("0.0.0.0"))) {
            started = true;

            while (true) {
                Socket socket = serverSocket.accept();
                Communication communication = new Communication(socket);

                socketExecutor.submit(new CommunicationExecutor(communication, messagePoolBusiness));
            }
        } catch (IOException ex) {
            LOG.error("Error processing a request ", ex);
        }
    }
}
