package manager;
import amazontools.AppConfig;
import amazontools.SQSHandler;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import messages.DoneTaskMessage;
import messages.NewImageTaskMessage;
import userdatascripts.WorkerUserDataScript;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * this class is the responsible to download a file from a bucket, parse the data and send
 *its lines to the workers queue, and update the manager once the work on the file finished by sending a done msg to the localAPP.
 */
public class Job implements  Runnable {
    private Data data;
    private String localAppSqsId;
    private String bucketName;
    private String fileKey;
    private int numOfTasks;
    private ConcurrentHashMap<String, String> resultMap; //<image url, the image text>
    private AtomicInteger numOfCompletedTasks;
    private AtomicInteger numOfFailedTasks;
    private String outputFileKey;

    public Job(String localAppQueueUrl, String _bucketName, String _fileKey, Data data){
        localAppSqsId=localAppQueueUrl;
        bucketName=_bucketName;
        fileKey= _fileKey;
        numOfCompletedTasks=new AtomicInteger(0);
        numOfFailedTasks= new AtomicInteger(0);
        this.data=data;
        resultMap= new ConcurrentHashMap<>();

    }
    public void run() {
        //download object from bucket.
        S3Object obj= data.getS3().download(bucketName,fileKey);

        //parse object into mission.
        ArrayList<NewImageTaskMessage> messages= s3objectToMsgs(obj);
        numOfTasks= messages.size();
        handleWorkersCreation(numOfTasks);

        //send all  missions to workersQueue.
        for(NewImageTaskMessage msg: messages){
            SQSHandler sqs= data.getSqs();
            String workersQueue=data.getWorkersInQueueUrl();
            sqs.sendMessage(workersQueue,msg.toString());
        }

        //wait untill the job is done
        while(!isJobDone()){
                try{
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(isJobDone()){
                    //job is done, create summary file, upload it to bucket
                    try {
                        String filePath = createSummaryFile();
                        outputFileKey = data.getS3().upload(bucketName, filePath);
                        break;
                    }catch (Exception e){e.printStackTrace();}
                }
        }

        //job is done- send done mission to the localApp and remove the job from the jobsMap.
        data.getSqs().sendMessage(localAppSqsId,new DoneTaskMessage(bucketName,outputFileKey).toString());
        data.getJobs().remove(localAppSqsId);
    }


    public ArrayList<NewImageTaskMessage> s3objectToMsgs(S3Object obj){
        ArrayList<NewImageTaskMessage>res = new ArrayList<NewImageTaskMessage>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
        String line;
        try {
            while((line = reader.readLine())!=null){
                if(line.isEmpty()) break;
                res.add(new NewImageTaskMessage(localAppSqsId,line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean isJobDone(){
        return numOfCompletedTasks.get()+numOfFailedTasks.get() == numOfTasks;
    }

    /**
     * this function creates a summary file from the results map and returns its path on the machine.
     * @return path of the file created.
     */
    public String createSummaryFile() {

        try {
            String fileName="summary"+UUID.randomUUID()+".ser";
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(resultMap);
            oos.close();
            fos.close();
            return fileName;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public synchronized void handleWorkersCreation(int numOfTasks){
        int awa=data.getActiveWorkers().get();
        if(awa<AppConfig.maxNumOfWorkers) {
            int numOfWorkersToAdd = (int) Math.ceil(numOfTasks / data.getN()) - awa;
            String inQueue = data.getWorkersInQueueUrl();
            String outQueue = data.getWorkersOutQueueUrl();
            for (int i = 0; i < numOfWorkersToAdd; i++) {
                System.out.println("===========Creating workers===============");
                Instance worker = data.getEc2().createInstance(1, 1, new WorkerUserDataScript(inQueue, outQueue, AppConfig.imageId).toString(), "worker").get(0);
                data.getActiveWorkers().addAndGet(1);
                String workerId = worker.getInstanceId();
                data.getWorkers().add(workerId);
                System.out.println("created a worker instance with id:" + workerId);
            }
        }
    }
    public synchronized ConcurrentHashMap<String, String> getResultMap() {
        return resultMap;
    }

    public synchronized AtomicInteger getNumOfCompletedTasks() {
        return numOfCompletedTasks;
    }

    public synchronized AtomicInteger getNumOfFailedTasks() {
        return numOfFailedTasks;
    }
}
