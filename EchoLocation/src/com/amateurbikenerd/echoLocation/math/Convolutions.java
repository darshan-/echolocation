package com.amateurbikenerd.echoLocation.math;

public class Convolutions {
    public static short[] convolve(short[] samples, short[] kernel) {
        int kernelSize = 512;
        short[] result = new short[samples.length/2];
        for (int n = kernelSize; n < kernelSize+samples.length/2; n++) {
            double sum = 0;
            for (int i=0; i<kernelSize; ++i)
                sum += samples[n-kernelSize+i]*kernel[i];
            result[n-kernelSize] = (short) (3.2768e-6 * sum);
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
