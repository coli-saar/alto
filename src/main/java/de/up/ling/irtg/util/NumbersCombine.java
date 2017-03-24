/**
 *
 */
package de.up.ling.irtg.util;

/**
 * This method can be used to combine integers / shorts into longs / integers in
 * order to represent pairs of these elements
 *
 * @author Christoph Teichmann created Jun 30, 2013 4:28:01 PM
 * @version 1.0
 */
public class NumbersCombine {

    /**
     * a suffix used to ensure that the first 32 bits of a long can be set to 0
     */
    private final static long LSUFFIX = (-1L >>> Integer.SIZE);

    /**
     * a suffix used to ensure that the first 32 bits of an int can be set to 0
     */
    private final static int ISUFFIX = (-1 >>> Short.SIZE);

    /**
     * combines the two given numbers into a long that is unique for the two
     * numbers and their order
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static long combine(int arg1, int arg2) {
        // cast first number to long
        long ret = arg1;
	// remove possible leading 1s from second element and then shift the bits of the first 
        // element to the left for the amount of bits in an int then takes the OR of the two resulting elements
        ret = (ret << Integer.SIZE) | ((arg2) & LSUFFIX);
        return ret;
    }

    /**
     * combines the two given numbers into an int that is unique for the two
     * numbers and their order
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static int combine(short arg1, short arg2) {
        // cast first number to int
        int ret = arg1;
	// remove possible leading 1s from second element and then shift the bits of the first 
        // element to the left for the amount of bits in an short then takes the OR of the two resulting elements
        ret = (ret << Short.SIZE) | ((arg2) & ISUFFIX);
        return ret;
    }

    /**
     * returns the unique int i that has some number j such that code =
     * combine(i,j)
     *
     * @param code
     * @return
     */
    public static int getFirst(long code) {
        return (int) (code >>> Integer.SIZE);
    }

    /**
     * returns the unique int i that has some number j such that code =
     * combine(j,i)
     *
     * @param code
     * @return
     */
    public static int getSecond(long code) {
        return (int) code;
    }

    /**
     * returns the unique short i that has some number j such that code =
     * combine(i,j)
     *
     * @param code
     * @return
     */
    public static short getFirst(int code) {
        return (short) (code >>> Short.SIZE);
    }

    /**
     * returns the unique short i that has some number j such that code =
     * combine(j,i)
     *
     * @param code
     * @return
     */
    public static short getSecond(int code) {
        return (short) code;
    }
    
}
