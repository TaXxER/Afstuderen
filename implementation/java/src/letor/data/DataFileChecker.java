package letor.data;

import letor.parallel.util.DataSets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Counts occurrence of Query ID's in data file
 *
 * Created by niek.tax on 8/27/2014.
 */
public class DataFileChecker {
    // Initialize parameter
    private static final DataSets.DataSet DATASET = DataSets.DataSet.CUSTOM_10;

    // Global variables
    private static String dataFolderPath = "C:/Git-data/Afstuderen/implementation/java/input/";
    private static String inputPath      = dataFolderPath+DataSets.getMetaData(DATASET).getName();

    public static void main(String[] args){
        System.out.println("Reading dataset");

        SortedMap<Long, Long> counterMap = new TreeMap<Long, Long>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inputPath + "/Fold1/train.txt"));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String qidString = line.split(" ")[1]; // Read qid
                qidString = qidString.substring(4);    // remove "qid:" prefix
                Long longQid = Long.parseLong(qidString);
                if(counterMap.containsKey(longQid))
                    counterMap.put(longQid, counterMap.get(longQid) + 1);
                else
                    counterMap.put(longQid, 1L);
            }
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Reading completed");
        for(Long key: counterMap.keySet()){
            System.out.println("key: "+key+" occurrences: "+counterMap.get(key));
        }

    }
}
