package manager;

import amazontools.AppConfig;

public class Main {

    /**
     * main class activated by managerUserDataScript with the args :
     * n- employee per n messages.
     * managerQueueUrl- the manager queue url.
     * ami image - the ami image numner
     * example: java -jar manager n managerurl amiImage
     */
    public static void main(String[] args){
        int n=Integer.valueOf(args[0]);
        String managerQueue= args[1];
        AppConfig.imageId=args[2];
        Manager manager=new Manager(managerQueue,n,AppConfig.numOfThreads);
        manager.start();
    }


}
