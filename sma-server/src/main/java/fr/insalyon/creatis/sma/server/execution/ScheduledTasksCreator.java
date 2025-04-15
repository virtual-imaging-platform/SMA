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

import org.apache.log4j.Logger;

public class ScheduledTasksCreator {

    private static final Logger LOG = Logger.getLogger(ScheduledTasksCreator.class);
    private final MessagePoolDAO messagePoolDAO;

    public ScheduledTasksCreator(MessagePoolDAO messagePoolDAO) {
        this.messagePoolDAO = messagePoolDAO;
    }

    public PoolCleaner getPoolCleanerTask() {
        return new PoolCleaner();
    }

    public MessagePool getMessagePoolTask(ExecutorService executorService) {
        return new MessagePool(executorService);
    }

    public class PoolCleaner implements Runnable {
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

    public class MessagePool implements Runnable {
        private final ExecutorService executor;

        public MessagePool(ExecutorService executorService) {
            this.executor = executorService;
        }

        @Override
        public void run() {
            try {
                final MessagePoolBusiness messagePoolBusiness = new MessagePoolBusiness(messagePoolDAO);

                List<MessageExecutor> callablesOperations = messagePoolDAO.getPendingOperations().stream()
                    .map(op -> new MessageExecutor(op, messagePoolBusiness)).toList();
    
                executor.invokeAll(callablesOperations, Constants.MESSAGE_POOL_MAX_WAIT_SECONDS, TimeUnit.SECONDS);
    
            } catch (DAOException | InterruptedException e) {
                LOG.warn("Failed to run Message Pool properly!", e);
            }
        }
    }
}
