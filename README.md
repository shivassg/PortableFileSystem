# PortableFileSystem
A Portable File System using Contiguous Allocation Method


PFS implements a FileSystem using the contiguous allocation method. It uses volume files of 10KB size to create and move files between the OS and this PFS. 

This project is implemented in Java.

Each volume (for ex: abc, abc.1, abc.2) is represented by the FileVolume class object
A FileVolume consists of DirectoryStructure and DataBlocks.
  - Each volume is of size 10KB which is divided into 40 blocks of 256 byte each (as block size is 256 bytes). The data blocks is written to the volume.
  - When a file cannot be stored in an existing volume, a new volume is created. For ex: abc.1, abc.2 etc.,

FileControlBlock : Class for File Control block. It has the metadata associated with the file

DirectoryStructure: Class that represents a directory structure associated with a volume. It consists of filenames and their associated FileControlBlocks, and the freeBlocks. BitSet for managing free blocks.

DataBlock: Datablock represents a single block in the volume. It contains byte array of 256 bytes which is used for holding the file contents. 

class FileSystem has the following methods:

open: This opens existing volumes with the given name, if it already exists. Otherwise it creates a new volume with the given name.

put: This method is used to put a user file from the OS to an available file Volume.

get: This method gets a user file from the PFS file system and creates a new file in the OS current directory

rm: This method is used to remove a user file from the PFS file system.

dir: This method is list all the files currently inside the PFS filesystem.

putr: This method is used to add remarks to an existing file in the PFS
kill: This method is used to delete all associated volumes.
getUserInput: This is method is used to get the user input.
