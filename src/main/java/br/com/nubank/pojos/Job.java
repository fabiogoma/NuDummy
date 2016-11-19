package br.com.nubank.pojos;

public class Job {
	private String InstanceId;
	private String RequestId;
	private String Schedule;
	private String Status;
	public String getInstanceId() {
		return InstanceId;
	}
	public String getRequestId() {
		return RequestId;
	}
	public void setRequestId(String requestId) {
		RequestId = requestId;
	}

	public void setInstanceId(String instanceId) {
		InstanceId = instanceId;
	}
	public String getSchedule() {
		return Schedule;
	}
	public void setSchedule(String schedule) {
		Schedule = schedule;
	}
	public String getStatus() {
		return Status;
	}
	public void setStatus(String status) {
		Status = status;
	}
}
