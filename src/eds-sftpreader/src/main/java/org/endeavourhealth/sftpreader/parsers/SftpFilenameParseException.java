package org.endeavourhealth.sftpreader.parsers;

public class SftpFilenameParseException extends Exception
{
    public SftpFilenameParseException() {
        super();
    }
    public SftpFilenameParseException(String message) {
        super(message);
    }
    public SftpFilenameParseException(String message, Throwable cause) {
        super(message, cause);
    }
    public SftpFilenameParseException(Throwable cause) {
        super(cause);
    }
}
