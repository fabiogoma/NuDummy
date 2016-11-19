package br.com.nubank;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.EC2MetadataUtils;

import br.com.nubank.pojos.Job;

public class Sleeper {
	private static Logger logger = Logger.getLogger(Sleeper.class);

	public static void main(String[] args) {
		
		Integer timer = Integer.valueOf(System.getenv("TIMER"));
		logger.info("Putting a thread to sleep for " + timer.toString() + " minutes");
		goSleep(timer);
		
		logger.info("Getting instance ID");
		Job job = new Job();
		job.setInstanceId(EC2MetadataUtils.getInstanceId().toString());
		job.setRequestId("");
		job.setSchedule("");
		job.setStatus("done");
		
		logger.info("Updating the status");
		updateStatus(job);
		
	}
	
	private static void goSleep(Integer timer){
		try {
			Thread.sleep(timer*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void updateStatus(Job job){
		AWSCredentials credentials = new EnvironmentVariableCredentialsProvider().getCredentials();
		
		AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		List<String> instanceIds = new ArrayList<String>();
		instanceIds.add(job.getInstanceId());
		
		Filter filter = new Filter("instance-id", instanceIds);
		
		List<Filter> filters = new ArrayList<Filter>();
		filters.add(filter);
		
		DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
		describeRequest.setFilters(filters);
		
		DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
        List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();
		
        for (SpotInstanceRequest describeResponse : describeResponses) {
        	job.setRequestId(describeResponse.getSpotInstanceRequestId().toString());
        }
        
		JSONObject jsonObject = new JSONObject(job);
    	String stringJob = jsonObject.toString();
		
		logger.info("Sending message to queue sqs_update");
		sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_UPDATE_URL"), stringJob));
		
		try {
			logger.info("Wait for 10 seconds before send signal to terminate the instance");
			Thread.sleep(10*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("Sending message to queue sqs_destroy");
		sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_DESTROY_URL"), stringJob));
	}
}
