/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
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
package fr.insalyon.creatis.sma.server.business;

import fr.insalyon.creatis.sma.common.bean.MessageOperation;
import fr.insalyon.creatis.sma.server.Configuration;
import fr.insalyon.creatis.sma.server.dao.DAOException;
import fr.insalyon.creatis.sma.server.dao.DAOFactory;
import fr.insalyon.creatis.sma.server.execution.MessagePool;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class MessagePoolBusiness {

    private final static Logger logger = Logger.getLogger(MessagePoolBusiness.class);

    /**
     *
     * @param operation
     * @throws BusinessException
     */
    public void addOperation(MessageOperation operation) throws BusinessException {

        try {
            DAOFactory.getDAOFactory().getMessagePoolDAO().add(operation);
            MessagePool.getInstance();

        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    public void sendEmail(String ownerEmail, String owner, String subject,
            String content, String[] recipients, boolean direct) throws BusinessException {
        // see https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html 
        try {
            logger.info("Sending email to: " + String.join(" ", recipients));
            Configuration conf = Configuration.getInstance();
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", conf.getMailProtocol());
            props.setProperty("mail.smtp.host", conf.getMailHost());
            props.setProperty("mail.smtp.port", String.valueOf(conf.getMailPort()));
            props.setProperty("mail.smtp.auth", String.valueOf(conf.isMailAuth()));
            props.setProperty("mail.smtp.starttls.enable", String.valueOf(conf.isMailAuth()));
            props.setProperty("mail.smtp.ssl.trust", conf.getMailSslTrust());
            
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
                logger.warn("There's no recipients to send the email.");
            }
        } catch (UnsupportedEncodingException | MessagingException ex) {
            logger.error(ex);
            throw new BusinessException(ex);
        }
    }
}
