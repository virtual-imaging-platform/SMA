package fr.insalyon.creatis.sma.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fr.insalyon.creatis.sma.common.Communication;
import fr.insalyon.creatis.sma.server.execution.Executor;
import fr.insalyon.creatis.sma.server.execution.MessageCleanerPool;

/**
 * SmaServer
 */
public class SmaServer extends Thread {

    private static final Logger logger = Logger.getLogger(Main.class);

    private boolean started = false;

    public synchronized void waitToBeReady() throws InterruptedException {
        while (started == false) {
            Thread.sleep(1000);
        }
    }
    @Override
    public void run() {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResource("smaLog4j.properties"));
        Configuration.getInstance();

        logger.info("Starting SMA Server on port " + Configuration.getInstance().getPort());

        // Pools
        MessageCleanerPool.getInstance();
        
        // Socket
        try (ServerSocket serverSocket = new ServerSocket(
            Configuration.getInstance().getPort(), 50, InetAddress.getByName("0.0.0.0"))) {
                
            started = true;
            while (true) {
                Socket socket = serverSocket.accept();
                Communication communication = new Communication(socket);
                new Executor(communication).start();
            }

        } catch (IOException ex) {
            logger.error("Error processing a request ", ex);
        }
    }
}