package PFS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.TreeSet;

// Class for File Control block. It has the metadata associated with the file

class FileControlBlock implements java.io.Serializable {
	private int fileSize;
	private long fileTimestamp;
	private int blockStart;
	private int blockLength;
	private String remarks;
	
	FileControlBlock(int fileSize, int blockStart, int blockLength) {
		this.fileSize = fileSize;
		this.fileTimestamp = System.currentTimeMillis();
		this.blockStart = blockStart;
		this.blockLength = blockLength;
		this.remarks = "";
	}

	@Override
	public String toString() {
		return "FileControlBlock [fileSize=" + fileSize + ", fileTimestamp="
				+ fileTimestamp + ", blockStart=" + blockStart
				+ ", blockLength=" + blockLength + ", remarks=" + remarks + "]";
	}

	public int getFileSize() {
		return fileSize;
	}

	public long getFileTimestamp() {
		return fileTimestamp;
	}

	public int getBlockStart() {
		return blockStart;
	}

	public int getBlockLength() {
		return blockLength;
	}
	
	public String getRemarks() {
		return remarks;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public void setFileTimestamp(long fileTimestamp) {
		this.fileTimestamp = fileTimestamp;
	}

	public void setBlockStart(int blockStart) {
		this.blockStart = blockStart;
	}

	public void setBlockLength(int blockLength) {
		this.blockLength = blockLength;
	}
	
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
}

/*
 * Class that represents a directory structure associated with a volume. It
 * consists of filenames and their associated FileControlBlocks, and the freeBlocks
 * BitSet for managing free blocks. 
 */
class DirectoryStructure implements java.io.Serializable {
	// Key = Filename, Value = FileControlBlock
	private HashMap<String, FileControlBlock> fcbMap;
	/* Each bit represents a block. If the bit is set (i.e. 1) it is in use. 
	 * If the bit is not set (i.e. 0), it is free.
	 */
	private BitSet freeBlocks;
	
	public DirectoryStructure() {
		fcbMap = new HashMap<String, FileControlBlock>();
		freeBlocks = new BitSet(FileVolume.MAX_BLOCKS); // Create a bit set of 40 bits set to 0
	}
	
	/*
	 * Creates a metadata for a file with the given name and size.
	 * Returns the block number where the data will be stored in the volume.
	 * 
	 * Throws IllegalArgumentException if a file already exists
	 * Throws IndexOutOfBoundsException if no space in this volume to store this file
	 */
	public ArrayList<Integer> createFileMetadata(String fileName, int fileSize) {
		if (fcbMap.get(fileName) != null) {
			// File already exists
			throw new IllegalArgumentException("File " + fileName + " already exists in volume");
		}
		
		// Find the number of free blocks that can hold this file
		int numberOfBlocksNeeded = (fileSize / FileVolume.BLOCK_SIZE) + (fileSize % FileVolume.BLOCK_SIZE == 0 ? 0 : 1);
		System.out.println("Number of blocks needed = " + numberOfBlocksNeeded);
		int nextFreeBlock = freeBlocks.nextClearBit(0);
		System.out.println("Next free block = "+ nextFreeBlock);
		while (nextFreeBlock < 40 && (nextFreeBlock + numberOfBlocksNeeded - 1 < 40)) {
			BitSet temp = freeBlocks.get(nextFreeBlock, nextFreeBlock + numberOfBlocksNeeded); // For 22 bytes only 1 bit, 256 bytes 2 bits
			if (temp.cardinality() == 0) {
				ArrayList<Integer> blocksToUse = new ArrayList<Integer>();
				for (int i=nextFreeBlock; i < nextFreeBlock + numberOfBlocksNeeded; i++) {
					freeBlocks.set(i);
					blocksToUse.add(i);
				}
				FileControlBlock fcb = new FileControlBlock(fileSize, nextFreeBlock, numberOfBlocksNeeded);
				fcbMap.put(fileName, fcb);
				System.out.println("Creating Blocks for " + fileName + "(" + fileSize + "): " + blocksToUse.toString());
				return blocksToUse;
			}
		}
		
		// No Free Block available
		System.out.println("No blocks available for " + fileName + "(" + fileSize + ": " + freeBlocks.toString());
		throw new IndexOutOfBoundsException("No free blocks available for " + fileName + "(" + fileSize + "): " + freeBlocks.toString());
	}
	
	public void deleteFileMetadata(String fileName) {
		FileControlBlock curFcb = fcbMap.get(fileName);
		int blockStart = curFcb.getBlockStart();
		int blockLength = curFcb.getBlockLength();
		System.out.println("Deleting Blocks for " + fileName + "(" + curFcb.getFileSize() + "): " + blockStart + ", length = " + blockLength);
		// Resets the bits associated with the file blocks back to zero (So that the block is considered available)
		freeBlocks.clear(blockStart, blockStart + blockLength);
		// Deletes the file from fcbMap
		fcbMap.remove(fileName);
	}
	
	public FileControlBlock getFileMetadata(String fileName) {
		return fcbMap.get(fileName);
	}
	
	public HashMap<String, FileControlBlock> listFileMetadata() {
		// Returns all FileControlBlocks (i.e. values) from the fcbMap
		return fcbMap;
	}

	@Override
	public String toString() {
		return "DirectoryStructure [fcbMap=" + fcbMap + ", freeBlocks="
				+ freeBlocks + "]";
	}
}

class DataBlock {
	private byte[] content; // A single data block is of size BLOCK_SIZE (256 bytes)
	
	public DataBlock() {
		content = new byte[FileVolume.BLOCK_SIZE];
		for (int i=0; i < FileVolume.BLOCK_SIZE; i++) {
			content[i] = 0;
		}
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}
}

public class FileVolume {
	private DirectoryStructure ds; // Metadata containing FileControlBlocks
	private DataBlock[] blocks; // 40 (10 KB / 256 bytes)
	private String volumePath; // For example: "/tmp/"
	private String volumeFileName; // For example: abc
	public static final int VOL_SIZE = 10240; // 10KB in bytes
	public static final int BLOCK_SIZE = 256; // bytes
	public static final int MAX_BLOCKS = (VOL_SIZE / BLOCK_SIZE);
	
	public FileVolume() {
		blocks = new DataBlock[MAX_BLOCKS];
		for (int i=0; i < MAX_BLOCKS; i++) {
			blocks[i] = new DataBlock();
		}
	}

	public FileVolume(String volumePath, String volumeFileName) {
		ds = new DirectoryStructure();
		blocks = new DataBlock[MAX_BLOCKS];
		for (int i=0; i < MAX_BLOCKS; i++) {
			blocks[i] = new DataBlock();
		}
		this.volumeFileName = volumeFileName;
		this.volumePath = volumePath;
	}
	
	public static FileVolume getFileVolumeFromFile(String volumePath, String volumeFileName) throws IOException, FileNotFoundException, ClassNotFoundException {
		// Reads the volume file and constructs the FileVolume object
		FileVolume fv = new FileVolume();
		fv.volumePath = volumePath;
		fv.volumeFileName = volumeFileName;
		
		FileInputStream fileIn = new FileInputStream(volumePath + "/" + volumeFileName + ".meta");
		ObjectInputStream objectIn = new ObjectInputStream(fileIn);
		DirectoryStructure ds = (DirectoryStructure) objectIn.readObject();
		
		fv.ds = ds;
		
		FileInputStream fis = new FileInputStream(volumePath + "/" + volumeFileName);
		for (DataBlock block : fv.getBlocks()) {
			fis.read(block.getContent());
		}
		return fv;
	}
	
	public void writeVolumeToFile() throws IOException {
		// Writes the current state of the object to the volumeFile
		// Write ds to .volumeFileName.meta and write data blocks to volumeFileName
		FileOutputStream fileOut = new FileOutputStream(this.volumePath + "/" + this.volumeFileName + ".meta");
		ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
		objectOut.writeObject(this.ds);
		objectOut.close();
		
		FileOutputStream fos = new FileOutputStream(this.volumePath + "/" + this.volumeFileName);
		for (DataBlock block : this.blocks) {
			fos.write(block.getContent());
		}
	}
	
	/*
	 * Creates a file in the PFS filesystem. Reads it from the OS and copies to PFS fileSystem.
	 * 
	 * Throws IllegalArgumentException if a file already exists
	 * Throws IndexOutOfBoundsException if no space in this volume to store this file
	 */
	public void createFile(String filePath, String fileName) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {		
		// Open the file in filePath (OS), read the bytes associated with it and copy it to datablocks
		File userFile = new File(filePath + "/" + fileName);
		byte[] userFileByes = Files.readAllBytes(userFile.toPath());
		int offset = 0;
		int bytesToCopy = userFileByes.length;
		
		// Get the block numbers needed to store the file
		ArrayList<Integer> blocksToUse = ds.createFileMetadata(fileName, bytesToCopy);
		for (int blockNum : blocksToUse) {
			System.arraycopy(userFileByes, offset, blocks[blockNum].getContent(), 0, Math.min(256, bytesToCopy));
			offset += 256;
			bytesToCopy -= 256;
		}
		
		// Finally write the volume object to file so that it is persisted
		writeVolumeToFile();
	}
	
	public void deleteFile(String fileName) throws IOException {
		// Only remove the metadata from the DirectoryStructure.
		// The datablocks need not be reset to 0 as it is anyway unused
		
		ds.deleteFileMetadata(fileName);
		// Finally write the volume object to file so that it is persisted
		writeVolumeToFile();
	}
	
	public byte[] getFile(String fileName) {
		// Get the bytes associated with a file
		// The caller will create a file in the OS
		FileControlBlock fcb = ds.getFileMetadata(fileName);
		if (fcb == null) {
			//System.out.println("File " + fileName + " does not exist in " + volumePath + "/" + volumeFileName);
			return null;
		}
		int blockStart = fcb.getBlockStart();
		int blockEnd = blockStart + fcb.getBlockLength() - 1;
		int fileSize = fcb.getFileSize();
		
		byte[] userFileBytes = new byte[fileSize];
		int offset = 0;
		int bytesToRead = fileSize;
		
		for (int block=blockStart; block <= blockEnd; block++) {
			System.arraycopy(blocks[block].getContent(), 0, userFileBytes, offset, Math.min(256, bytesToRead));
			offset += 256;
			bytesToRead -= 256;
		}
		System.out.println("Reading file of size " + fileSize + " from block " + blockStart + " to " + blockEnd + " in volume " + volumeFileName);
		return userFileBytes;
	}
	
	public HashMap<String, FileControlBlock> listFiles() {
		return ds.listFileMetadata();
	}

	public DirectoryStructure getDs() {
		return ds;
	}

	public DataBlock[] getBlocks() {
		return blocks;
	}

	public String getVolumeFileName() {
		return volumeFileName;
	}
	
	public String getVolumePath() {
		return volumePath;
	}

	public void setDs(DirectoryStructure ds) {
		this.ds = ds;
	}

	public void setBlocks(DataBlock[] blocks) {
		this.blocks = blocks;
	}

	public void setVolumeFileName(String volumeFileName) {
		this.volumeFileName = volumeFileName;
	}

	@Override
	public String toString() {
		return "FileVolume [ds=" + ds + ", blocks=" + Arrays.toString(blocks)
				+ ", volumePath=" + volumePath + ", volumeFileName="
				+ volumeFileName + "]";
	}
	
}
