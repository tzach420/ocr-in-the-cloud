package messages;

/**
 * this message is sent from the manager to a local app once the manager finished to work on the file.
 * this msg contains the bucket name and url of the output file.
 *example: doneTask-bucketname243533-outputfilekey
 */
public class DoneTaskMessage extends AppMessage {

    public DoneTaskMessage(String bucketName, String outputFileKey){
        this.description="doneTask";
        this.body=bucketName+msgDelim+outputFileKey;

    }
}
