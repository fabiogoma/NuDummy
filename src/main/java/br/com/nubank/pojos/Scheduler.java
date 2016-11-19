package br.com.nubank.pojos;

import java.util.Date;
import java.util.HashMap;

public class Scheduler {

	private String image;
	private Date schedule;
	private HashMap<String, String> variables = new HashMap<String, String>();
	
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public Date getSchedule() {
		return schedule;
	}
	public void setSchedule(Date schedule) {
		this.schedule = schedule;
	}
	public HashMap<String, String> getVariables() {
		return variables;
	}
	public void setVariables(HashMap<String, String> variables) {
		this.variables = variables;
	}
	
}
