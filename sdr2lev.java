//  sdr2lev.java
//  convert SDR2x file to 'lev' format

import java.io.*;
import java.util.*;
import java.text.*;

public class sdr2lev{
	protected BufferedReader in;
	protected LinkedList<String> oRawData, observationRows;
    protected String sampleRow = "                                                              10.00000    10.00000";

	//  specification of digital level
	protected double maxDist, minDist, staffLength;

	//  constructor
	public sdr2lev(String sFileName){
		try{
			in = new BufferedReader(new FileReader(sFileName));

			Properties prop = new Properties();
			prop.load(new BufferedReader(new FileReader("config.txt")));

			maxDist = Double.parseDouble(prop.getProperty("maxDist"));
			minDist = Double.parseDouble(prop.getProperty("minDist"));
			staffLength = Double.parseDouble(prop.getProperty("staffLength"));

			//  test coding
			//  System.out.println(maxDist + "\t" + minDist + "\t" + staffLength);

		}catch(Exception ex){
			System.out.println(ex);
			System.exit(0);
		}
	}

	public void filterRawDate(){
		try{
			oRawData = new LinkedList<String>();

			String sLine = null;
			while((sLine=in.readLine())!=null){
				//  skip the 'mumbel jumble'
				if(sLine.startsWith("63LV")){
					//  System.out.println(sLine);
					oRawData.add(sLine);
				}
			}

		}catch(Exception ex){
			System.out.println(ex);
			System.exit(0);
		}
	}

	public void printRawDate(){
		for(int index=0; index<oRawData.size(); index++){
			System.out.println(oRawData.get(index));
		}
	}

	protected String getDescription(String sLine){
		//  columns 29-44 Description (BS=backsight)
		return sLine.substring(28, 30);
	}

    protected String getPointId(String sLine){
        return cutLeadingZeros(sLine.substring(4, 8));
    }

    protected String cutLeadingZeros(String sLine){
        if(sLine.startsWith("0")){
            return cutLeadingZeros(sLine.substring(1));
        }else{
            return new String(sLine);
        }
    }

    protected String getDistance(String sLine){
    	//  columns 9~18 Distance
        double dist = Double.parseDouble(sLine.substring(8, 18));
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(dist);
    }

    protected String getStaffReading(String sLine){
    	//  columns 19~28 staff reading
        double staffReading = Double.parseDouble(sLine.substring(18, 28));
        DecimalFormat df = new DecimalFormat("0.00000");
        return df.format(staffReading);
    }

	public boolean checkRawData(){
		boolean result = true;

		if(!getDescription(oRawData.getFirst()).equalsIgnoreCase("BS")){
			System.out.println("Error! First element is not Back Sight!");
			result = false;
		}

		if(!getDescription(oRawData.getLast()).equalsIgnoreCase("FS") && !getDescription(oRawData.getLast()).equalsIgnoreCase("FI")){
			System.out.println("Error! Last element is not Fore Sight!");
			result = false;
            return result;
		}

        for(int index=0; index<oRawData.size()-1; index++){
            if(getDescription(oRawData.get(index)).equalsIgnoreCase("BS")){
                //  two consecutive BS
                if(getDescription(oRawData.get(index+1)).equalsIgnoreCase("BS")){
                    System.out.println("Error! two consecutive BS!");
                    result = false;
                }
            }

            if(getDescription(oRawData.get(index)).equalsIgnoreCase("IS")){
                if(getDescription(oRawData.get(index+1)).equalsIgnoreCase("BS")){
                    System.out.println("Error! a BS was found after an IS!");
                    result = false;
                }
            }

            if(getDescription(oRawData.get(index)).equalsIgnoreCase("FS") || getDescription(oRawData.get(index)).equalsIgnoreCase("FI")){
                if(!getDescription(oRawData.get(index+1)).equalsIgnoreCase("BS")){
                    System.out.println("Error! a FS was not followed by a BS!");
                    result = false;
                }
            }
        }

		return result;
	}

    void packaging(){
        //  pack the observations into ESCS *.lev format
        observationRows = new LinkedList<String>();

        String firstRow = insertBS(oRawData.getFirst(), "BS" + sampleRow);

        String lastRow = insertFS(oRawData.getLast(), "FS" + sampleRow);

        //  test coding
        System.out.println(firstRow);
        //  System.out.println(lastRow);

        int index = 1;
        while(index<oRawData.size()-1){
            String sRawData = oRawData.get(index);
            if(getDescription(sRawData).equalsIgnoreCase("FS") || getDescription(sRawData).equalsIgnoreCase("FI")){
                String fieldBookRow = insertFS(sRawData, "CP" + sampleRow);

                index++;
                sRawData = oRawData.get(index);
                fieldBookRow = insertBS(sRawData, fieldBookRow);

                //  test coding
                System.out.println(fieldBookRow);
            }

            if(getDescription(sRawData).equalsIgnoreCase("IS")){
                String fieldBookRow = insertIS(sRawData, "IS" + sampleRow);

                //  test coding
                System.out.println(fieldBookRow);
            }

            index++;
        }
        //  test coding
        System.out.println(lastRow);

    }

    protected String insertFS(String bsObservation, String orginalFieldBookRow){
    	StringBuffer fieldBookRow = new StringBuffer(orginalFieldBookRow);

    	String pointId = getPointId(bsObservation);
    	fieldBookRow.replace(3, 3 + pointId.length(), pointId);

    	String dist = getDistance(bsObservation);
    	fieldBookRow.replace(55, 55 + dist.length(), dist);

    	String staffReading = getStaffReading(bsObservation);
    	fieldBookRow.replace(45, 45 + staffReading.length(), staffReading);

    	return fieldBookRow.toString();
    }

    protected String insertBS(String bsObservation, String orginalFieldBookRow){
    	StringBuffer fieldBookRow = new StringBuffer(orginalFieldBookRow);

    	String pointId = getPointId(bsObservation);
    	fieldBookRow.replace(3, 3 + pointId.length(), pointId);

    	String dist = getDistance(bsObservation);
    	fieldBookRow.replace(16, 16 + dist.length(), dist);

    	String staffReading = getStaffReading(bsObservation);
    	fieldBookRow.replace(23, 23 + staffReading.length(), staffReading);

    	return fieldBookRow.toString();
    }

    protected String insertIS(String bsObservation, String orginalFieldBookRow){
        StringBuffer fieldBookRow = new StringBuffer(orginalFieldBookRow);

        String pointId = getPointId(bsObservation);
        fieldBookRow.replace(3, 3 + pointId.length(), pointId);

        String dist = getDistance(bsObservation);
        fieldBookRow.replace(55, 55 + dist.length(), dist);

        String staffReading = getStaffReading(bsObservation);
        fieldBookRow.replace(33, 33 + staffReading.length(), staffReading);

        return fieldBookRow.toString();
    }

	public static void main(String[] args)throws Exception{
		if(args.length<1){
			System.out.println("usage : java sdr2lev FileName");
			System.exit(0);
		}

		sdr2lev oLevFile = new sdr2lev(args[0]);
		oLevFile.filterRawDate();
		if(oLevFile.checkRawData()){
			//  oLevFile.printRawDate();
            oLevFile.packaging();
		}

		//  String sDesc = sLine.substring(28, 30);
		//  String sPointId = sLine.substring(4, 8);
		//  String sDist = sLine.substring(8, 18);
		//  String sRead = sLine.substring(18, 28);

	}
}
