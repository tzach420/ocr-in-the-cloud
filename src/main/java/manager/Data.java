package manager;

import amazontools.EC2Handler;
import amazontools.S3Handler;
import amazontools.SQSHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Data {
    private EC2Handler ec2;
    private S3Handler s3;
    private SQSHandler sqs;

    // SQS Queues:
    private String workersInQueueUrl;
    private String workersOutQueueUrl;
    private String managerQueueUrl;
    private String LocalAppThatSentTerminateId;


    private int n;
    private ExecutorService jobsPool;


    //atomics
    private AtomicBoolean terminate;
    private AtomicInteger activeWorkers;

    //data structrues
    private ConcurrentHashMap<String, Job> Jobs; //<client sqs id, job>
    private ConcurrentLinkedQueue<String> workers;

    public Data(String managerQueue, int n, int numOfThreads){
        ec2 = new EC2Handler();
        s3 = new S3Handler();
        sqs = new SQSHandler();
        jobsPool = Executors.newFixedThreadPool(numOfThreads);
        terminate = new AtomicBoolean(false);
        activeWorkers= new AtomicInteger(0);
        managerQueueUrl = managerQueue;
        workersInQueueUrl = sqs.createQueue("workersQueueIn");
        workersOutQueueUrl = sqs.createQueue("workersQueueOut");
        this.n=n;
        Jobs = new ConcurrentHashMap<>();
        workers = new ConcurrentLinkedQueue<>();
    }

    public String getLocalAppThatSentTerminateId() {
        return LocalAppThatSentTerminateId;
    }

    public void setLocalAppThatSentTerminateId(String localAppThatSentTerminateId) {
        LocalAppThatSentTerminateId = localAppThatSentTerminateId;
    }

    public  EC2Handler getEc2() {
        return ec2;
    }

    public S3Handler getS3() {
        return s3;
    }

    public SQSHandler getSqs() {
        return sqs;
    }

    public String getWorkersInQueueUrl() {
        return workersInQueueUrl;
    }

    public String getWorkersOutQueueUrl() {
        return workersOutQueueUrl;
    }

    public String getManagerQueueUrl() {
        return managerQueueUrl;
    }

    public int getN() {
        return n;
    }

    public ExecutorService getJobsPool() {
        return jobsPool;
    }

    public ConcurrentLinkedQueue<String> getWorkers() {
        return workers;
    }

    public synchronized AtomicBoolean getTerminate() {
        return terminate;
    }

    public synchronized void setTerminate(boolean val) {
        this.terminate.set(val);
    }

    public synchronized ConcurrentHashMap<String, Job> getJobs() {
        return Jobs;
    }

    public synchronized AtomicInteger getActiveWorkers() {
        return activeWorkers;
    }


}
