/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.sma.server.execution.executors;

import fr.insalyon.creatis.sma.common.bean.MessageOperation;
import fr.insalyon.creatis.sma.common.bean.OperationStatus;
import fr.insalyon.creatis.sma.server.business.BusinessException;
import fr.insalyon.creatis.sma.server.business.MessagePoolBusiness;
import fr.insalyon.creatis.sma.server.utils.Configuration;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageExecutor implements Callable<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(MessageExecutor.class);
    private final MessageOperation operation;
    private final MessagePoolBusiness poolBusiness;

    public MessageExecutor(MessageOperation operation, MessagePoolBusiness messagePoolBusiness) {
        this.operation = operation;
        this.poolBusiness = messagePoolBusiness;
    }

    @Override
    public Void call() throws Exception {
        LOG.info("[MessagePool] Processing operation '" + operation.getId() + "'.");

        try {
            poolBusiness.updateStatus(operation, OperationStatus.Running);
            sendEmail(
                    operation.getFromEmail(), 
                    operation.getFromName(), 
                    operation.getSubject(), 
                    operation.getContents(), 
                    operation.getRecipients(), 
                    operation.isDirect());
            poolBusiness.updateStatus(operation, OperationStatus.Done);

        } catch (BusinessException ex) {
            LOG.error("Error occured", ex);
            retry();
        }
        return null;
    }

    public void sendEmail(String ownerEmail, String owner, String subject,
            String content, String[] recipients, boolean direct) throws BusinessException {
        // see https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html 
        try {
            LOG.info("Sending email to: " + String.join(" ", recipients));
            Configuration conf = Configuration.getInstance();
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", conf.getMailProtocol());
            props.setProperty("mail.smtp.host", conf.getMailHost());
            props.setProperty("mail.smtp.port", String.valueOf(conf.getMailPort()));
            props.setProperty("mail.smtp.auth", String.valueOf(conf.isMailAuth()));
            props.setProperty("mail.smtp.starttls.enable", String.valueOf(conf.isMailAuth()));

            if (conf.isMailSslTrust()) {
                props.setProperty("mail.smtp.ssl.trust", conf.getMailHost());
            }
            
            Session session = Session.getDefaultInstance(props);
            session.setDebug(false);

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setContent(content, "text/html");
            mimeMessage.addHeader("Content-Type", "text/html");

            InternetAddress from = new InternetAddress(ownerEmail, owner);
            mimeMessage.setReplyTo(new InternetAddress[]{from});
            mimeMessage.setFrom(from);
            mimeMessage.setSentDate(new Date());
            mimeMessage.setSubject(subject);

            Transport transport = session.getTransport();

            if (conf.isMailAuth()) {
                transport.connect(
                    conf.getMailHost(), conf.getMailPort(),
                    conf.getMailUsername(), conf.getMailPassword());
            } else {
                transport.connect();
            }

            InternetAddress[] addressTo = null;

            if (recipients != null && recipients.length > 0) {
                addressTo = new InternetAddress[recipients.length];
                for (int i = 0; i < recipients.length; i++) {
                    addressTo[i] = new InternetAddress(recipients[i]);
                }
                if (direct) {
                    mimeMessage.setRecipients(Message.RecipientType.TO, addressTo);
                } else {
                    mimeMessage.setRecipients(Message.RecipientType.BCC, addressTo);
                }

                transport.sendMessage(mimeMessage, addressTo);
                transport.close();

            } else {
                LOG.warn("There's no recipients to send the email.");
            }
        } catch (UnsupportedEncodingException | MessagingException ex) {
            LOG.error("Error occured", ex);
            throw new BusinessException(ex);
        }
    }

    public void retry() throws BusinessException {
        if (operation.getRetryCount() == Configuration.getInstance().getMaxRetryCount()) {
            poolBusiness.updateStatus(operation, OperationStatus.Failed);
        } else {
            operation.incrementRetryCount();
            poolBusiness.updateStatus(operation, OperationStatus.Rescheduled);
        }
    }
}
