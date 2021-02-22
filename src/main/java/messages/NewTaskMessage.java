package messages;

/**
 * new task is send from the localApp to the manager containing the following information:
 * description- let the manager know its new task message.
 * body- containing the localApp presonal queue,the bucket name and the file key.
 * example: "newTask-localAppQueue123454-dsp1Bucket1234511-c_programfiles_data.txt"
 */
public class NewTaskMessage extends AppMessage {

    public NewTaskMessage(String localQueueName, String bucketName, String fileKey){
        this.description="newTask";
        this.body=localQueueName+msgDelim+bucketName+msgDelim+fileKey;

    }
}
