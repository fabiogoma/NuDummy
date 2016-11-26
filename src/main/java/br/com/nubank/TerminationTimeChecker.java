package br.com.nubank;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class TerminationTimeChecker implements Runnable {
	private static Logger logger = Logger.getLogger(TerminationTimeChecker.class);
	
	public static String getJson(){
    	DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.MINUTE, 2);
		String newTime = df.format(cal.getTime());

		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(Paths.get("/tmp/schedule.json"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String jsonFile = new String(encoded, StandardCharsets.UTF_8);
		
		JSONObject jsonObject = new JSONObject(jsonFile).getJSONObject("job");
		
		jsonObject.remove("schedule");
		jsonObject.put("schedule", newTime);
		
		logger.info("JSON schedule time changed to: " + newTime);

		return jsonObject.toString();
	}
	
	public static void checking() throws InterruptedException{
		try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/spot/termination-time");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String terminationTime = "";
            if (null != (terminationTime = br.readLine())) {
            	logger.info("Terminate on: " + terminationTime);
            	
            	AWSCredentials credentials = new EnvironmentVariableCredentialsProvider().getCredentials();	
        		AmazonSQS sqs = new AmazonSQSClient(credentials);
        		
        		String newSpotInstanceRequest = getJson();
        		        		
        		logger.info("Request a new instance to continue this job");
        		sqs.sendMessage(new SendMessageRequest(System.getenv("SQS_LAUNCH_URL"), newSpotInstanceRequest));
        		
            }
	    } catch (FileNotFoundException e) {
	    	Thread.sleep(5*1000);
	    	checking();
	    	logger.info("Check again in 5 seconds");
	    	e.printStackTrace();
	    } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			checking();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
