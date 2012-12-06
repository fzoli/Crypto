package org.dyndns.fzoli.utils.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

public class ShellUtils {
	
	private static Process createProcess(boolean isRoot) throws IOException {
		String command = isRoot ? "su" : "sh";
		return Runtime.getRuntime().exec(command);
	}
	
	private static String getProcessOutput(Process proc) {
		InputStreamReader reader = new InputStreamReader(proc.getInputStream());
		StringBuilder log = new StringBuilder();
		final char[] buf = new char[10];
		int read = 0;
		try {
			while ((read = reader.read(buf)) != -1) {
				log.append(buf, 0, read);
			}
		} catch (IOException ex) {}
		return log.toString();
	}
	
	private static ProcessExitInfo waitProcess(Process proc, long timeout) {
		ProcessWaiter waiter = new ProcessWaiter(proc);
		Thread worker = new Thread(waiter);
		worker.start();
		try {
			worker.join(timeout);
		}
		catch (InterruptedException ex) {
			worker.interrupt();
		    Thread.currentThread().interrupt();
		}
		finally {
			proc.destroy();
		}
		return waiter.getExitInfo();
	}
	
	public static boolean execute(String cmd, String input, long timeout, boolean isRoot) {
		try {
	    	final Process proc = createProcess(isRoot);
	    	final PrintStream out = new PrintStream(proc.getOutputStream());
			final BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			out.println(cmd);
			stdout.readLine();
			out.println(input);
			out.flush();
			out.println("exit");
			out.flush();
			return waitProcess(proc, timeout).isSuccess();
		}
		catch(IOException ex) {
			return false;
		}
	}
	
	public static ProcessInfo execute(String[] cmds, long timeout, boolean isRoot) {
		try {
			Process proc = createProcess(isRoot);
			OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
			for (String cmd : cmds) {
				out.write(cmd + "\n");	
			}
			out.flush();
			out.write("exit\n");
			out.flush();
			ProcessExitInfo info = waitProcess(proc, timeout);
			if(info.isInterrupted()) {
				return new ProcessInfo("InterruptedException", false);
			}
			return new ProcessInfo(getProcessOutput(proc), info.isSuccess());
		}
		catch(IOException ex) {
			return new ProcessInfo("IOException", false);
		}
	}
	
	public static ProcessInfo execute(String[] cmds, boolean isRoot) {
		return execute(cmds, Long.MAX_VALUE, isRoot);
	}
		
	public static ProcessInfo execute(String cmd, boolean isRoot) {
		return execute(new String[] {cmd}, isRoot);
	}
	
}

class ProcessExitInfo {
	
	private int exitCode;
	private boolean interrupted;
	
	public ProcessExitInfo(int exitCode, boolean interrupted) {
		this.exitCode = exitCode;
		this.interrupted = interrupted;
	}
	
	public int getExitCode() {
		return exitCode;
	}
	
	public boolean isInterrupted() {
		return interrupted;
	}
	
	public boolean isSuccess() {
		return !interrupted && exitCode == 0;
	}
	
}

class ProcessWaiter implements Runnable {
	
	private Process proc;
	private Integer exitCode;
	
	public ProcessWaiter(Process proc) {
		this.proc = proc;
	}
	
	public boolean isInterrupted() {
		return exitCode == null;
	}
	
	public int getExitCode() {
		return isInterrupted() ? -1 : exitCode;
	}
	
	public ProcessExitInfo getExitInfo() {
		return new ProcessExitInfo(getExitCode(), isInterrupted());
	}
	
	@Override
	public void run() {
		try {
			exitCode = proc.waitFor();
		}
		catch (InterruptedException ex) {
			;
		}
	}
	
}