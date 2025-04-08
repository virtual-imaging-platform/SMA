package fr.insalyon.creatis.sma.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fr.insalyon.creatis.sma.common.Communication;
import fr.insalyon.creatis.sma.server.execution.ScheduledTasks;
import fr.insalyon.creatis.sma.server.execution.executors.CommunicationExecutor;
import fr.insalyon.creatis.sma.server.utils.Configuration;
import fr.insalyon.creatis.sma.server.utils.Constants;

public class SmaServer extends Thread {

    private static final Logger logger = Logger.getLogger(Main.class);

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService fastExecutor;
    private final ExecutorService longExecutor;
    private final ScheduledTasks tasks;
    private final Configuration config;
    private boolean started = false;

    public SmaServer() {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResource("smaLog4j.properties"));

        config = Configuration.getInstance();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        fastExecutor = Executors.newCachedThreadPool();
        longExecutor = Executors.newFixedThreadPool(config.getMailMaxRuns());

        tasks = new ScheduledTasks(longExecutor);

        schedule();
    }

    public synchronized void waitToBeReady() throws InterruptedException {
        while (started == false) {
            Thread.sleep(1000);
        }
    }

    public void schedule() {
        scheduledExecutorService.scheduleWithFixedDelay(
            () -> tasks.messagePoolCleaner(), 0, Constants.CLEANER_POOL_SLEEP_HOURS, TimeUnit.HOURS);
        scheduledExecutorService.scheduleWithFixedDelay(
            () -> tasks.messagePool(), 0, Constants.MESSAGE_POOL_SLEEP_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        logger.info("Starting SMA Server on port " + config.getPort());

        try (ServerSocket serverSocket = new ServerSocket(config.getPort(), 50, InetAddress.getByName("0.0.0.0"))) {
            started = true;

            while (true) {
                Socket socket = serverSocket.accept();
                Communication communication = new Communication(socket);

                fastExecutor.submit(new CommunicationExecutor(communication));
            }
        } catch (IOException ex) {
            logger.error("Error processing a request ", ex);
        }
    }
}