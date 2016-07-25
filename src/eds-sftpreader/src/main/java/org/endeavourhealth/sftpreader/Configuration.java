package org.endeavourhealth.sftpreader;

import ch.qos.logback.classic.db.DBAppender;
import ch.qos.logback.core.db.DataSourceConnectionSource;
import com.mchange.v2.c3p0.DataSources;
import org.endeavourhealth.core.configuration.DatabaseConnection;
import org.endeavourhealth.core.configuration.SftpReaderConfiguration;
import org.endeavourhealth.core.utility.XmlSerializer;
import org.endeavourhealth.sftpreader.dbModel.DbConfiguration;
import org.endeavourhealth.sftpreader.utilities.postgres.PgStoredProcException;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class Configuration
{
    // class members //
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final String CONFIG_XSD = "SftpReaderConfiguration.xsd";
    private static final String CONFIG_RESOURCE = "SftpReaderConfiguration.xml";

    private static Configuration instance = null;

    public static Configuration getInstance() throws Exception
    {
        if (instance == null)
            instance = new Configuration();

        return instance;
    }

    // instance members //
    private SftpReaderConfiguration localConfiguration;
    private DbConfiguration dbConfiguration;

    private Configuration() throws Exception
    {
        loadLocalConfiguration();
        registerLogbackDbAppender();
        loadDbConfiguration();
    }

    private void loadLocalConfiguration() throws Exception
    {
        LOG.info("Loading local configuration file from resource " + CONFIG_RESOURCE);
        localConfiguration = XmlSerializer.deserializeFromResource(SftpReaderConfiguration.class, CONFIG_RESOURCE, CONFIG_XSD);
    }

    private void loadDbConfiguration() throws PgStoredProcException, SQLException
    {
        DataLayer dataLayer = new DataLayer(getDataSource(localConfiguration.getDatabaseConnections().getSftpReader()));
        this.dbConfiguration = dataLayer.getConfiguration(getInstanceId());
    }

    private void registerLogbackDbAppender() throws SQLException
    {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        DataSourceConnectionSource dataSourceConnectionSource = new DataSourceConnectionSource();
        dataSourceConnectionSource.setDataSource(getDataSource(localConfiguration.getDatabaseConnections().getLogback()));
        dataSourceConnectionSource.setContext(rootLogger.getLoggerContext());
        dataSourceConnectionSource.start();

        DBAppender dbAppender = new DBAppender();
        dbAppender.setConnectionSource(dataSourceConnectionSource);
        dbAppender.setContext(rootLogger.getLoggerContext());
        dbAppender.setName("PostgresDbAppender");
        dbAppender.start();

        rootLogger.addAppender(dbAppender);
    }

    private static DataSource getDataSource(DatabaseConnection databaseConnection) throws SQLException
    {
        PGSimpleDataSource pgSimpleDataSource = new PGSimpleDataSource();
        pgSimpleDataSource.setServerName(databaseConnection.getHostname());
        pgSimpleDataSource.setPortNumber(databaseConnection.getPort().intValue());
        pgSimpleDataSource.setDatabaseName(databaseConnection.getDatabase());
        pgSimpleDataSource.setUser(databaseConnection.getUsername());
        pgSimpleDataSource.setPassword(databaseConnection.getPassword());
        return DataSources.pooledDataSource(pgSimpleDataSource);
    }

    public String getInstanceId()
    {
        return localConfiguration.getInstanceId();
    }

    public SftpReaderConfiguration getLocalConfiguration()
    {
        return this.localConfiguration;
    }

    public DbConfiguration getDbConfiguration()
    {
        return this.dbConfiguration;
    }
}
