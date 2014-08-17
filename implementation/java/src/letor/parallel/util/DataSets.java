package letor.parallel.util;

/**
 * Holds metadata elements for used data sets
 * Created by niek.tax on 6/26/2014.
 */
public class DataSets {
    public static enum DataSet {
        OHSUMED, MQ2007, MQ2008, MSLR_WEB10K,
        MSLR_WEB30K, CUSTOM_2, CUSTOM_10
    }

    public static Metadata getMetaData(DataSet ds){
        Metadata value = null;
        switch(ds){
            case OHSUMED:
                value = OHSUMED;
                break;
            case MQ2007:
                value = MQ2007;
                break;
            case MQ2008:
                value = MQ2008;
                break;
            case MSLR_WEB10K:
                value = MSLR_WEB10K;
                break;
            case MSLR_WEB30K:
                value = MSLR_WEB30K;
                break;
            case CUSTOM_2:
                value = CUSTOM_2;
                break;
            case CUSTOM_10:
                value = CUSTOM_10;
                break;
            default:
                System.err.print("Dataset unknown!");
        }
        return value;
    }

    public static Metadata OHSUMED     = new Metadata("ohsumed",     45,  5151958L,     1764005L,   1764005L);
    public static Metadata MQ2007      = new Metadata("MQ2007",      46,  25820919L,    8753466L,   8753466L);
    public static Metadata MQ2008      = new Metadata("MQ2008",      46,  5927007L,     2237346L,   2237346L);
    public static Metadata MSLR_WEB10K = new Metadata("MSLR-WEB10K", 136, 838011150L,   280714022L, 280714022L);
    public static Metadata MSLR_WEB30K = new Metadata("MSLR-WEB30K", 136, 2624103186L,  878653757L, 8788653757L);
    public static Metadata CUSTOM_2    = new Metadata("Custom-2",    136, 4613302631L,  871874450L, 871874450L);
    public static Metadata CUSTOM_10   = new Metadata("Custom-10",   136, 23085163023L, 871874450L, 871874450L);
}
