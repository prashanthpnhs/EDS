package org.endeavourhealth.ui.json;

import java.util.Date;

public class JsonServiceSystemStatus {
    private String systemName;
    private Date lastDataDate; //internal date of last data received
    private Date lastDataReceived; //when last data was received
    private boolean isProcessingUpToDate; //if last data received has been processed OK
    private boolean isProcessingInError; //if inbound processing is in error
    private Date lastDateSuccessfullyProcessed; //last time we processed anything successfully
    private Date lastDataDateSuccessfullyProcessed; //data datetime of last data we processed successfully
    private String publisherMode; //whether this interface is set to auto-fail or route to the bulk queues

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public Date getLastDataDate() {
        return lastDataDate;
    }

    public void setLastDataDate(Date lastDataDate) {
        this.lastDataDate = lastDataDate;
    }

    public Date getLastDataReceived() {
        return lastDataReceived;
    }

    public void setLastDataReceived(Date lastDataReceived) {
        this.lastDataReceived = lastDataReceived;
    }

    public boolean isProcessingUpToDate() {
        return isProcessingUpToDate;
    }

    public void setProcessingUpToDate(boolean processingUpToDate) {
        isProcessingUpToDate = processingUpToDate;
    }

    public boolean isProcessingInError() {
        return isProcessingInError;
    }

    public void setProcessingInError(boolean processingInError) {
        isProcessingInError = processingInError;
    }

    public Date getLastDateSuccessfullyProcessed() {
        return lastDateSuccessfullyProcessed;
    }

    public void setLastDateSuccessfullyProcessed(Date lastDateSuccessfullyProcessed) {
        this.lastDateSuccessfullyProcessed = lastDateSuccessfullyProcessed;
    }

    public Date getLastDataDateSuccessfullyProcessed() {
        return lastDataDateSuccessfullyProcessed;
    }

    public void setLastDataDateSuccessfullyProcessed(Date lastDataDateSuccessfullyProcessed) {
        this.lastDataDateSuccessfullyProcessed = lastDataDateSuccessfullyProcessed;
    }

    public String getPublisherMode() {
        return publisherMode;
    }

    public void setPublisherMode(String publisherMode) {
        this.publisherMode = publisherMode;
    }
}
