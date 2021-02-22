package userdatascripts;

/**
 * this class represent the command of to apply when the ec2 instance of the worker is up. (the user script)
 *
 * example: "java -jar user arg0 arg1... argn
 */
public class WorkerUserDataScript extends UserDataScript {

    public WorkerUserDataScript(String inputQueue,String outputQueue, String imageId){
        String jarName="DSP1-COMPLETE.jar";
        String classPath="worker.Main";
        executeCommand="#!/bin/bash\n" + "cd /home/ubuntu/\n" +  "sudo java -cp "+jarName+" "+classPath+" "+inputQueue+" "+outputQueue+" "+imageId+"\n";
    }
}
