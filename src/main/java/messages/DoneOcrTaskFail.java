package messages;

/**
 * this message is sent from a worker to the manager once the worker failed to finish to work on single line from the file.
 * this msg contains the localAppId and the url of an image
 *example: doneOcrTaskFail_--_243localAppQueue243533_--_https://imageurlhere.com_--_ cannot open the url
 */
public class DoneOcrTaskFail extends AppMessage{
    public DoneOcrTaskFail(String localAppSqsId, String imageUrl,String exception){
        this.description="doneOcrTaskFail";
        this.body=localAppSqsId+msgDelim+imageUrl+msgDelim+exception;

    }
}
