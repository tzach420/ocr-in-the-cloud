package messages;

/**
 * this msg is sent in 2 cases:
 * 1. we got "terminate" argument from the user and the local app need to terminate the manger.
 * 2. the manager need to terminate a worker.
 * args: the sender sqs.
 * example: terminate 2434localApp12334
 */
public class TerminateMessage extends AppMessage{

    public TerminateMessage(String senderSqsUrl){
        this.description="terminate";
        this.body=senderSqsUrl;
    }
}
