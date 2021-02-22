package localapp;

import java.util.*;
import amazontools.AppConfig;
import amazontools.EC2Handler;
import amazontools.S3Handler;
import amazontools.SQSHandler;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;
import messages.NewTaskMessage;
import messages.TerminateMessage;
import userdatascripts.ManagerUserDataScript;

import java.io.*;

enum ManagerStatus {
    EXISTS,
    NOTEXISTS
}

public class LocalApp {
    private EC2Handler ec2 = null;
    private SQSHandler sqsHandler =null;
    private S3Handler s3Handler= null;
    private ManagerStatus managerStatus;
    private String managerQueueUrl;
    private String personalQueueUrl;
    private String localAppBucketName;
    private String fileKey;
    private boolean needToTerminate;
    private int n;

    public String getPersonalQueueUrl() {
        return personalQueueUrl;
    }

    public LocalApp(boolean terminate, int _n){
        ec2 = new EC2Handler();
        sqsHandler = new SQSHandler();
        s3Handler= new S3Handler();
        this.needToTerminate=terminate;
        this.n=_n;
    }
    //init

    public void deleteBuckets(){
        s3Handler.deleteAllBuckets();

    }

    //upload
    public void uploadFile(String Filepath){
        //Local Application uploads the file with the list of images to S3
        localAppBucketName= generateUniqueName(AppConfig.bucketName);
        s3Handler.createBucket(localAppBucketName);
        fileKey = s3Handler.upload(localAppBucketName,Filepath);
    }


    //handleManager
    public void handleManager(){
        managerStatus=getManagerStatus();
        switch(managerStatus){
            case NOTEXISTS: //creates the manager instance and its queue.
                System.out.println("Manager does not exists, creating manager... \n");
                managerQueueUrl = sqsHandler.createQueue(generateUniqueName("managerQueue"));
                ec2.createInstance(1,1,new ManagerUserDataScript(n,managerQueueUrl,AppConfig.imageId).toString(),"manager").get(0);
                managerStatus=ManagerStatus.EXISTS;
                break;

            case EXISTS:
                System.out.println("Manager exists, submit to his queue...\n");
                managerQueueUrl = sqsHandler.getQueueUrl("managerqueue");
                if(managerQueueUrl==null) {
                    System.out.println("Manager queue doesn't exists. Please try again in a few moments...");
                    System.exit(0);
                }
                break;
            default:
        }
    }

    public void connect(){

        // create personal sqs:
        personalQueueUrl = sqsHandler.createQueue(generateUniqueName("LocalAppSqs"));

        //send the manager new task message.
        sqsHandler.sendMessage(managerQueueUrl, new NewTaskMessage(personalQueueUrl,localAppBucketName,fileKey).toString());
    }

    /**
     * waiting for  msg with specific description from the manager.
     * @return the  msg.
     * example: waitForMsg("doneTask")
     */
    public String waitForMsg(String msgDesc,String queueUrl){
        System.out.println("Waiting for response from the manager...");
        while(true){
            List<Message> messages = sqsHandler.recieveMessage(queueUrl,1,2*AppConfig.visibillityTime);//FIXME- check for visibillity.
            for(Message msg : messages) {
                if (msg.getBody().contains(msgDesc)) {
                    return msg.getBody();
                }
            }
        }
    }

    public S3Object downloadSummaryFile(String doneMsg){
        String[] data = doneMsg.split(AppConfig.msgDelim);
        String bucketName= data[1];
        String fileKey=data[2];
        S3Object summary = s3Handler.download(bucketName, fileKey);
        return summary;
    }

    public void fileToHtml(String outputFilePath,S3Object file){
        //Map<link,text>
        Map<String,String> map = deserialize(file.getObjectContent());
        String html=    "<!DOCTYPE html>\n"+
                        "<html>\n"+
                        "<body>\n";
        for (Map.Entry<String,String> entry : map.entrySet()){
            String link=entry.getKey();
            String text= entry.getValue();
            html+=  "<p>\n" +
                    "<img src="+link+"><br>"+
                    text+"<br></p>\n";
        }
        html+="</body></html>";

        try {
            //crashes here.
            File output = new File(outputFilePath);
            FileWriter fileWriter = new FileWriter(output);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(html);
            fileWriter.flush();
            fileWriter.close();
            System.out.println("create html file in: " +outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Map<String, String> deserialize(S3ObjectInputStream obj) {
        Map<String, String> map = new HashMap<>();
        try {
            BufferedInputStream inputStream = new BufferedInputStream(obj);
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            map = (Map<String, String>) ois.readObject();
            ois.close();
            inputStream.close();
            return map;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void terminate(){
        if (needToTerminate) {
            System.out.println("terminating manager...");
            sqsHandler.sendMessage(managerQueueUrl, new TerminateMessage(personalQueueUrl).toString());
            // wait for termination message from the manager.
            waitForMsg("terminate",personalQueueUrl);
            System.out.println("manager is done, terminates the manager and its queue");
            sqsHandler.deleteQueue(personalQueueUrl);
            ec2.deleteInstancesWithTag("");
        }
    }


    public String generateUniqueName(String name){
        return (name +UUID.randomUUID()).substring(0,30).replace('-','a')
                .replace('.','a').toLowerCase();
    }


    public ManagerStatus getManagerStatus(){
        boolean exists= ec2.tagExists("manager");
        return exists ? ManagerStatus.EXISTS : ManagerStatus.NOTEXISTS;
    }


}
