package org.endeavourhealth.sftpreader.implementations;

import org.apache.commons.lang3.Validate;
import org.endeavourhealth.sftpreader.model.db.DbConfiguration;
import org.endeavourhealth.sftpreader.model.exceptions.SftpFilenameParseException;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SftpFilenameParser
{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SftpFilenameParser.class);

    private boolean isFilenameValid = false;
    protected DbConfiguration dbConfiguration;
    private String pgpFileExtensionFilter;

    public SftpFilenameParser(String filename, DbConfiguration dbConfiguration)
    {
        Validate.notNull(dbConfiguration, "dbConfiguration is null");

        this.dbConfiguration = dbConfiguration;
        this.pgpFileExtensionFilter = dbConfiguration.getPgpFileExtensionFilter();

        try
        {
            parseFilename(filename, dbConfiguration.getPgpFileExtensionFilter());

            if (!dbConfiguration.getInterfaceFileTypes().contains(generateFileTypeIdentifier()))
                throw new SftpFilenameParseException("File type " + generateFileTypeIdentifier() + " not recognised");

            isFilenameValid = true;
        }
        catch (Exception e)
        {
            isFilenameValid = false;
            LOG.error("Error '" + e.getMessage() + "' parsing filename: " + filename);
        }
    }

    protected abstract void parseFilename(String filename, String pgpFileExtensionFilter) throws SftpFilenameParseException;
    protected abstract String generateBatchIdentifier();
    protected abstract String generateFileTypeIdentifier();

    public boolean isFilenameValid()
    {
        return this.isFilenameValid;
    }

    public String getBatchIdentifier()
    {
        if (!isFilenameValid)
            return "UNKNOWN";

        return generateBatchIdentifier();
    }

    public String getFileTypeIdentifier()
    {
        if (!isFilenameValid)
            return "UNKNOWN";

        return generateFileTypeIdentifier();
    }

    public String getPgpFileExtensionFilter()
    {
        return this.pgpFileExtensionFilter;
    }
}
