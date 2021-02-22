package userdatascripts;

/**
 * this class represent the command of to apply when the ec2 instance of the manager is up. (the user script)
 * args: n, managerQueueUrl, AMIimageId
 * example: "java -jar manager arg0 arg1... argn
 */
public class ManagerUserDataScript extends UserDataScript {

    public ManagerUserDataScript(int n,String managerQueue, String amiId){
        String jarName="DSP1-COMPLETE.jar";
        String classPath="manager.Main";
        executeCommand="#!/bin/bash\n" + "cd /home/ubuntu/\n" +  "sudo java -cp "+jarName+" "+classPath+" " +n+" " +managerQueue+" " +amiId+"\n";

    }
}
