package org.endeavourhealth.sftpreader.implementations;

import org.endeavourhealth.sftpreader.model.db.Batch;
import org.endeavourhealth.sftpreader.model.db.DbConfiguration;

public abstract class SftpNotificationCreator
{
    public abstract String createNotificationMessage(DbConfiguration dbConfiguration, Batch batch);
}
