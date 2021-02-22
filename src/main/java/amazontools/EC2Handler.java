package amazontools;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class EC2Handler {

    private AmazonEC2 ec2;

    public EC2Handler() {
        this.ec2 = AmazonEC2ClientBuilder.defaultClient();
    }



    /**
     * @param tagName the name of tag to check if exists
     * @return if the tag exists
     */
    public boolean tagExists(String tagName){
        try {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);
            Tag tag = new Tag(tagName, "");

            for (Reservation reservation : response.getReservations())
                for (Instance instance : reservation.getInstances())
                    if (instance.getTags().contains(tag)) {
                        if(instance.getState().getName().equals("running")) {
                            return true;
                        }
                    }
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }

        return false;
    }

    /**
     * terminates an instance by id.
     * @param
     */
    public void terminatedInstance(String instanceID){
        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest().withInstanceIds(instanceID);
        ec2.terminateInstances(terminateInstancesRequest)
                .getTerminatingInstances()
                .get(0)
                .getPreviousState()
                .getName();
    }

    /**
     * @param min - the min number of instances to create
     * @param max - the max number of instances to create
     * @param userdata - the script to launch for each instance
     * @return array List of the instances created.
     */
    public ArrayList<Instance> createInstance(int min, int max, String userdata,String tags){
        Tag tag = new Tag(tags, "");
        TagSpecification resourceTags = new TagSpecification().withTags(tag);
        resourceTags.setResourceType(ResourceType.Instance);

        // Convert userData script to base 64
        String encodedUserData = Base64.getEncoder().encodeToString(userdata.getBytes());
        // ami image we created with various installations
        String projectPrivateAmi = AppConfig.imageId;

        //Create the request to run
        RunInstancesRequest request = new RunInstancesRequest(projectPrivateAmi, min, max);
        // define instance type
        request.setInstanceType(InstanceType.T2Micro.toString());
        // define the script to run in base 64
        request.withUserData(encodedUserData);
        //define the tag
        request.withTagSpecifications(resourceTags);
        // define the iam role
        request.withIamInstanceProfile(new IamInstanceProfileSpecification().withArn(AppConfig.roleArn));


        // run the instance with the above defined request
        RunInstancesResult instancesResult = null;
        try{
            instancesResult = this.ec2.runInstances(request);
        } catch (Exception e){
            e.printStackTrace();
            return new ArrayList<Instance>();
        }
        return new ArrayList<Instance> ( instancesResult.getReservation().getInstances());
    }


    /**
     * @param instances - list of instances to terminate.
     *                  if the list is null, all instances will be terminated
     * @return Number of instacnes terminated
     */
    public int terminateInstances(ArrayList<Instance> instances) {
        if (instances == null) {
            instances = getInstances("");
        }

        if (instances.size() == 0){
            return 0;
        }

        ArrayList<String> instancesToTerminate = new ArrayList<>();
        for (Instance instance:
                instances) {
            instancesToTerminate.add(instance.getInstanceId());
        }
        TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest(instancesToTerminate);
        if (!instances.isEmpty()) {
            TerminateInstancesResult result = this.ec2.terminateInstances(terminateRequest);
            return result.getTerminatingInstances().size();
        }
        return 0;

    }

    public void deleteInstancesWithTag(String tag){
        ArrayList<Instance> instances = getInstances(tag);
        terminateInstances(instances);
    }

    /**
     * @param tagName - can be empty string or an an existing tag name
     * @return - return a list of instances that has tagName attached to them.
     *          If the the tag is empty string, then return all instances.
     */
    public ArrayList<Instance> getInstances(String tagName){

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        boolean notdone = true;
        DescribeInstancesResult response= null;
        ArrayList<Instance> instancesResult= new ArrayList<>();
        while(notdone) {
            try {
                response = this.ec2.describeInstances(request);

            } catch (Exception e){
                try {
                    System.out.println("Ec2 describe Exception");
                    Thread.sleep(1000);
                    return getInstances(tagName);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            List<Reservation> reservations = response.getReservations();

            for(Reservation reservation :
                    reservations) {
                List<Instance> instances = reservation.getInstances();
                for (Instance instance:
                        instances ) {
                    Tag tag = new Tag(tagName,tagName);
                    Boolean run = instance.getState().getName().equals("running");
                    Boolean pend = instance.getState().getName().equals("pending");
                    if ( (tagName.equals("") || tagName == null) && ( run || pend )){
                        instancesResult.add(instance);
                    }
                    else if (instance.getTags().contains(tag) && (run || pend)){
                        instancesResult.add(instance);
                    }
                }
            }
            request.setNextToken(response.getNextToken());

            if(response.getNextToken() == null) {
                notdone = false;
            }

        }
        return  instancesResult;
    }
}