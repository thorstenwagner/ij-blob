package ij.blob.tests;

import ij.IJ;
import ij.blob.CustomBlobFeature;

public class MyBlobFeature extends CustomBlobFeature {
	
	public double LocationFeature(Integer width, Integer height) {
        double feature =  getBlob().getCenterOfGravity().distance((double)width,(double)height);
        IJ.log("F " + feature);
        return feature;
    }

}
