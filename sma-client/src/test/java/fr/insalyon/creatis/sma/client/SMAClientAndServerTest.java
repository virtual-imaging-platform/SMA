package fr.insalyon.creatis.sma.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import fr.insalyon.creatis.sma.server.SmaServer;
import fr.insalyon.creatis.sma.server.utils.Configuration;
import jakarta.mail.internet.MimeMessage;

@TestInstance(Lifecycle.PER_CLASS)
public class SMAClientAndServerTest {

    @Mock
    private Configuration   configuration;

    private SMAClient       client;
    private GreenMail       mailServer;
    private SmaServer       smaServer;

    private void mockConfig(SmtpServer server) {
        configuration = Mockito.mock(Configuration.class);
    
        when(configuration.getMailHost()).thenReturn(server.getBindTo());
        when(configuration.getMailProtocol()).thenReturn(server.getProtocol());
        when(configuration.getPort()).thenReturn(8082);
        when(configuration.getMaxHistory()).thenReturn(90);
        when(configuration.getMaxRetryCount()).thenReturn(5);
        when(configuration.getMailFrom()).thenReturn("test@test.com");
        when(configuration.getMailFromName()).thenReturn("test");
        when(configuration.getMailMaxRuns()).thenReturn(10);
        when(configuration.getMailPort()).thenReturn(server.getPort());

        Configuration.getInstance().setConfiguration(configuration);
    }
    
    @BeforeAll
    public void initServer() throws InterruptedException {
        mailServer = new GreenMail(ServerSetupTest.SMTP);
        mailServer.start();
        mockConfig(mailServer.getSmtp());
        
        smaServer = new SmaServer();
        smaServer.start();
        smaServer.waitToBeReady();
    }

    @AfterAll
    public void stopServer() {
        smaServer.interrupt();
        mailServer.stop();
    }

    @Test
    public void spamMailClient() throws Exception {
        Instant before, after;
        final String message = "je suis vraiment trop fort";
        final String subject = "wow ce titre est incroyable";
        final String username = "bliblou";
        final int nmails = 1000;
        String[] randomAddress;
        client = new SMAClient(InetAddress.getLocalHost(), configuration.getPort());

        for (int i = 0; i < nmails; i++) {
            randomAddress = generateRandomAddress();
            client.sendEmail(subject, message, randomAddress, true, username);
            System.err.println("Sending mail to : " + randomAddress[0]);
        }

        before = Instant.now();
        System.err.println("Waiting for mails to reach de server !");
        while (mailServer.getReceivedMessages().length != nmails) {
            System.err.println("Waiting ! (" + mailServer.getReceivedMessages().length + " mails arrived)");
            Thread.sleep(1000);
            after = Instant.now();
            assertFalse(Duration.between(before, after).toSeconds() > Constants.MAX_WAIT_SPAM_SEC);
        }
        System.err.println("All mails sended !");

        for (final MimeMessage mail : mailServer.getReceivedMessages()) {
            System.err.println("Checking mail to " + mail.getAllRecipients()[0].toString());
            assertEquals(mail.getSubject(), subject);
            assertEquals(mail.getContent(), message);
        }
    }

    private String[] generateRandomAddress() {
        return new String[] { UUID.randomUUID().toString() + "@insa.fr" };
    }
}
