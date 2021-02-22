package localapp;

import com.amazonaws.services.s3.model.S3Object;

public class Main {

    //java -jar yourjar.jar inputFileName outputFileName n ||
    // java -jar yourjar.jar inputFileName outputFileName n terminate
    public static void main(String[] args) {
        //handle input.
        int n;
        String inputFileName=args[0];
        String outputFileName=args[1];
        boolean terminate= args[args.length-1].equals("terminate");
        if(terminate) n = Integer.valueOf(args[args.length-2]);
        else n = Integer.valueOf(args[args.length-1]);

        //Init local app.
        LocalApp local= new LocalApp(terminate,n);

        //-----------------------------------testing-----------------------------//
        //local.deleteBuckets();


       //upload the input file
        String filePath = System.getProperty("user.dir") + "/"+inputFileName;
        local.uploadFile(filePath);

        //handle managar status
        local.handleManager();

        //connect to manager
        local.connect();

        //wait for manager to send the summary file
        String msg=local.waitForMsg("doneTask",local.getPersonalQueueUrl());

        //download summary file
        S3Object summary= local.downloadSummaryFile(msg);

        //create html file from the summary file
        String outputFilePath=System.getProperty("user.dir") + "/"+outputFileName;
        local.fileToHtml(outputFilePath,summary);

        //handle termination
        local.terminate();




    }
}
