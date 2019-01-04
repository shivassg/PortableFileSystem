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


Sample Output
-------------
PFS>open abc
PFS>put /tmp/small_input.txt
Checking volume abc...
Number of blocks needed = 1
Next free block = 0
Creating Blocks for small_input.txt(22): [0]
PFS>put /tmp/big_input.txt
Checking volume abc...
Number of blocks needed = 4
Next free block = 1
Creating Blocks for big_input.txt(1022): [1, 2, 3, 4]
PFS>put /tmp/very_big_input.txt
Checking volume abc...
Number of blocks needed = 36
Next free block = 5
No blocks available for very_big_input.txt(9162: {0, 1, 2, 3, 4}
Creating new volume abc.1...
Number of blocks needed = 36
Next free block = 0
Creating Blocks for very_big_input.txt(9162): [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35]
PFS>dir
small_input.txt	22 bytes 	02-12-2018 19:49:23	
big_input.txt	1022 bytes 	02-12-2018 19:49:35	
very_big_input.txt	9162 bytes 	02-12-2018 19:50:06	
PFS>putr small_input.txt Small Input
Reading file of size 22 from block 0 to 0 in volume abc
PFS>putr big_input.txt Big Input
Reading file of size 1022 from block 1 to 4 in volume abc
PFS>putr very_big_input.txt Very Big Input
Reading file of size 9162 from block 0 to 35 in volume abc.1
PFS>dir
small_input.txt	22 bytes 	02-12-2018 19:49:23	Small Input
big_input.txt	1022 bytes 	02-12-2018 19:49:35	Big Input
very_big_input.txt	9162 bytes 	02-12-2018 19:50:06	Very Big Input
PFS>get small_input.txt
Reading file of size 22 from block 0 to 0 in volume abc
PFS>get big_input.txt
Reading file of size 1022 from block 1 to 4 in volume abc
PFS>get very_big_input.txt
Reading file of size 9162 from block 0 to 35 in volume abc.1
PFS>rm big_input.txt
Reading file of size 1022 from block 1 to 4 in volume abc
Deleting Blocks for big_input.txt(1022): 1, length = 4
PFS>dir
small_input.txt	22 bytes 	02-12-2018 19:49:23	Small Input
very_big_input.txt	9162 bytes 	02-12-2018 19:50:06	Very Big Input
PFS>kill abc
PFS>exit
