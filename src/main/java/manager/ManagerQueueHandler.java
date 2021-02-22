package manager;

import amazontools.AppConfig;
import messages.MsgType;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;


public class ManagerQueueHandler implements  Runnable {
        Data data;

    public ManagerQueueHandler(Data data){
        this.data=data;
    }

    /**
     * Reads messages from the managerQueue - from local apps.
     * msg types:
     * NEWTASK - local app sent manager new task to execute.
     * TERMINATE- local app tells the manager to terminate itself.
     */
    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            List<Message> messages = data.getSqs().recieveMessage(data.getManagerQueueUrl(),1, AppConfig.visibillityTime);
            for (Message message : messages) {
                System.out.println("ManagerQueueHandler received message with body: " + message.getBody());
                MsgType msgType= AppConfig.getMsgType(message);
                String msgContent=message.getBody();
                String[] attr = msgContent.split(AppConfig.msgDelim);

                if(msgType==MsgType.TERMINATE){
                    //save the local app sqs in order to send it terminate msg later.
                    data.setLocalAppThatSentTerminateId(attr[1]);
                    data.getSqs().deleteMessage(data.getManagerQueueUrl(),message);
                    data.setTerminate(true);
                    Thread.currentThread().interrupt();
                    break;
                }

                else if(msgType==MsgType.NEWTASK){
                    //* example: "newTask-localAppQueue123454-dsp1Bucket1234511-c_programfiles_data.txt"
                    String localAppQueueUrl= attr[1];
                    String bucketName= attr[2];
                    String fileKey=attr[3];
                    //define new job (new doc)
                    Job job = new Job(localAppQueueUrl,bucketName,fileKey,data);
                    //assign job and add it to the thread pool.
                    data.getJobs().putIfAbsent(localAppQueueUrl,job);
                    Thread jobThread= new Thread(job);
                    data.getJobsPool().execute(jobThread);
                    //delete the msg from the manager queue.
                    data.getSqs().deleteMessage(data.getManagerQueueUrl(),message);

                }

            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

    }
}


