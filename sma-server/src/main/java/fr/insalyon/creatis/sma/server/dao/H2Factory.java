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
package fr.insalyon.creatis.sma.server.dao;

import fr.insalyon.creatis.sma.server.dao.h2.MessagePoolData;
import fr.insalyon.creatis.sma.server.utils.Constants;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class H2Factory {

    private static final Logger logger = Logger.getLogger(H2Factory.class);
    private static H2Factory instance;
    private final String DBURL = "jdbc:h2:file:./db/sma.dbl";
    private JdbcConnectionPool connectionPool;

    public static H2Factory getInstance() {
        if (instance == null) {
            instance = new H2Factory();
        }
        return instance;
    }

    private H2Factory() {
        connectionPool = JdbcConnectionPool.create(DBURL, "sa", "");
        connectionPool.setMaxConnections(Constants.MAX_DB_CONNECTIONS);
        connectionPool.setLoginTimeout(Constants.TIMEOUT_POOL_SECONDS);

        createTables();
    }

    private void createTables() {
        try (Connection connection = getConnection()) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE MessagePool ("
                        + "id VARCHAR(100), "
                        + "registration TIMESTAMP, "
                        + "fromEmail VARCHAR(255), "
                        + "fromName VARCHAR(255), "
                        + "subject VARCHAR(255), "
                        + "contents CLOB, "
                        + "recipients CLOB, "
                        + "direct BOOLEAN, "
                        + "status VARCHAR(50), "
                        + "username VARCHAR(255), "
                        + "retrycount INT, "
                        + "PRIMARY KEY (id)"
                        + ")");
                st.executeUpdate("CREATE INDEX user_idx ON MessagePool(username)");
            }
        } catch (SQLException ex) {
            logger.info("Table MessagePool already exists!");
        }
    }

    private Connection getConnection() throws SQLException {
        Connection connection = connectionPool.getConnection();

        connection.setAutoCommit(true);
        return connection;
    }

    public MessagePoolDAO getMessagePoolDAO() throws DAOException {
        try {
            return new MessagePoolData(getConnection());
        } catch (SQLException e) {
            DAOException.logException(e);
            throw new DAOException(e);
        }
    }
}
