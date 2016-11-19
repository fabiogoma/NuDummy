package br.com.nubank;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import br.com.nubank.pojos.Job;

public class Sleeper {
	private static Logger logger = Logger.getLogger(Sleeper.class);

	public static void main(String[] args) {
		
		goSleep();
		
		Job job = new Job();
		job.setInstanceId(getInstanceId());
		job.setRequestId("");
		job.setSchedule("");
		job.setStatus("done");
		
		updateStatus(job);

	}
	
	private static void goSleep(){
		try {
			Thread.sleep(60*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String getInstanceId(){
		String instanceId = null;
		try {
			URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
			BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
			String strTemp = "";
			while (null != (strTemp = br.readLine())) {
				instanceId = strTemp;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return instanceId;
	}

	private static void updateStatus(Job job){
		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		
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
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_update", stringJob));
		
		logger.info("Sending message to queue sqs_destroy");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_destroy", stringJob));
	}
}
