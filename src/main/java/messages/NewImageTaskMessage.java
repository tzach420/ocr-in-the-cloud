package messages;

/**
 * this msg is sent from the managerQueueHandler to the workerQueue.
 * msg contains description, localAppSqsId and a link for worker to download and apply on it his algorithm.
 * example: newImageTask_12324localAPP3534_https://blabla.com/downloadfromhere
 */
public class NewImageTaskMessage extends AppMessage {

    public NewImageTaskMessage(String localAppSqs,String link){
        this.description="newImageTask";
        this.body=localAppSqs+msgDelim+link;
    }
}
