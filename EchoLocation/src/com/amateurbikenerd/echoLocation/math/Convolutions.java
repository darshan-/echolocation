package com.amateurbikenerd.echoLocation.math;

public class Convolutions {
	/*
	 * I'm giving this to you, as the implementation is rather messy.
	 * This is the version with full "tails" implemented: a value is assigned
	 * whenever samples and the kernel have any overlap at all.
	 * As of June 19, it is only partially tested.
	 */
	public static short[] convolve(short[] samples, short[] kernel) {
		int len = kernel.length / 2;
		int start = len / 2;
		short[] result = new short[samples.length + len];
		for (int n = 0; n < result.length; n++) {
			double sum = 0;
			for (int i = start; i<len; i++) {
				if (i<samples.length + (len-1) - n && i >= len-n-1) {
					sum += samples[i+n-len+1]*kernel[len-i-1];
				}
			}
			result[n] = (short) (3.2768e-6 * sum);
		}
		return result;
	}

	public static short[] zipper(short[] left, short[] right){
		if(! (left.length == right.length))
			throw new AssertionError("left and right are not the same length, left is " + left.length + ", right is " + right.length);
		short[] ret = new short[left.length + right.length];
		int leftIdx = 0;
		int rightIdx = 0;
		for(int i = 0; i < ret.length; i++){
			if(i % 2 == 0){
				ret[i] = left[leftIdx];
				leftIdx++;
			}else{
				ret[i] = right[rightIdx];
				rightIdx++;
			}
		}
		return ret;
	}
}
