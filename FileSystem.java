package PFS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class FileSystem {
	
	public ArrayList<FileVolume> open(String volumePath, String volumeFileName) throws FileNotFoundException, ClassNotFoundException, IOException {
		// First check if the file is there in the volumePath/volumeFileName
		// If not there, create a new FileVolume class, if already there convert the existing volume files to FileVolume object

		ArrayList<FileVolume> fvList = new ArrayList<FileVolume>();
		File dir = new File(volumePath);
		File[] listOfFiles = dir.listFiles();
		ArrayList<File> filesOfVolume = new ArrayList<File>();
		for (int i=0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().startsWith(volumeFileName) && !listOfFiles[i].getName().contains("meta")) {
				filesOfVolume.add(listOfFiles[i]);
			}
		}
		if (filesOfVolume.size() != 0) {
			for (File f: filesOfVolume) {
				FileVolume fv;
				fv = FileVolume.getFileVolumeFromFile(volumePath, f.getName());
				fvList.add(fv);
			}
		} else {
			FileVolume fv = new FileVolume(volumePath,volumeFileName);
			fv.writeVolumeToFile();
			fvList.add(fv);
		}

		return fvList;
	}
	
	public FileVolume put(ArrayList<FileVolume> fvList, String userFilePath, String userFileName, String volumePath) throws IOException {
		// Puts a user file from the OS to an available fileVolume
		// Returns a new volume if it had been created as part of this operation, Otherwise null
		//
		//    1) First ensure that the file size does not exceed 10240 bytes (10 KB)
		//    2) Then walk all fileVolumes in fvList and ensure the file is already not there
		//    3) Once we ensure file does not already exist, walk from the first volume
		//       to try creating the file in an existing volume. If no space in any existing volumes,
		//       create a new volume.
		File f = new File(userFilePath+"/"+userFileName);
		if (!f.exists() || !f.isFile() || f.length() > 10240) {
			throw new IllegalArgumentException("Invalid File Name or Size");
		}
		
		for (FileVolume fv : fvList) {
			if(fv.getFile(userFileName) != null) {
				throw new IllegalArgumentException("File Already exists in " + fv.getVolumeFileName());
			}
		}
		
		for (FileVolume fv : fvList) {
			try {
				System.out.println("Checking volume " + fv.getVolumeFileName() + "...");
				fv.createFile(userFilePath, userFileName);
				return null;
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IndexOutOfBoundsException e) {
				// It means that there is no space in the current FileVolume. Try other volumes
				continue;
			} catch (IOException ex) {
				throw ex;
			}
		}
		
		// Create a new volume
		String newVolumeName = "";
		if (fvList.get(0).getVolumeFileName().contains(".")) {
			newVolumeName = fvList.get(0).getVolumeFileName().split(".")[0] + "." + fvList.size();
		} else {
			newVolumeName = fvList.get(0).getVolumeFileName() + ".1";
		}
		System.out.println("Creating new volume " + newVolumeName + "...");
		FileVolume fv = new FileVolume(volumePath, newVolumeName);
		try {
		    fv.createFile(userFilePath, userFileName);
		    return fv;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IndexOutOfBoundsException e) {
			// It means that there is no space in the current FileVolume. Try other volumes
			throw e;
		} catch (IOException ex) {
			throw ex;
		}
	}
	
	public void get(ArrayList<FileVolume> fvList, String userFileName) throws IOException {
		// Gets a user file from the PFS file system and creates a new file in the OS's cur directory
		for (FileVolume fv: fvList) {
			byte[] userFileBytes = fv.getFile(userFileName);
			if (userFileBytes != null) {
				// Write in the current directory
				FileOutputStream fos = new FileOutputStream(userFileName);
				fos.write(userFileBytes);
				fos.close();
				return;
			}
		}
	}
	
	public void rm(ArrayList<FileVolume> fvList, String userFileName) throws IOException {
		// Removes a user file from the PFS file system
		for (FileVolume fv: fvList) {
			if (fv.getFile(userFileName) != null) {
				fv.deleteFile(userFileName);
				break;
			}
		}
	}
	
	public void dir(ArrayList<FileVolume> fvList) {
		// List all files currently inside the PFS fileSystem
		for (FileVolume fv: fvList) {
			HashMap<String, FileControlBlock> fcbMap = fv.listFiles();
			Iterator it = fcbMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				String fileName = (String) pair.getKey();
				FileControlBlock fcb = (FileControlBlock) pair.getValue();
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				String dateString  = dateFormat.format(new Date(fcb.getFileTimestamp()));
				System.out.println(fileName + "\t" + fcb.getFileSize() + " bytes \t" + dateString + "\t" + fcb.getRemarks());
			}
		}
	}
	
	public void putr(ArrayList<FileVolume> fvList, String userFileName, String userRemarks) throws IOException {
		// Add remarks to an existing file in the PFS
		for (FileVolume fv: fvList) {
			if (fv.getFile(userFileName) != null) {
				FileControlBlock fcb = fv.getDs().getFileMetadata(userFileName);
				fcb.setRemarks(userRemarks);
				fv.writeVolumeToFile();
				break;
			}
		}
		
	}
	
	public void kill(ArrayList<FileVolume> fvList) {
		// Delete all associated volumes
		for (FileVolume fv : fvList) {
			File f = new File(fv.getVolumePath() + "/" + fv.getVolumeFileName());
			f.delete();
			f = new File(fv.getVolumePath() + "/" + fv.getVolumeFileName() + ".meta");
			f.delete();
		}
	}
	
	public static void getUserInput(FileSystem pfs) {
		// A CLI shell Prompt that will get user input.
		String userChoice = "";
		ArrayList<FileVolume> curVolInUse = null;
		while (!userChoice.equalsIgnoreCase("exit")) {
			System.out.print("PFS>");
			Scanner reader = new Scanner(System.in);
			userChoice = reader.nextLine();
			String[] args = userChoice.trim().split(" ");
			if (args[0].equals("open")) {
				try {
					curVolInUse = pfs.open("/tmp/", args[1]);
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (args[0].equals("put")) {
				String[] userFileNameWithPath = args[1].split("/");
				String userFileName = userFileNameWithPath[userFileNameWithPath.length - 1];
				StringJoiner sj = new StringJoiner("/");
				for (int i=0; i < userFileNameWithPath.length - 1; i++) {
					sj.add(userFileNameWithPath[i]);
				}
				String userFilePath = sj.toString();
				try {
					FileVolume fv = pfs.put(curVolInUse, userFilePath, userFileName, "/tmp/");
					if (fv != null) {
						// A new volume as created. Updated volume in use 
						try {
							String[] temp = fv.getVolumeFileName().split("\\.");
							curVolInUse = pfs.open("/tmp/", temp[0]);
						} catch (ClassNotFoundException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (args[0].equals("get")) {
				String userFileName = args[1];
				try {
					pfs.get(curVolInUse, userFileName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (args[0].equals("rm")) {
				String userFileName = args[1];
				try {
					pfs.rm(curVolInUse, userFileName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (args[0].equals("dir")) {
				pfs.dir(curVolInUse);
			} else if (args[0].equals("putr")) {
				String userFileName = args[1];
				StringJoiner sj = new StringJoiner(" ");
				for (int i=2; i < args.length; i++) {
					sj.add(args[i]);
				}
				String remarks = sj.toString();
				try {
					pfs.putr(curVolInUse, userFileName, remarks);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (args[0].equals("kill")) {
				pfs.kill(curVolInUse);
				curVolInUse = null;
			}
 		}
	}

	public static void main(String[] args) {
		FileSystem pfs = new FileSystem();
		FileSystem.getUserInput(pfs);
	}

}
