package amazontools;
import com.amazonaws.services.sqs.model.Message;
import messages.MsgType;

public class AppConfig {
    public static String imageId="ami-09f35faccab31f6c9";
    public static final String queuePrefix= "https://sqs.us-west-2.amazonaws.com/002041186709/";
    public static final String msgDelim= "_--_";
    public static final String bucketName= "dsp1bucket";
    public static final String roleArn="arn:aws:iam::430125612162:instance-profile/role-for-everything";
    public static int visibillityTime=30;
    public static int numOfThreads=10;
    public static final String tesseractDataPath= "/home/ubuntu"; //this is the adress on my ec2 instance
    public static int maxNumOfWorkers=10;
    //public static final String tesseractDataPath= "/home/tzach"; //FIXME FOR TESTS ONLY




    public static MsgType getMsgType(Message msg){
        String body=msg.getBody();
        MsgType msgType= MsgType.NONE;
        String[] data = body.split(AppConfig.msgDelim);
        String msgDesc=data[0];
        switch (msgDesc){
            case "doneOcrTask": msgType= MsgType.DONEOCRTASK;
                break;
            case "doneOcrTaskFail": msgType= MsgType.DONEOCRTASKFAIL;
                break;
            case "doneTask": msgType= MsgType.DONETASK;
                break;
            case "newImageTask": msgType= MsgType.NEWIMAGETASK;
                break;
            case "newTask": msgType= MsgType.NEWTASK;
                break;
            case "terminate": msgType= MsgType.TERMINATE;
                break;
        }
        return msgType;
    }
}
