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

    // Maximum relevance label, deduct from dataset
    private int max = 2;

    public Document(double[] attributes, double[] weights, int relevance){
        this.attributes = attributes;
        this.weights = weights;
        this.relevance  = relevance;

        // Calculate predictions
        this.predicted = 0.0;
        for(int i=0; i<attributes.length; i++){
            predicted += attributes[i]*weights[i];
        }
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
