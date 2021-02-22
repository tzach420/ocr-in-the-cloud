package messages;

/**
 * this message is sent from a worker to the manager once the worker finished to work on single line from the file successfully.
 * this msg contains the localAppId  url of an image and image text.
 *example: doneOcrTask_--_243localAppQueue243533_--_https://imageurlhere.com_--_imageText here
 */
public class DoneOcrTask extends AppMessage {

    public DoneOcrTask(String localAppSqsId, String imageUrl, String imageText){
        this.description="doneOcrTask";
        this.body=localAppSqsId+msgDelim+imageUrl+msgDelim+imageText;

    }
}
