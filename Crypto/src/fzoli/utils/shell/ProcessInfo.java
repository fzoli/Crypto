package fzoli.utils.shell;

public class ProcessInfo {
	
	private boolean success;
	private String output;
	
	public ProcessInfo(String output, boolean success) {
		this.output = output;
		this.success = success;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public String getOutput() {
		return output;
	}
	
}
