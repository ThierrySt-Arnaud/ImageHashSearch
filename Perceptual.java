// Thierry St-Arnaud
// 27649460

// Receives an array of doubles and performs a perceptual hash on it.
// The resulting hash is put in the received BitSet.
// This will modify the received image array.

import java.util.*;

public class Perceptual{
	public static void Hash(double[] image, int hashLength, BitSet hash) throws IllegalArgumentException{
		int n = image.length;
		// Hash must be a perfect square
		if (hashLength < 4 || !isSquare(hashLength))
			throw new IllegalArgumentException("Invalid hash length.");
		// And image length should be a perfect square, power of 2, larger than hash length
		else if ( Integer.bitCount(n) != 1 || n < hashLength || !isSquare(n))
			throw new IllegalArgumentException("Invalid image length.");

		// Call the in-place Fast DCT algorithm 
		FastDctFft.transform(image);

		// Then window the DCT to hashlength
		double[] windowedDCT = new double[hashLength];
		window(image, hashLength, windowedDCT);

		// And compute the final hash
		compute(windowedDCT, hash);
	}

	private static void window(double[] imageDCT, int hashLength, double[] windowed){
		int hashWidth = (int) Math.sqrt(hashLength);
		int DCTwidth = (int) Math.sqrt(imageDCT.length);

		// Get low horizontal, vertical and diagonal frenquencies
		// of the DCT, skipping over the DC component
		for (int i = 0; i < hashLength-1;i++){
			int j = DCTwidth*Math.floorDiv(i+1, hashWidth)+(i+1)%hashWidth;
			windowed[i] = imageDCT[j];
		}
		// Then put a last diagonal frequency
		int j = hashWidth*DCTwidth+hashWidth;
		windowed[hashLength-1] = imageDCT[j];
	}

	private static void compute(double[] windowedDCT, BitSet hash){
		int hashLength = windowedDCT.length;
		double avg = 0;
		// To compute the hash, we first compute the average
		for (int i = 0; i < hashLength; i++)	avg += windowedDCT[i];
		avg /= (double) hashLength;

		// Then, for each coefficient that is higher than
		// the average, we set the corresponding bit.
		for (int i = 0; i < hashLength; i++){
			if (windowedDCT[i] > avg)
				hash.set(i);
		}
	}

	// This perfect square check algorithm is taken from a stackoverflow answer
	// https://stackoverflow.com/questions/295579/fastest-way-to-determine-if-an-integers-square-root-is-an-integer
	// It is certainly very fast, but it is a FAIL FAST algorithm, which assumes that 
	// we'll get a random values and only a few of them are squares. In this case,
	// we expect a large number of perfect squares and use this to check for input
	// correctness. I have yet to find a PASS FAST algorithm.
	static long squareMask = 0; // JVM dependent, but normally 0xC840C04048404040, computed below
	public static boolean isSquare(long x) {
		// If squareMask not computed yet, do it now
		if (squareMask == 0){
	    	for (int i=0; i<64; ++i) squareMask |= Long.MIN_VALUE >>> (i*i);
		}
	    // This tests if the 6 least significant bits are right.
	    // Moving the to be tested bit to the highest position saves us masking.
	    if (squareMask << x >= 0) return false;
	    final int numberOfTrailingZeros = Long.numberOfTrailingZeros(x);
	    // Each square ends with an even number of zeros.
	    if ((numberOfTrailingZeros & 1) != 0) return false;

	    x >>= numberOfTrailingZeros;
	    // Now x is either 0 or odd.
	    // In binary each odd square ends with 001.
	    // Postpone the sign test until now; handle zero in the branch.
	    if ((x&7) != 1 | x <= 0) return x == 0;
	    // Do it in the classical way.
	    // The correctness is not trivial as the conversion from long to double is lossy!
	    final long tst = (long) Math.sqrt(x);
	    return tst * tst == x;
	}
}