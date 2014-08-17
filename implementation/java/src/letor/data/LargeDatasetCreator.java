package letor.data;

import com.google.common.collect.HashMultimap;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Util file that creates a single fold of very large training data by duplicating MSLR-web30k fold 1
 *
 * Created by niek.tax on 7/29/2014.
 */
public class LargeDatasetCreator {
    // Class parameters
    private static int duplicationFactor = 10;

    // Global variables
    private static String dataFolderPath = "C:/Git-data/Afstuderen/implementation/java/input";
    private static String inputPath      = dataFolderPath+"/MSLR-WEB30K";
    private static String outputPath     = dataFolderPath+"/custom-"+duplicationFactor;

    public static void main(String[] args){
        System.out.println("Reading dataset");
        // First pass: retrieve set of qid's
        HashSet<Long> qidSet            = retrieveQids();

        System.out.println("Calculating new Query ID's");
        HashMultimap<Long, Long> qidMap = calculateNewQids(qidSet);

        System.out.println("Writing new dataset");
        // Second pass: write new qid's
        writeNewQids(qidMap);

        System.out.println("Done");
    }

    private static HashSet<Long> retrieveQids(){
        HashSet<Long>   qidSet = new HashSet<Long>();
        BufferedReader  reader = null;
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
                qidSet.add(Long.parseLong(qidString));
            }
            reader.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        return qidSet;
    }

    private static HashMultimap<Long, Long> calculateNewQids(HashSet<Long> qidSet){
        HashMultimap<Long, Long> qidMap = HashMultimap.create();
        long maxQid = 0;
        for(Long l:qidSet){
            if(l>maxQid)
                maxQid = l;
        }

        for(Long l:qidSet){
            LinkedList<Long> newQids = new LinkedList<Long>();
            for(int i=0; i<duplicationFactor; i++){
                newQids.add(l+i*maxQid);
            }
            qidMap.putAll(l, newQids);
        }
        return qidMap;
    }

    private static void writeNewQids(HashMultimap<Long, Long> newQids){
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(inputPath + "/Fold1/train.txt"));
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        try{
            writer = new BufferedWriter(new FileWriter(outputPath + "/Fold1/train.txt"));
        }catch(IOException e){
            e.printStackTrace();
        }

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Long qid = Long.parseLong(line.split(" ")[1].substring(4)); // Read qid
                for(Long newQid : newQids.get(qid)){
                    writer.write(getLine(line, newQid));
                }
            }
            reader.close();
            writer.flush();
            writer.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private static String getLine(String line, Long newQid){
        String[] lineParts = line.split(" ");
        lineParts[1] = lineParts[1].substring(0,4).concat(""+newQid);
        return StringUtils.join(lineParts);
    }
}
