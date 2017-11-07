package database.conversion;

/**
 * Created by Thomas on 06.09.2015.
 */
public enum LGAFrequency {
    weekly, Monthly, Yearly;

    public static LGAFrequency indexToFrequency(int idx) {
        LGAFrequency LGAFrequency = null;
        if (idx == 0) {
            LGAFrequency = weekly;
        } else if (idx == 1) {
            LGAFrequency = Monthly;
        } else if (idx == 2) {
            LGAFrequency = Yearly;
        }
        return LGAFrequency;
    }

    public static int FrequencyToIndex(LGAFrequency LGAFrequency) {
        switch (LGAFrequency) {
            case weekly:
                return 0;
            case Monthly:
                return 1;
            case Yearly:
                return 2;
        }

        return -1;
    }


}
