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
package fr.insalyon.creatis.sma.server;

import java.io.File;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class Configuration {

    private static final Logger logger = Logger.getLogger(Configuration.class);
    private static Configuration instance;
    private static final String confFile = "sma-server.conf";
    // General
    private int port;
    private int maxHistory;
    private int maxRetryCount;
    private int mailPort;
    private boolean mailAuth;
    private String mailHost;
    private String mailSslTrust;
    private String mailUsername;
    private String mailPassword;
    private String mailProtocol;
    private String mailFrom;
    private String mailFromName;
    private int mailMaxRuns;

    public static Configuration getInstance() {

        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public void setConfiguration(Configuration config) {
        instance = config;
    }

    private Configuration() {
        try {
            logger.info("Loading configuration file.");
            PropertiesConfiguration config = new PropertiesConfiguration(new File(confFile));

            port = config.getInt(Constants.LAB_AGENT_PORT, 8082);
            mailAuth = config.getBoolean(Constants.LAB_MAIL_AUTH, false);
            mailUsername = config.getString(Constants.LAB_MAIL_USERNAME, "default");
            mailPassword = config.getString(Constants.LAB_MAIL_PASSWORD, "password");
            maxHistory = config.getInt(Constants.LAB_AGENT_MAX_HISTORY, 90);
            maxRetryCount = config.getInt(Constants.LAB_AGENT_RETRYCOUNT, 5);
            mailHost = config.getString(Constants.LAB_MAIL_HOST, "smtp.localhost");
            mailSslTrust = config.getString(Constants.LAB_MAIL_SSL_TRUST, "");
            mailPort = config.getInt(Constants.LAB_MAIL_PORT, 25);
            mailProtocol = config.getString(Constants.LAB_MAIL_PROTOCOL, "smtp");
            mailFrom = config.getString(Constants.LAB_MAIL_FROM, "example@example.com");
            mailFromName = config.getString(Constants.LAB_MAIL_FROM_NAME, "Example");
            mailMaxRuns = config.getInt(Constants.LAB_MAIL_MAX_RUNS, 5);

            config.setProperty(Constants.LAB_AGENT_PORT, port);
            config.setProperty(Constants.LAB_AGENT_RETRYCOUNT, maxRetryCount);
            config.setProperty(Constants.LAB_AGENT_MAX_HISTORY, maxHistory);

            config.setProperty(Constants.LAB_MAIL_PROTOCOL, mailProtocol);
            config.setProperty(Constants.LAB_MAIL_HOST, mailHost);
            config.setProperty(Constants.LAB_MAIL_PORT, mailPort);
            config.setProperty(Constants.LAB_MAIL_SSL_TRUST, mailSslTrust);

            config.setProperty(Constants.LAB_MAIL_AUTH, mailAuth);
            config.setProperty(Constants.LAB_MAIL_USERNAME, mailUsername);
            config.setProperty(Constants.LAB_MAIL_PASSWORD, mailPassword);

            config.setProperty(Constants.LAB_MAIL_FROM, mailFrom);
            config.setProperty(Constants.LAB_MAIL_FROM_NAME, mailFromName);
            config.setProperty(Constants.LAB_MAIL_MAX_RUNS, mailMaxRuns);

            config.save();

        } catch (ConfigurationException ex) {
            logger.error(ex);
        }
    }

    public int getPort() {
        return port;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public boolean isMailAuth() {
        return mailAuth;
    }

    public String getMailHost() {
        return mailHost;
    }

    public String getMailSslTrust() {
        return mailSslTrust;
    }

    public String getMailUsername() {
        return mailUsername;
    }

    public String getMailPassword() {
        return mailPassword;
    }

    public String getMailProtocol() {
        return mailProtocol;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public int getMailPort() {
        return mailPort;
    }

    public int getMailMaxRuns() {
        return mailMaxRuns;
    }
}
