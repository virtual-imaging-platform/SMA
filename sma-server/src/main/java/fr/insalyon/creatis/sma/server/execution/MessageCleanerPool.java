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
import fr.insalyon.creatis.sma.server.Configuration;
import fr.insalyon.creatis.sma.server.dao.DAOException;
import fr.insalyon.creatis.sma.server.dao.DAOFactory;
import fr.insalyon.creatis.sma.server.dao.MessagePoolDAO;
import java.util.Calendar;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class MessageCleanerPool extends Thread {

    private static final Logger logger = Logger.getLogger(MessagePool.class);
    private static MessageCleanerPool instance;
    private volatile boolean stop;
    private MessagePoolDAO messagePoolDAO;

    public static MessageCleanerPool getInstance() {

        if (instance == null) {
            instance = new MessageCleanerPool();
            instance.start();
        }
        return instance;
    }

    private MessageCleanerPool() {

        this.stop = false;
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -(Configuration.getInstance().getMaxHistory()));
                
                messagePoolDAO = DAOFactory.getDAOFactory().getMessagePoolDAO();
                
                for (MessageOperation operation : messagePoolDAO.getOldOperations(cal.getTime())) {
                    messagePoolDAO.remove(operation);
                    logger.info("Removed: " + operation.getRegistration() 
                            + ", FROM: " + operation.getFromName() 
                            + ", TO: " + operation.getRecipientsAsString());
                }
                
            } catch (DAOException ex) {
                // do nothing
            }

            try {
                sleep(86400000);
            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
        instance = null;
    }
}
