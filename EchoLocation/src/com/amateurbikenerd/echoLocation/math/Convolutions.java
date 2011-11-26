package com.amateurbikenerd.echoLocation.math;

public class Convolutions {
    public static short[] convolve(short[] samples, short[] kernel) {
        int kernelSize = 512;
        short[] result = new short[samples.length/2];
        for (int n = kernelSize; n < kernelSize+samples.length/2; n++) {
            long sum = 0;
            for (int i=0; i<kernelSize; ++i)
                sum += samples[n-kernelSize+i]*kernel[i];
            result[n-kernelSize] = (short) (sum / 305176);
        }
        return result;
    }

    public static short[] stereoConvolve(short[] samples, short[][] kernels) {
        int kernelSize = 512;
        short[] result = new short[samples.length/2];
        for (int n = kernelSize; n < kernelSize+samples.length/2; n++) {
            long sum = 0;
            for (int i=0; i<kernelSize; i+=2)
                sum += samples[n-kernelSize+i]*kernels[i%2][i];
            result[n-kernelSize] = (short) (sum / 305176);
        }
        return result;
    }
}
