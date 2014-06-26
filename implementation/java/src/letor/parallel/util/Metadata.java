package letor.parallel.util;

/**
 * Holds metadata of a dataset
 * Created by niek.tax on 6/26/2014.
 */
public class Metadata {
    private String  name;
    private int     dim;
    private long    max_train_size;
    private long    max_vali_size;
    private long    max_test_size;

    public Metadata(String name, int dim, long max_train_size, long max_vali_size, long max_test_size){
        this.name           = name;
        this.dim            = dim;
        this.max_train_size = max_train_size;
        this.max_vali_size  = max_vali_size;
        this.max_test_size  = max_test_size;
    }

    public String getName(){
        return name;
    }

    public int getDim(){
        return dim;
    }

    public long getMax_train_size(){
        return max_train_size;
    }

    public long getMax_vali_size(){
        return max_vali_size;
    }

    public long getMax_test_size(){
        return max_test_size;
    }
}
