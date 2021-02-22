package amazontools;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import software.amazon.ion.NullValueException;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using
 * the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on
 * Amazon S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 *                   AwsCredentials.properties file before you try to run this
 *                   sample.
 * http://aws.amazon.com/security-credentials
 */
public class S3Handler {

    private AmazonS3 s3 = null;

    public S3Handler() {

        try{
            this.s3 = AmazonS3ClientBuilder.defaultClient();
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }


    public AmazonS3 getS3() {
        return s3;
    }


    public void createBucket(String newBucket){

        if (!s3.doesBucketExistV2(newBucket)) {
            try {
                s3.createBucket(newBucket);
                System.out.println("Created Bucket " + newBucket);

            } catch (AmazonServiceException ase) {
                printServiceError(ase);

            } catch (AmazonClientException ace) {
                printClientError(ace);
            }
        }
    }


    /**
     * upload a file from filePath to the bucket with bucketName
     * @param bucketName
     * @param filePath
     * @return the key of the uploaded file
     */
    public String upload(String bucketName, String filePath){
        System.out.println("Uploading a new object to S3 from a file");
        String key = UUID.randomUUID().toString()+filePath.replace('\\', 'a').replace('/', 'a')
                .replace(':', 'a');
        File file = new File(filePath);
        PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
        s3.putObject(req);
        return key;
    }


    public S3Object download(String bucketName,String key) {
        S3Object answer = null;
        try {
            System.out.println("downloading file: " + key);
            answer = s3.getObject(new GetObjectRequest(bucketName, key));
        }  catch (AmazonServiceException ase) {
            printServiceError(ase);

        } catch (AmazonClientException ace) {
            printClientError(ace);
        } catch (NullValueException e){
            e.printStackTrace();
        }

        return answer;
    }

    public void deleteAllBuckets() {
        try {
            List<Bucket> listOfBuckets = s3.listBuckets();
            for (Bucket bucket : listOfBuckets) {
                ObjectListing objectListing = s3.listObjects(bucket.getName());
                while (true) {
                    Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
                    while (objIter.hasNext()) {
                        s3.deleteObject(bucket.getName(), objIter.next().getKey());
                    }
                    if (objectListing.isTruncated()) {
                        objectListing = s3.listNextBatchOfObjects(objectListing);
                    } else {
                        break;
                    }
                }
                System.out.println("deleting the bucket: " + bucket.getName());
                s3.deleteBucket(bucket.getName());

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void deleteObject(String bucketName, String key){
        try {
            System.out.println("Deleting an object from "+bucketName);
            this.s3.deleteObject(bucketName, key);

        }  catch (AmazonServiceException ase) {
            printServiceError(ase);

        } catch (AmazonClientException ace) {
            printClientError(ace);
        }
    }


    private void printServiceError(AmazonServiceException ase){
        System.out.println("Caught an AmazonServiceException, which means your request made it " +
                "to Amazon S3, but was rejected with an error response for some reason.");
        System.out.println("Error Message:    " + ase.getMessage());
        System.out.println("HTTP Status Code: " + ase.getStatusCode());
        System.out.println("AWS Error Code:   " + ase.getErrorCode());
        System.out.println("Error Type:       " + ase.getErrorType());
        System.out.println("Request ID:       " + ase.getRequestId());
    }
    private void printClientError(AmazonClientException ace){
        System.out.println("Caught an AmazonClientException, which means the client encountered " +
                "a serious internal problem while trying to communicate with S3, such as not " +
                "being able to access the network.");
        System.out.println("Error Message: " + ace.getMessage());
    }

}