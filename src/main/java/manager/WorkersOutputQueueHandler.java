package manager;

import amazontools.AppConfig;
import com.amazonaws.services.sqs.model.Message;
import messages.MsgType;

import java.util.List;

public class WorkersOutputQueueHandler implements  Runnable{
    private Data data;

    public WorkersOutputQueueHandler(Data data){
        this.data=data;
    }

    /**
     * Reads messages from the out workers queue and delete it. This class get terminated by the manager after
     * all the workers finished their tasks and there are no more tasks to execute.
     * possible msgs:
     * TERMINATE- the worker terminated itself.
     * DONEOCRTASK- the worker succeed to complete the task (applied the ocr algorithm on the image)
     * DONEOCRTASKFAIL- the worker failed to complete the task (failed to download the image / failed to apply the ocr algorithm)
     * NOTE- if the worker stopped working due to other reason than mentioned above,
     * it is not consider as failiure - another worker should get his task.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            List<Message> messages = data.getSqs().recieveMessage(data.getWorkersOutQueueUrl(), 1, AppConfig.visibillityTime);
            for (Message message : messages) {
                MsgType msgType = AppConfig.getMsgType(message);
                String[] msgData = message.getBody().split(AppConfig.msgDelim);

               /* if (msgType == MsgType.TERMINATE) {
                    data.getActiveWorkers().decrementAndGet();
                    System.out.println("Worker terminated, there are  " + data.getActiveWorkers().get() + " active workers left.");
                }*/

                if (msgType == MsgType.DONEOCRTASK) { //worker finished successfully - doneOcrTask
                    //example: doneOcrTask_--_243localAppQueue243533_--_https://imageurlhere.com_--_imageText here
                    System.out.println("message from worker: " + message.getBody() + " task succeed");
                    //get the job who sent the task.
                    Job job = data.getJobs().get(msgData[1]);
                    //update the results
                    String imageUrl= msgData[2];
                    String imageText=msgData[3];
                    job.getResultMap().put(imageUrl,imageText);
                    //increment number of completed tasks.
                    job.getNumOfCompletedTasks().addAndGet(1);
                    //now the job checks if it is done in its own loop.
                }

                else if (msgType == MsgType.DONEOCRTASKFAIL) { //worker did not succeed to process the image.
                    System.out.println("message from worker: " + message.getBody() + " task failed.");
                    //get the job who sent the task.
                    Job job = data.getJobs().get(msgData[1]);
                    //update the results
                    String imageUrl= msgData[2];
                    String imageText=msgData[3];
                    job.getResultMap().put(imageUrl,imageText);
                    //increment number of failed tasks.
                    job.getNumOfFailedTasks().addAndGet(1);
                    //now the job checks if it is done in his own loop.
                }
                data.getSqs().deleteMessage(data.getWorkersOutQueueUrl(), message);
            }
        }
    }
}
