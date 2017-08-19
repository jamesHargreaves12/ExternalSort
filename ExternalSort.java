package uk.ac.cam.jh2045.fjava.tick0;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.PriorityQueue;

public class ExternalSort {
	public static byte[] radixSort(byte[] original)
	{
		
		 //perform radix sort per byte with counting sort for the internal sort.
		 int count[];
		 byte newBytes[];
		 for(int byteNum = 3; byteNum >= 0; byteNum --)
		 {
			 count = new int[256];
			 for(int num = 0; num < original.length/4; num++)
			 {
				 if(byteNum == 0) 
					 count[original[4*num + byteNum]+128] += 1;
				 else if(original[4*num + byteNum]>=0)
					 count[original[4*num + byteNum]] += 1;
				 else 
					 count[256+original[4*num + byteNum]] += 1;
			 }

			 //cumulative
			 int cumulative = 0;
			 int temp =0;
			 for(int i = 0; i < 256; i ++)
			 {
				 temp = cumulative;
				 cumulative += count[i]*4;
				 count[i] = temp;
			 }

			 newBytes = new byte[original.length];
			 int location;
			 for(int num = 0; num < original.length/4; num++)
			 {
				 if(byteNum == 0) 
					 location = original[4*num + byteNum]+128;
				 else if(original[4*num + byteNum]>=0)
					 location = original[4*num + byteNum];
				 else 
					 location = 256+original[4*num + byteNum];

					newBytes[count[location]] = original[4*num];
					newBytes[count[location]+1] = original[4*num+1];
					newBytes[count[location]+2] = original[4*num+2];
					newBytes[count[location]+3] = original[4*num+3];
					count[location] += 4;
			 }
			 original = newBytes;
			 newBytes = null;
			 count = null;
		 }
		return original;
	}	
	
	public static void sortInitial(RandomAccessFile f,DataOutputStream dg, int initialSize) throws IOException
	{
		long leftpointer = 0;
		int length;
		byte bytes[];
		while(leftpointer < f.length())
		{
			if(f.length() - leftpointer> initialSize)bytes = new byte[initialSize];
			else bytes = new byte[(int) (f.length()-leftpointer)];

			f.seek(leftpointer);
			length = f.read(bytes);
			dg.write(radixSort(bytes));

			leftpointer += length;
			bytes = null;
		}
		 dg.flush();
	}
	
	public static void OnePassMergeSort(RandomAccessFile in, DataOutputStream out, long sortedSectionSize)
	{
		/*assume that RAM > 8192*numSortedBlocks 
		 * sorted blocks have size RAM 
		 * and so fileSize = numSortedBlocks*RAM
		 * so works for all file sizes < RAM^2/8192, Seems reasonable.
		 * */  
		try {
			//PriorityQueue<Pair> minHeap = new PriorityQueue<Pair>();
			
			
			final int numberSortedSections = (int) (in.length()/sortedSectionSize)+1;
			long[] startPointers = new long[numberSortedSections]; // records start of each sorted section
			long[] toGoBlocks = new long[numberSortedSections]; // records number of 8192 blocks still unread on Disk for section
			byte[][] blocks = new byte[numberSortedSections][8192]; // stores the 8192 blocks
			int[] blockPointers = new int[numberSortedSections]; // records pointer location in block
			int[] blockSize = new int[numberSortedSections]; // record how much of the 8192 is current block - may not use entire 
			int[] initialMins = new int[numberSortedSections]; //required for the min heap 
			//initialise values;
			for(int i =0; i < numberSortedSections; i ++)
			{
				startPointers[i] = sortedSectionSize*i; 
				in.seek(sortedSectionSize*i);
				int length = in.read(blocks[i]);
				if(i != numberSortedSections-1)
					toGoBlocks[i] = sortedSectionSize/8192-1;
				else
					toGoBlocks[i] = (in.length()-sortedSectionSize*i)/8192;
				blockSize[i] = length;
				startPointers[i] += length;
				int val = ((blocks[i][blockPointers[i]+3] & 0xFF) | ((blocks[i][2 + blockPointers[i]] & 0xFF) << 8) |
		                  ((blocks[i][1 + blockPointers[i]] & 0xFF) << 16) | ((blocks[i][0 + blockPointers[i]] & 0xFF) << 24));
				initialMins[i] = val;
			}
			MinimumHeap heap = new MinimumHeap(initialMins);			
			
			
			while(!heap.isEmpty())
			{
				//select smallest value and add to output buffer
				int smallestBlock = heap.minBlock();
				int smallestBlockPointer = blockPointers[smallestBlock];
				byte[] entireOfSmallestBlock = blocks[smallestBlock];
				int smallestVal = ((entireOfSmallestBlock[smallestBlockPointer+3] & 0xFF) | ((entireOfSmallestBlock[2 + smallestBlockPointer] & 0xFF) << 8) |
		                  ((entireOfSmallestBlock[1 + smallestBlockPointer] & 0xFF) << 16) | ((entireOfSmallestBlock[0 + smallestBlockPointer] & 0xFF) << 24));

				out.writeInt(smallestVal);
				
				blockPointers[smallestBlock] += 4;
				if(smallestBlockPointer >= blockSize[smallestBlock]-4)
				{
					if(toGoBlocks[smallestBlock] > 0)
					{
						// space left
						toGoBlocks[smallestBlock] --; 
						in.seek(startPointers[smallestBlock]);
						int length = in.read(blocks[smallestBlock]);
						blockSize[smallestBlock] = length;
						startPointers[smallestBlock] += length;
						blockPointers[smallestBlock] = 0;
						heap.replace((blocks[smallestBlock][3] & 0xFF) | ((blocks[smallestBlock][2] & 0xFF) << 8) |
				                  ((blocks[smallestBlock][1] & 0xFF) << 16) | ((blocks[smallestBlock][0] & 0xFF) << 24));
					}
					else
					{
						heap.deleteHead();
					}
				}
				else
				{
					smallestBlockPointer += 4;
					heap.replace((blocks[smallestBlock][smallestBlockPointer+3] & 0xFF) | ((blocks[smallestBlock][2 + smallestBlockPointer] & 0xFF) << 8) |
			                  ((blocks[smallestBlock][1 + smallestBlockPointer] & 0xFF) << 16) | ((blocks[smallestBlock][0 + smallestBlockPointer] & 0xFF) << 24));
				}
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void sort(String f1, String f2) throws FileNotFoundException, IOException {
		RandomAccessFile f = new RandomAccessFile(f1,"rw");
		RandomAccessFile g = new RandomAccessFile(f2,"rw");		
		DataOutputStream dg = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(g.getFD())));
		//have assumed that the file is length 4.x - ie can be wrapped into a sequence of  integers
		
		// next bit works out best size for the initial sort in memory 
		//- no IO advantage to big reads since read in in blocks anyway - make size a multiple of block size
		// however since sorting in RAM is relatively fast want to sort as much as possible in RAM before continuing to the external sort
		// we -2 since using radix sort doesnt sort in place;
		long freeMem = Runtime.getRuntime().freeMemory();
		int power = (int) Math.floor(Math.log10(freeMem)*3.32);
		long sizeOfRead = 8192;
		for(int i = 13; i < power-2; i++)
		{
			sizeOfRead*=2;
		}
		
		//perform the initial sort in Memory
		sortInitial(f,dg,(int) sizeOfRead);
		f.seek(0);
		g.seek(0);
		DataOutputStream dataOutputLocation = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(f.getFD())));
		
		if(f.length() > sizeOfRead)
		{
			//still sorting to be peformed 
			OnePassMergeSort(g, dataOutputLocation, sizeOfRead);
		}
		else
		{
			//simple copy is required
			byte[] buffer = new byte[8192];
			int length = g.read(buffer);
			while(length > 0)
			{
				dataOutputLocation.write(buffer,0,length);
				length = g.read(buffer);
			}
		}
		
		dataOutputLocation.close();
		
		f.close();
		g.close();

	}


	private static String byteToHex(byte b) {
		String r = Integer.toHexString(b);
		if (r.length() == 8) {
			return r.substring(6);
		}
		return r;
	}

	public static String checkSum(String f) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			DigestInputStream ds = new DigestInputStream(
					new FileInputStream(f), md);
			byte[] b = new byte[512];
			while (ds.read(b) != -1)
				;

			String computed = "";
			for(byte v : md.digest()) 
				computed += byteToHex(v);

			return computed;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "<error computing checksum>";
	}

	public static void main(String[] args) throws Exception 
	{
		long before = System.currentTimeMillis();
		for(int i = 17; i <= 17; i ++)
		{
			String f1 = "data/test" + i + "a.dat";
			String f2 = "data/test" + i + "b.dat";
			sort(f1, f2);
			System.out.println("The checksum is: "+checkSum(f1));
		}
		long after = System.currentTimeMillis();
		System.out.println(after - before);
	}
}
