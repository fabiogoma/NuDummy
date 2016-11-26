package br.com.nubank;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class TerminationTimeChecker extends Thread {
	private static Logger logger = Logger.getLogger(TerminationTimeChecker.class);
	
	public TerminationTimeChecker() {
		
	}
	
	public void run(){
		try {
			checking();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
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
   	
        		String newSpotInstanceRequest = getJson();
        		JSONObject jobJson = new JSONObject(newSpotInstanceRequest);
        		logger.info("Request a new instance to continue this job");
        		
        		URL provisionerUrl;
        		URLConnection urlConn;
        		DataOutputStream printout;
        		provisionerUrl = new URL ("http://" + jobJson.getJSONObject("job").getString("PROVISIONER_IP") + "/schedule/");
        		urlConn = provisionerUrl.openConnection();
        		urlConn.setDoInput (true);
        		urlConn.setDoOutput (true);
        		urlConn.setUseCaches (false);
        		urlConn.setRequestProperty("Content-Type","application/json");   

        		urlConn.connect();  
        		
        		JSONObject jsonParam = new JSONObject(newSpotInstanceRequest);
        		
        		printout = new DataOutputStream(urlConn.getOutputStream());
        		printout.writeBytes(URLEncoder.encode(jsonParam.toString(),"UTF-8"));
        		printout.flush ();
        		printout.close ();
        		
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

}
