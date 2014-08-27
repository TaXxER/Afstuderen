package udf.listnet.util;

import java.util.Comparator;

/**
 * Class containing a Document
 *
 * Created by niek.tax on 6/2/2014.
 */
public class Document implements Comparable<Document>{
    private double[] attributes;
    private int relevance;
    private double predicted;
    private double[] weights;

    public Document(int relevance, double predicted){
        this.relevance  = relevance;
        this.predicted  = predicted;
    }

    // Default comparator, sorts on relevance
    public int compareTo(Document other){
        return this.relevance > other.relevance ? -1 : this.relevance < other.relevance ? 1 : 0;
    }

    // Alternative comparator, sorts on predicted relevance
    public static Comparator<Document> getPredictedComparator() {
        return new Comparator<Document>() {
            public int compare(Document one, Document two){
               return one.predicted > two.predicted ? -1 : one.predicted < two.predicted ? 1 : 0;
            }
        };
    }

    public int getRelevance(){
        return relevance;
    }

    public String toString(){
        return "doc: "+relevance+" - "+predicted;
    }
}
