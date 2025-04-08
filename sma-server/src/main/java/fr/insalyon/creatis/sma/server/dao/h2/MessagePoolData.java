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
package fr.insalyon.creatis.sma.server.dao.h2;

import fr.insalyon.creatis.sma.common.bean.MessageOperation;
import fr.insalyon.creatis.sma.common.bean.OperationStatus;
import fr.insalyon.creatis.sma.server.dao.DAOException;
import fr.insalyon.creatis.sma.server.dao.MessagePoolDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class MessagePoolData implements MessagePoolDAO {

    private final static Logger logger = Logger.getLogger(MessagePoolData.class);
    private Connection connection;

    public MessagePoolData(Connection connection) {
        this.connection = connection;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            DAOException.logException(e);
        }
    }

    @Override
    public void add(MessageOperation operation) throws DAOException {
        String query =  "INSERT INTO MessagePool(id, registration, fromEmail, fromName, subject, "
        +               "contents, recipients, direct, status, username, retrycount) "
        +               "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, operation.getId());
            ps.setTimestamp(2, new Timestamp(operation.getRegistration().getTime()));
            ps.setString(3, operation.getFromEmail());
            ps.setString(4, operation.getFromName());
            ps.setString(5, operation.getSubject());
            ps.setString(6, operation.getContents());
            ps.setString(7, operation.getRecipientsAsString());
            ps.setBoolean(8, operation.isDirect());
            ps.setString(9, operation.getStatus().name());
            ps.setString(10, operation.getUsername());
            ps.setInt(11, operation.getRetryCount());
            ps.executeUpdate();

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void update(MessageOperation operation) throws DAOException {
        String query =  "UPDATE MessagePool SET registration = ?, fromEmail = ?, fromName = ?, "
        +               "subject = ?, contents = ?, recipients = ?, direct = ?, "
        +               "status = ?, username = ?, retrycount = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, new Timestamp(operation.getRegistration().getTime()));
            ps.setString(2, operation.getFromEmail());
            ps.setString(3, operation.getFromName());
            ps.setString(4, operation.getSubject());
            ps.setString(5, operation.getContents());
            ps.setString(6, operation.getRecipientsAsString());
            ps.setBoolean(7, operation.isDirect());
            ps.setString(8, operation.getStatus().name());
            ps.setString(9, operation.getUsername());
            ps.setInt(10, operation.getRetryCount());
            ps.setString(11, operation.getId());
            ps.executeUpdate();

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public void remove(MessageOperation operation) throws DAOException {
        String query = "DELETE FROM MessagePool WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, operation.getId());
            ps.execute();

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public List<MessageOperation> getPendingOperations() throws DAOException {
        String query = getSelect() + "WHERE status = ? OR STATUS = ? ORDER BY registration";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, OperationStatus.Queued.name());
            ps.setString(2, OperationStatus.Rescheduled.name());

            ResultSet rs = ps.executeQuery();
            List<MessageOperation> operations = new ArrayList<MessageOperation>();
            while (rs.next()) {
                operations.add(getMessageOperation(rs));
            }
            return operations;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    @Override
    public List<MessageOperation> getOldOperations(Date date) throws DAOException {
        List<MessageOperation> operations = new ArrayList<MessageOperation>();
        String query =  getSelect() + "WHERE registration < ? AND (status = ? OR status = ?)"
        +               "ORDER BY registration";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, new Timestamp(date.getTime()));
            ps.setString(2, OperationStatus.Done.name());
            ps.setString(3, OperationStatus.Failed.name());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                operations.add(getMessageOperation(rs));
            }
            return operations;

        } catch (SQLException ex) {
            logger.error(ex);
            throw new DAOException(ex);
        }
    }

    private String getSelect() {
        return "SELECT id, registration, fromEmail, fromName, subject, contents, "
                + "recipients, direct, status, username, retrycount FROM MessagePool ";
    }

    private MessageOperation getMessageOperation(ResultSet rs) throws SQLException {
        return new MessageOperation(
                rs.getString("id"),
                new Date(rs.getTimestamp("registration").getTime()),
                rs.getString("fromEmail"),
                rs.getString("fromName"),
                rs.getString("subject"),
                rs.getString("contents"),
                rs.getString("recipients"),
                rs.getBoolean("direct"),
                OperationStatus.valueOf(rs.getString("status")),
                rs.getString("username"),
                rs.getInt("retrycount"));
    }
}
