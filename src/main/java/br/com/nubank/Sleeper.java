package br.com.nubank;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
		AmazonSQS sqs = new AmazonSQSClient(credentials);
		
		JSONObject jsonObject = new JSONObject(job);
    	String stringJob = jsonObject.toString();
		
		logger.info("Sending message to queue sqs_update");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_update", stringJob));
		
		logger.info("Sending message to queue sqs_destroy");
		sqs.sendMessage(new SendMessageRequest("https://us-west-2.queue.amazonaws.com/678982507510/sqs_destroy", stringJob));
	}
}
