package messages;

import amazontools.AppConfig;

public abstract class AppMessage {
    protected String description;
    protected String body;
    protected String msgDelim= AppConfig.msgDelim;

    public String toString(){
        return description+msgDelim+ body;

    }
}
