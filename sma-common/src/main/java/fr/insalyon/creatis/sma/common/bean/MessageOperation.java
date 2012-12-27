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
package fr.insalyon.creatis.sma.common.bean;

import fr.insalyon.creatis.sma.common.Constants;
import java.util.Date;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class MessageOperation {

    private String id;
    private Date registration;
    private String fromEmail;
    private String fromName;
    private String subject;
    private String contents;
    private String[] recipients;
    private boolean direct;
    private OperationStatus status;
    private String username;
    private int retryCount;

    public MessageOperation(String fromEmail, String fromName, String subject, String contents, String recipients,
            boolean direct, String username) {

        this("mo-" + System.nanoTime(), new Date(), fromEmail, fromName, subject,
                contents, recipients, direct, OperationStatus.Queued, username, 0);
    }

    public MessageOperation(String id, Date registration, String fromEmail,
            String fromName, String subject, String contents, String recipients,
            boolean direct, OperationStatus status, String username, int retryCount) {

        this.id = id;
        this.registration = registration;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.subject = subject;
        this.contents = contents;
        this.recipients = recipients.split(Constants.MSG_SEP_2);
        this.direct = direct;
        this.status = status;
        this.username = username;
        this.retryCount = retryCount;
    }

    public String getId() {
        return id;
    }

    public Date getRegistration() {
        return registration;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public String getSubject() {
        return subject;
    }

    public String getContents() {
        return contents;
    }

    public String[] getRecipients() {
        return recipients;
    }

    public String getRecipientsAsString() {

        StringBuilder sb = new StringBuilder();
        for (String recipient : recipients) {
            if (sb.length() > 0) {
                sb.append(Constants.MSG_SEP_2);
            }
            sb.append(recipient);
        }
        return sb.toString();
    }

    public boolean isDirect() {
        return direct;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(OperationStatus status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public int getRetryCount() {
        return retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
}
