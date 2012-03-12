/*
The software is licensed under a Creative Commons Attribution 3.0 Unported License.
Copyright (C) 2012 Timo Rantalainen
*/

/*
A class to convert a CSV-file to .mat file
Mat file format obtained from  http://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf on 12th of march 2012
javac WriteMat.java CSVReader.java
java WriteMat
*/
import java.io.*;
public class WriteMat{
	
	FileOutputStream writer = null;
	
	public static void main(String[] args){
		if (args.length < 2){
			System.out.println("Please give full .csv-file and .mat file path, e.g.:");
			System.out.println("java WriteMat H:/UserData/winMigrationBU/Deakin/David/sample.csv H:/UserData/winMigrationBU/Deakin/David/test.mat");
			System.out.println("Optionally a third agrument can be given for csv-file spearator, e.g. for a tab separated file \\t");
		}
		String separator = ",";
		if (args.length > 2){
			separator = args[2];
			System.out.println("Separator changed");
		}				
		CSVReader reader = new CSVReader(args[0],separator);
		WriteMat mainProgram = new WriteMat(args[1]);
		
		for (int i = 0;i<reader.columnHeadings.size();++i){
			/*Assign the data to an array...*/
			double[] columnOfData = new double[reader.columns.get(i).size()];
			for (int r = 0;r<columnOfData.length;++r){
				columnOfData[r] = reader.columns.get(i).get(r);
			}
			System.out.println(reader.columnHeadings.get(i));
			mainProgram.writeArray(columnOfData,reader.columnHeadings.get(i));
		}
		mainProgram.closeFile();
	}

	public WriteMat(String fileName){
		try{
			writer = new FileOutputStream(fileName);
			writeHeader();
		}catch (Exception err){System.out.println("Couldn't open "+err.toString());}
	}
	
	public void writeArray(double[] data, String varName){
		int varNameLength = varName.length();
		if (varNameLength%8 != 0){
			varNameLength+=8-varName.length()%8;
		}
		byte[] fileData = new byte[8+8+2*4+8+2*4+8+varNameLength+8+data.length*8];
		fileData = createData(fileData,data,varName);
		try{
			writer.write(fileData);
		}catch (Exception err){System.out.println("Couldn't write array "+err.toString());}
	}
	
	public void closeFile(){
		try{
			writer.close();
		}catch (Exception err){System.out.println("Couldn't close "+err.toString());}
	}
	private void writeHeader(){		
		byte[] headerData = new byte[128];
		String text = "Timo's CSV to mat script";
		try{
			byte[] textToWrite = text.getBytes("US-ASCII");
			//First four bytes need to be non-zero
			for (int i = 0;i<textToWrite.length;++i){		
				headerData[i] = textToWrite[i];
			}
		}catch (Exception err){System.out.println("Couldn't find US-ASCII encoding");}
		//Set flags
		long flag1;
		flag1 = 0x0100;
		headerData = putBytes(headerData,flag1,124,2);
		/*Check whether this needs to be MI or IM (IM = LE)*/
		headerData[126] = (byte) 73;	//I
		headerData[127] = (byte) 77;	//M
		try{
			writer.write(headerData);
		}catch (Exception err){System.out.println("Couldn't write "+err.toString());}
	}

	private byte[] createData(byte[] fileData,double[] data, String varName){
		
		int offset = 0;
		long dataType = 0;
		long dataSize = 0;
		int varNameLength = varName.length();
		if (varNameLength%8 != 0){
			varNameLength+=8-varName.length()%8;
		}
		
		/*Array miMATRIX*/
			/*tag*/
			dataType = 14;	//miMATRIX
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			dataSize = 8+8+8+8+8+varNameLength+8+data.length*8; //Check this size...
			fileData= putBytes(fileData,dataSize,offset,4);
			offset+=4;
		/*Array flags  2*4+2*4 bytes*/ 
			/*tag*/
			dataType = 6;	//miUINT32
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			dataSize = 2*4; //Check this size...
			fileData= putBytes(fileData,dataSize,offset,4);
			offset+=4;
			/*data*/
			/*Data type and flags (flags can be 0...)*/
			dataType = 6;	//Double Flag
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			//Insert 0s for undefined
			fileData= putBytes(fileData,0,offset,4);
			offset+=4;
		/*Dimension array 2*4+4*dims*/
			dataType = 5;	//miINT32
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			dataSize = 2*4; //Check this size...
			fileData= putBytes(fileData,dataSize,offset,4);
			offset+=4;
			/*data*/
			/*rows x cols*/
			dataType = 1;	//rows
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			dataType = data.length;	//cols
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
		/*Array name 16+stringLength*/
			dataType = 1;	//miINT8
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			dataSize = varName.length(); //Check this size...
			fileData= putBytes(fileData,dataSize,offset,4);
			offset+=4;
			/*data*/
			/*VarName*/
			try{
				byte[] tagData = varName.getBytes("US-ASCII"); //{0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28};
				fileData = putString(fileData,tagData,offset);
				System.out.println("tD lenght "+tagData.length+" varNameL "+varNameLength);
				offset = offset+varNameLength;
			}catch (Exception err){System.out.println("Couldn't find US-ASCII encoding");}
		/*Array 16+10*8*/
			/*Data type*/
			dataType = 9;	//miDOUBLE
			fileData= putBytes(fileData,dataType,offset,4);
			offset+=4;
			/*Data size*/
			dataSize = data.length*8;
			fileData= putBytes(fileData,dataSize,offset,4);
			offset+=4;
			/*DATA*/
			for (int i = 0; i< data.length ;++i){
				fileData = putDouble(fileData,data[i],offset);
				offset+=8;
			}
		return fileData;
	}
	
	private byte[] putBytes(byte[] fileData, long input, int offset,int noOfBytes){
		//System.out.println("Put Long");
		for (int i = 0; i < noOfBytes;++i){
			short temp =(short) ((input & (255L <<(8*i)))>>8*i);
			//System.out.println(temp);
			fileData[offset+i]  = (byte) temp;
		}
		return fileData;
	}
	
	private byte[] putString(byte[] fileData, byte[] input, int offset){
		//System.out.println("Put Long");
		for (int i = 0; i < input.length;++i){
			fileData[offset+i]  = input[i];
		}
		return fileData;
	}
	
	private byte[] putDouble(byte[] fileData,double input, int offset){
		//System.out.println("Put Double");
		for (int i = 0; i < 8;++i){
			short temp =(short) ((Double.doubleToRawLongBits(input) & (255L <<(8*i)))>>8*i);
			//System.out.println(temp);
			fileData[(int) offset+i]  = (byte) temp;
		}
		return fileData;
	}
}