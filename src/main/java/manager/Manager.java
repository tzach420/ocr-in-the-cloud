package manager;
import amazontools.SQSHandler;
import com.amazonaws.services.ec2.model.Instance;
import messages.TerminateMessage;

import java.util.List;

public class Manager {

    Data data;
    private int second=1000;
    private Thread workersThread;
    private Thread managerThread;

    public Manager(String managerQueue,int n,int numOfThreads){
       data= new Data(managerQueue,n,numOfThreads);
    }
    
    public void start(){
        //init thread that handles reading from  the manager queue and sending jobs to workers.
        ManagerQueueHandler managerQhandler = new ManagerQueueHandler(data);
        managerThread = new Thread(managerQhandler);
        managerThread.start();

        //init thread that handles reading from  the workers out queue and update data.
        WorkersOutputQueueHandler workersQueueHandler = new WorkersOutputQueueHandler(data);
        workersThread = new Thread(workersQueueHandler);
        workersThread.start();

        //wait
        while (!data.getTerminate().get());

        //termination
        handleTermination();
    }



    /**
     * Handle the termination process- applied on recieving terminaion msg from localApp.
     * should send termination msg to all the workers.
     * close all the
     *
     */
    public void handleTermination(){
        waitForAllJobsToFinish();
        //waitForAllWorkersToFinish();
        killAllTheWorkers();
        deleteAllSqsExcept(data.getLocalAppThatSentTerminateId());
        //delete all buckets and their content.
        System.out.println("===========deleting buckets===============");
        data.getS3().deleteAllBuckets();
        System.out.println("finished deleting all buckets, queues and workers. now exiting");
        //send terminate msg to the local app.
        data.getSqs().sendMessage(data.getLocalAppThatSentTerminateId(), new TerminateMessage(data.getManagerQueueUrl()).toString());
        System.exit(0);


    }

    public void waitForAllJobsToFinish(){
        System.out.println("Waiting for all jobs to finish...");
        while(!data.getJobs().isEmpty()){
            try {
                Thread.sleep(5*second);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @pre- all the jobs are done so there are no active workers.
     */
    public void killAllTheWorkers(){
        System.out.println("===========Killing Workers===============");
        for (String worker : data.getWorkers()) {
            System.out.println("killing worker instance: " + worker);
            data.getEc2().terminatedInstance(worker);
        }
    }

    /**
     * deletes all the sqs except from the sqsName sqs.
     * @param sqsName- the name of the sqs we wish not to remove.
     */
    public void deleteAllSqsExcept(String sqsName){
        System.out.println("===========deleting queues===============");
        SQSHandler sqs = data.getSqs();
        List<String> sqsList = sqs.getQueueList();
        for (String sqsUrl : sqsList) {
            if (!sqsUrl.contains(sqsName)) {
                sqs.deleteQueue(sqsUrl);
                System.out.println("deleted "+sqsUrl);
            }
        }
    }
}





