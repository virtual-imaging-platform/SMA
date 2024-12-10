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
package fr.insalyon.creatis.sma.client;

import fr.insalyon.creatis.sma.common.Communication;
import fr.insalyon.creatis.sma.common.Constants;
import fr.insalyon.creatis.sma.common.ExecutorConstants;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class SMAClient {

    private InetAddress host;
    private int         port;

    public SMAClient(String host, int port) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
    }

    public SMAClient(InetAddress host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     *
     * @param subject
     * @param contents
     * @param recipients
     * @param direct
     * @param username
     * @throws SMAClientException
     */
    public String sendEmail(String subject, String contents, String[] recipients,
            boolean direct, String username) throws SMAClientException {

        try {
            StringBuilder sb = new StringBuilder();
            for (String recipient : recipients) {
                if (sb.length() > 0) {
                    sb.append(Constants.MSG_SEP_2);
                }
                sb.append(recipient);
            }
            
            Communication communication = getCommunication();
            communication.sendMessage(
                    ExecutorConstants.MESSAGEPOOL_ADD_OPERATION + Constants.MSG_SEP_1
                    + subject + Constants.MSG_SEP_1
                    + contents + Constants.MSG_SEP_1
                    + sb.toString() + Constants.MSG_SEP_1
                    + direct + Constants.MSG_SEP_1
                    + username);
            communication.sendEndOfMessage();

            String operationID = communication.getMessage();
            communication.close();
            return operationID;

        } catch (IOException ex) {
            throw new SMAClientException(ex);
        }
    }

    private Communication getCommunication() throws SMAClientException {

        try {
            return new Communication(new Socket(host, port));

        } catch (UnknownHostException ex) {
            throw new SMAClientException(ex);
        } catch (IOException ex) {
            throw new SMAClientException(ex);
        }
    }
}
