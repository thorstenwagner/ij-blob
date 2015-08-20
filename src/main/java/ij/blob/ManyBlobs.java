/*
    IJBlob is a ImageJ library for extracting connected components in binary Images
    Copyright (C) 2012  Thorsten Wagner wagner@biomedical-imaging.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MER
TABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package ij.blob;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.NextImageOpener;
import ij.process.ColorProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/*
 * This library extracts connected components . For this purpose it uses the
 * following algorithm : F. Chang, A linear-time
 * component-labeling algorithm using contour tracing technique, Computer
 * Vision and Image Understanding, vol. 93, no. 2, pp. 206-220, 2004.
 */

/**
 * Represents the result-set of all detected blobs as ArrayList
 * @author Thorsten Wagner
 */

public class ManyBlobs extends ArrayList<Blob> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ImagePlus binaryImage = null;
	private ImagePlus labeledImage = null;
	private int BACKGROUND = 255;
	private int OBJECT = 0;

	public ManyBlobs() {

	}
	
	/**
	 * @param imp Binary Image
	 */
	public ManyBlobs(ImagePlus binaryImage) {
		setImage(binaryImage);
	}
	

	
	
	/**
	 * Mutator to modify the background target. This method will switch
	 * the background to the user's specification and also swap the OBJECT
	 * value to be the opposite. e.g. If the users specifies the background
	 * to be black, the objects (blobs) looked for will be white.
	 
	 * @param backgroundVal : 0 or 1 (black/white respectively)
	 */
	public void setBackground(int val){
		if(val > 1)
			throw new IllegalArgumentException("Value must be 0 or 1 (black/white respectively)");
		
		if(val == 0){
			BACKGROUND = val;
			OBJECT = 255;
		}
		else {
			BACKGROUND = 255;
			OBJECT = 0;
		}
	}
	
	private void setImage(ImagePlus imp) {
		this.binaryImage = imp;
		ImageStatistics stats = imp.getStatistics();
		
		boolean notBinary = (stats.histogram[0] + stats.histogram[255]) != stats.pixelCount;
		boolean toManyChannels = (imp.getNChannels()>1);
		boolean wrongBitDepth = (imp.getBitDepth()!=8);
		if (notBinary | toManyChannels | wrongBitDepth) {
			throw new java.lang.IllegalArgumentException("Wrong Image Format. IJ Blob only supports 8-bit, single-channel binary images");
		}
	}
	
	/**
	 * Start the Connected Component Algorithm
	 * @see  F. Chang, A linear-time component-labeling algorithm using contour tracing technique, Computer Vision and Image Understanding, vol. 93, no. 2, pp. 206-220, 2004.
	 */
	public void findConnectedComponents() {
		if(binaryImage==null){
			throw new RuntimeException("Cannot run findConnectedComponents: No input image specified");
		}
		ConnectedComponentLabeler labeler = new ConnectedComponentLabeler(this,binaryImage,BACKGROUND,OBJECT);
		labeler.doConnectedComponents();
		labeledImage = labeler.getLabledImage();
	}
	/**
	 * 
	 * @return Return the labeled Image.
	 */
	public ImagePlus getLabeledImage() {
		if(labeledImage == null){
			throw new RuntimeException("No input image was analysed for connected components");
		}
		return labeledImage;
	}
	

	public void setLabeledImage(ImagePlus p) {
		labeledImage = p;
	}
	
	/**
	 * Returns a specific {@link Blob} which encompasses a point
	 * @param x x coordinate of the point
	 * @param y y coordinate of the point
	 * @return The blob which contains the point, otherwise null
	 */
	public Blob getSpecificBlob(int x, int y){
		
		   for(int i = 0; i < this.size(); i++){
		       if(this.get(i).getOuterContour().contains(x, y)){
		    	   return this.get(i);
		       }
		   }
		   return null;
	}
	
	/**
	 * Returns a specific blob which encompasses a point
	 * @return The blob which contains the point, otherwise null
	 */
	public Blob getSpecificBlob(Point p){
		   return getSpecificBlob(p.x,p.y);
	}
	
	public Blob getBlobByLabel(int id){
		for (Blob b : this) {
			if(b.getLabel()==id){
				return b;
			}
		}
		return null;
	}

	
	/**
	 * Filter all blobs which feature (specified by the methodName) is higher than 
	 * the lowerLimit or lower than the upper limit.
	 * For instance: filterBlobs(Blob.GETENCLOSEDAREA,40,100) will filter all blobs between 40 and 100 pixel².
	 * @param methodName Getter method of the blob feature (double as return value).
	 * @param lowerLimit Lower limit for the feature to filter blobs.
	 * @param upperLimit Upper limit for the feature to filter blobs.
	 * @return The filtered blobs.
	 */
	public ManyBlobs filterBlobs(double lowerLimit, double upperLimit, String methodName, Object... methodparams){
		ManyBlobs blobs = new ManyBlobs();
		blobs.setImage(binaryImage);
		@SuppressWarnings("rawtypes")
		Class classparams[] = {};
		if(methodparams.length >0){
			classparams = new Class[methodparams.length];
			for(int i = 0; i< methodparams.length; i++){
				classparams[i] = methodparams[i].getClass();
			}
		}
		
		try {
			boolean methodInBuild = true;
			boolean methodIsCustom = false;
			Method m = null;
			try {
				m = Blob.class.getMethod(methodName, classparams);
			}
			catch (NoSuchMethodException e) {
				methodInBuild = false;
				
			}
			int featureIndex = 0;
			int methodIndex = 0;
			if(!methodInBuild){
				for(int i = 0; i < Blob.customFeatures.size(); i++){
					Method customMethods[] = Blob.customFeatures.get(i).getClass().getDeclaredMethods();
					for(int j = 0; j < customMethods.length; j++){
						if(customMethods[j].getName() == methodName){
							
							methodIsCustom = true;
							methodIndex = j;
							featureIndex = i;
							m = customMethods[j];
							break;
						}
					}
					if(methodIsCustom){break;}
				}
			}

			for(int i = 0; i < this.size(); i++) {
				if(this.get(i).getOuterContour().npoints< 4){
					continue;
				}
				double value = 0;
				Object methodvalue = null;
				if(methodInBuild){
					methodvalue = m.invoke(this.get(i), methodparams);
				}
				else if(methodIsCustom){
					methodvalue =  m.invoke((Blob.customFeatures.get(featureIndex)), methodparams);
				}
				else{
		
					throw new NoSuchMethodException("The method " + methodName + " was not found");
				}

				if (methodvalue instanceof Integer){
					int help = (Integer) methodvalue;
					value = (double)help;
				}
				else if (methodvalue instanceof Double){
					value= (Double) methodvalue;
				}
				else {
					IJ.log("Return type not supported");
				}
	
				boolean included= false;
				
				if (Double.isNaN(value)){
					included = true;
				}
				else if (Double.isInfinite(upperLimit)) {
					included =  (value >= lowerLimit) ? true : false;
					if(!included){
						included = (Math.abs(lowerLimit-value)<0.0001) ? true:false;
					}
				}
				else
				{
					included = (value >= lowerLimit && value <= upperLimit) ? true : false;
					if(!included){
					included = (!included && Math.abs(lowerLimit-value)<0.0001) ? true:false;
					}
					if(!included){
					included = (!included && Math.abs(upperLimit-value)<0.0001) ? true:false;
					}
				}
				if(included){
					blobs.add(this.get(i));
				//	IJ.log("ADD");
					
				}else{
					//IJ.log("NOT INC " + methodName + " v " + value);
				}
			}
		} catch (NoSuchMethodException e) {
			IJ.log("Method not found: " + e.getMessage());
			e.printStackTrace();
		} catch (SecurityException e) {
			IJ.log(e.getMessage());
		
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			IJ.log(e.getMessage());
			throw new IllegalArgumentException("Method " + methodName + " was called with wrong types of parameters");
		} catch (InvocationTargetException e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
		}
		blobs.setLabeledImage(generateLabeledImageFromBlobs(blobs));
	//	IJ.log("Blobs Nachher " + blobs.size());
		return blobs;
		
	}
	

	
	private ImagePlus generateLabeledImageFromBlobs(ManyBlobs blobs){
		
		ImagePlus labImg = NewImage.createRGBImage("Labeled Image", labeledImage.getWidth() , labeledImage.getHeight(), 1, NewImage.FILL_WHITE);
		ColorProcessor labledImageProc = (ColorProcessor)labImg.getProcessor();
		for(int i = 0; i < blobs.size(); i++){
			int helpcol = (int)(((double)i)/blobs.size() * (255*255*255));
			blobs.get(i).drawLabels(labledImageProc,new Color(helpcol));
		}
		
		return labImg;
	}
	
	/**
	 * Filter all blobs which feature (specified by the methodName) is higher than 
	 * the lowerLimit and lower than the upper limit.
	 * For instance: filterBlobs(Blob.GETENCLOSEDAREA,40,100) will filter all blobs between 40 and 100 pixel².
	 * @param methodName Getter method of the blob feature (double as return value).
	 * @param limits First Element is the lower limit, second element is the upper limit
	 * @return The filtered blobs.
	 */
	public ManyBlobs filterBlobs(double[] limits, String methodName, Object... methodparams){
		return filterBlobs(limits[0], limits[1], methodName,methodparams);
	}
	/**
	 * Filter all blobs which feature (specified by the methodName) is higher than 
	 * the lower limit.
	 * For instance: filterBlobs(Blob.GETENCLOSEDAREA,40) will filter all blobs with an area higher than 40 pixel²
	 * @param methodName Getter method of the blob feature (double as return value).
	 * @param lowerlimit Lower limit for the feature to filter blobs.
	 * @return The filtered blobs.
	 * */
	public ManyBlobs filterBlobs(double lowerlimit, String methodName, Object... methodparams){
		return filterBlobs(lowerlimit, Double.POSITIVE_INFINITY, methodName, methodparams);
	}


}
