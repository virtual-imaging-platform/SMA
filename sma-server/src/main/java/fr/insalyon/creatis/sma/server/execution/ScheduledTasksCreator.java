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
package fr.insalyon.creatis.sma.server.execution;

import fr.insalyon.creatis.sma.common.bean.MessageOperation;
import fr.insalyon.creatis.sma.server.business.MessagePoolBusiness;
import fr.insalyon.creatis.sma.server.dao.DAOException;
import fr.insalyon.creatis.sma.server.dao.MessagePoolDAO;
import fr.insalyon.creatis.sma.server.execution.executors.MessageExecutor;
import fr.insalyon.creatis.sma.server.utils.Configuration;
import fr.insalyon.creatis.sma.server.utils.Constants;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledTasksCreator {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasksCreator.class);

    public PoolCleaner getPoolCleanerTask(MessagePoolDAO messagePoolDAO) {
        return new PoolCleaner(messagePoolDAO);
    }

    public MessagePool getMessagePoolTask(ExecutorService executorService, MessagePoolDAO messagePoolDAO, MessagePoolBusiness messagePoolBusiness) {
        return new MessagePool(executorService, messagePoolDAO, messagePoolBusiness);
    }

    public static class PoolCleaner implements Runnable {
        private final MessagePoolDAO messagePoolDAO;

        public PoolCleaner(MessagePoolDAO messagePoolDAO) {
            this.messagePoolDAO = messagePoolDAO;
        }

        @Override
        public void run() {
            LOG.info("Running Message Cleaner Pool");

            try {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, - (Configuration.getInstance().getMaxHistory()));
                
                for (MessageOperation operation : messagePoolDAO.getOldOperations(cal.getTime())) {
                    messagePoolDAO.remove(operation);
                    LOG.info("Removed: " + operation.getRegistration() 
                            + ", FROM: " + operation.getFromName() 
                            + ", TO: " + operation.getRecipientsAsString());
                }
    
            } catch (DAOException e) {
                LOG.warn("Failed to run Message Cleaner Pool properly!", e);
            } finally {
                LOG.info("Finishing Message Cleaner Pool");
            }
        }
    }

    public static class MessagePool implements Runnable {
        private final ExecutorService executor;
        private final MessagePoolDAO messagePoolDAO;
        private final MessagePoolBusiness messagePoolBusiness;

        public MessagePool(ExecutorService executorService, MessagePoolDAO messagePoolDAO, MessagePoolBusiness messagePoolBusiness) {
            this.executor = executorService;
            this.messagePoolDAO = messagePoolDAO;
            this.messagePoolBusiness = messagePoolBusiness;
        }

        @Override
        public void run() {
            try {
                List<MessageExecutor> callablesOperations = messagePoolDAO.getPendingOperations().stream()
                    .map(op -> new MessageExecutor(op, messagePoolBusiness)).toList();
    
                executor.invokeAll(callablesOperations, Constants.MESSAGE_POOL_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
    
            } catch (DAOException | InterruptedException e) {
                LOG.warn("Failed to run Message Pool properly!", e);
            }
        }
    }
}
