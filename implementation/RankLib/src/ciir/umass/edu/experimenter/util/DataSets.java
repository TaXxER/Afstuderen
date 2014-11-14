package ciir.umass.edu.experimenter.util;

/**
 * Holds metadata elements for used data sets
 * Created by niek.tax on 6/26/2014.
 */
public class DataSets {
    public static enum DataSet {
        OHSUMED, TD2003, TD2004, NP2003, NP2004, HP2003, HP2004,
        MQ2007, MQ2008,
        MSLR_WEB10K, MSLR_WEB30K,
        CUSTOM, MINI
    }

    public static Metadata getMetaData(DataSet ds, int duplicationNumber){
        Metadata value = null;
        switch (ds) {
            case CUSTOM:
                value = new Metadata("custom", 136, 2306651316L * (duplicationNumber+1),  878653757L, 8788653757L);
                break;
            default:
                value = getMetaData(ds);
        }
        return value;
    }

    public static Metadata getMetaData(DataSet ds){
        Metadata value = null;
        switch(ds){
            case OHSUMED:
                value = OHSUMED;
                break;
            case TD2003:
                value = TD2003;
                break;
            case TD2004:
                value = TD2004;
                break;
            case NP2003:
                value = NP2003;
                break;
            case NP2004:
                value = NP2004;
                break;
            case HP2003:
                value = HP2003;
                break;
            case HP2004:
                value = HP2004;
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
            case MINI:
                value = MINI;
                break;
            default:
                System.err.println("ERROR: Dataset unknown!");
        }
        return value;
    }

    public static Metadata OHSUMED     = new Metadata("ohsumed",     45,  5151958L,     1764005L,   1764005L);
    public static Metadata TD2003      = new Metadata("TD2003",      0,   0L,           0L,         0L);
    public static Metadata TD2004      = new Metadata("TD2004",      0,   0L,           0L,         0L);
    public static Metadata NP2003      = new Metadata("TD2003",      0,   0L,           0L,         0L);
    public static Metadata NP2004      = new Metadata("TD2004",      0,   0L,           0L,         0L);
    public static Metadata HP2003      = new Metadata("HP2003",      0,   0L,           0L,         0L);
    public static Metadata HP2004      = new Metadata("HP2004",      0,   0L,           0L,         0L);
    public static Metadata MQ2007      = new Metadata("MQ2007",      46,  25820919L,    8753466L,   8753466L);
    public static Metadata MQ2008      = new Metadata("MQ2008",      46,  5927007L,     2237346L,   2237346L);
    public static Metadata MSLR_WEB10K = new Metadata("MSLR-WEB10K", 136, 838011150L,   280714022L, 280714022L);
    public static Metadata MSLR_WEB30K = new Metadata("MSLR-WEB30K", 136, 2624103186L,  878653757L, 8788653757L);
    public static Metadata MINI        = new Metadata("mini",        45,  143380L,      195505L,    195505L);
}