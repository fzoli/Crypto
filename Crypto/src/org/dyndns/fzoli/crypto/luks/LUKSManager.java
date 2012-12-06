package org.dyndns.fzoli.crypto.luks;

import java.io.File;

import org.dyndns.fzoli.crypto.database.ListEntry;
import org.dyndns.fzoli.utils.DigestUtils;
import org.dyndns.fzoli.utils.shell.ShellUtils;

public class LUKSManager {
	
	private final LoopbackLogger LOGGER;
	
	private final String CRYPTSETUP;
	
	public LUKSManager(LoopbackLogger logger, String cryptsetup) {
		this.LOGGER = logger;
		this.CRYPTSETUP = cryptsetup;
	}
	
	public boolean isBinaryExists() {
		return new File(CRYPTSETUP).exists();
	}
	
	private String findFreeLoopbackDevice() {
		try {
			String device = ShellUtils.execute("losetup -f", false).getOutput();
			return device.substring(0, device.length() - 1);
		}
		catch (Exception ex) {
			return null;
		}
	}
	
	private String createMapperName(String filePath) {
		String name;
		try {
			name = DigestUtils.md5(filePath);
		}
		catch (Exception e) {
			name = filePath.substring(filePath.lastIndexOf("/") + 1);
		}
		return "LUKS" + name;
	}
	
	public boolean isUsed(ListEntry entry) {
		return isUsed(createMapperName(entry.getFilePath()));
	}
	
	private boolean isUsed(String mapper) {
		return isMounted(mapper) || isLoopbackUsed(mapper);
	}
	
	private boolean isMounted(String mapper) {
		String df = ShellUtils.execute("df |grep " + mapper, false).getOutput();
		return df.contains("/dev/mapper/" + mapper);
	}
	
	private boolean isLoopbackUsed(String mapper) {
		String dev = LOGGER.getLog(mapper);
		if (dev == null) return false;
		String lo = ShellUtils.execute("losetup", false).getOutput();
		return lo.contains(dev);
	}
	
	public boolean umount(ListEntry entry) {
		return umount(entry.getFilePath());
	}
	
	private boolean umount(String filePath) {
		String mapper = createMapperName(filePath);
		if (isBinaryExists() && isUsed(mapper)) {
			boolean umounted = ShellUtils.execute(new String[] {
					"umount /dev/mapper/" + mapper,
					CRYPTSETUP + " luksClose " + mapper,
					"losetup -d " + LOGGER.getLog(mapper)
			}, true).isSuccess();
			if (umounted) LOGGER.removeLog(mapper);
			return umounted;
		}
		return false;
	}
	
	public boolean isPathsExists(ListEntry entry) {
		return isPathsExists(entry.getFilePath(), entry.getMountPath());
	}
	
	private boolean isPathsExists(String filePath, String mountPath) {
		return new File(filePath).exists() && new File(mountPath).exists();
	}
	
	public boolean mount(ListEntry entry, String password) {
		return mount(entry.getFilePath(), entry.getMountPath(), password);
	}
	
	private boolean mount(String filePath, String mountPath, String password) {
		String mapper = createMapperName(filePath);
		if (!isBinaryExists() || (!isPathsExists(filePath, mountPath) && !isUsed(mapper))) return false;
		String loDevice = findFreeLoopbackDevice();
		if (loDevice == null) return false;
		boolean prepared = ShellUtils.execute(new String[] {
			"mknod " + loDevice + " b 7 " + loDevice.substring(9),
			"losetup " + loDevice + " " + filePath
			}, true).isSuccess();
		if (prepared) {
			if (ShellUtils.execute(CRYPTSETUP + " luksOpen " + loDevice + " " + mapper, password, 20000, true)) {
				if (ShellUtils.execute("mount /dev/mapper/" + mapper + " " + mountPath, true).isSuccess()) {
					LOGGER.addLog(mapper, loDevice);
					return true;
				}
			}
			else {
				ShellUtils.execute("losetup -d " + loDevice, false);
			}
		}
		return false;
	}
	
}