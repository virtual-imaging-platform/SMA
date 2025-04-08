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
import fr.insalyon.creatis.sma.server.dao.DAOException;
import fr.insalyon.creatis.sma.server.dao.H2Factory;
import fr.insalyon.creatis.sma.server.dao.MessagePoolDAO;
import fr.insalyon.creatis.sma.server.utils.Configuration;

import org.apache.log4j.Logger;

public class MessageExecutor {

    private static final Logger logger = Logger.getLogger(MessageExecutor.class);
    private final MessageOperation operation;
    private MessagePoolDAO poolDAO;

    public MessageExecutor(MessageOperation operation) {
        this.operation = operation;
    }

    public void send() {
        logger.info("[MessagePool] Processing operation '" + operation.getId() + "'.");

        try {
            poolDAO = H2Factory.getInstance().getMessagePoolDAO();

            updateStatus(operation, OperationStatus.Running);
            new MessagePoolBusiness().sendEmail(
                    operation.getFromEmail(), 
                    operation.getFromName(), 
                    operation.getSubject(), 
                    operation.getContents(), 
                    operation.getRecipients(), 
                    operation.isDirect());
            updateStatus(operation, OperationStatus.Done);

        } catch (DAOException | BusinessException ex) {
            logger.error(ex);
            retry();
        } finally {
            poolDAO.close();
        }
    }

    public void retry() {
        try {
            if (operation.getRetryCount() == Configuration.getInstance().getMaxRetryCount()) {
                updateStatus(operation, OperationStatus.Failed);
            } else {
                operation.incrementRetryCount();
                updateStatus(operation, OperationStatus.Rescheduled);
            }
        } catch (DAOException ex) {
            logger.warn("Failed to retry message task !");
        }
    }

    private void updateStatus(MessageOperation operation, OperationStatus status) throws DAOException {
        if (poolDAO != null) {
            operation.setStatus(status);
            poolDAO.update(operation);
        }
    }
}
