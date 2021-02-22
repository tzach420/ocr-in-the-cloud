package worker;

import amazontools.AppConfig;
import amazontools.SQSHandler;
import com.amazonaws.services.sqs.model.Message;
import messages.DoneOcrTask;
import messages.DoneOcrTaskFail;
import messages.MsgType;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Worker {
    private SQSHandler sqs;
    private String workersInQueueUrl;
    private String workersOutQueueUrl;
    private Tesseract tesseract ;
    public Worker(String inQueueUrl,String outputQueueUrl){
        sqs=new SQSHandler();
        workersInQueueUrl=inQueueUrl;
        workersOutQueueUrl=outputQueueUrl;
        tesseract = new Tesseract();
        tesseract.setDatapath(AppConfig.tesseractDataPath);
    }

    /**
     * main process of the worker. reads messages from inWorkerQueue
     * msg types:
     * Terminate- the manager will send exacly |number of workers| termination messages to the WorkersQueue
     * when recieving terminate msg the worker will remove it from the queue, send back terminate msg
     * to the manager and exit his main loop, so that each worker will get exacly 1 terminaion msg.
     * NEW TASK MSG- uppon recieving new task msg the worker will apply the algorithm on the image link,
     * and will send the manager msg with the results.
     */
    public void start(){

        while(true){
            List<Message> messages = sqs.recieveMessage(workersInQueueUrl,1,2*AppConfig.visibillityTime);
            for(Message message : messages){
                MsgType msgType= AppConfig.getMsgType(message);
                String msgContent=message.getBody();
                String[] attr = msgContent.split(AppConfig.msgDelim);

                if(msgType== MsgType.NEWIMAGETASK){
                    // * example: newImageTask_12324localAPP3534_https://blabla.com/downloadfromhere
                    String localAppSqs=attr[1];
                    String imageUrl=attr[2];
                    String imageTxt = null;

                    try {
                        imageTxt = tesseract.doOCR(ImageIO.read(new URL(imageUrl).openStream()));
                        sqs.sendMessage(workersOutQueueUrl,new DoneOcrTask(localAppSqs,imageUrl,imageTxt).toString());

                    } catch (TesseractException e) {
                        //problem with the algorithm - send doneTaskFail back to the manger.
                        sqs.sendMessage(workersOutQueueUrl,new DoneOcrTaskFail(localAppSqs,imageUrl,"Failed to apply OCR algorithm on the image.").toString());
                    } catch (MalformedURLException e) {
                        //problem with the url - send doneTaskFail back to the manger.
                        sqs.sendMessage(workersOutQueueUrl,new DoneOcrTaskFail(localAppSqs,imageUrl,"Failed to open the url.").toString());
                    } catch (IOException e) {
                        //problem with the image - send doneTaskFail back to the manger.
                        sqs.sendMessage(workersOutQueueUrl,new DoneOcrTaskFail(localAppSqs,imageUrl,"Failed to get the image.").toString());
                    }
                    sqs.deleteMessage(workersInQueueUrl,message);
                    //NOTE- if the worker has crushed for some reason, the msg will still be in the queue
                    //and another worker will process it. (because of visibillity time);
                }

            }
        }
    }
}
