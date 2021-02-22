package worker;

import amazontools.AppConfig;

/**
 * @pre: the machine has tesseract installed -sudo apt install tesseract-ocr
 * // the machine has testData folder containning the eng.traineddata.
 * // the path for the testData in the WorkerConfig is right.
 */
public class Main {

    public static void main(String[] args) {

        String inputQueueUrl=args[0];
        String outputQueueUrl= args[1];
        AppConfig.imageId=args[2];
        Worker worker=new Worker(inputQueueUrl,outputQueueUrl);
        worker.start();
    }
}
