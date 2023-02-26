/*
    IJBlob is a ImageJ library for extracting connected components in binary Images
    Copyright (C) 2012  Thorsten Wagner wagner@biomedical-imaging.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package ij.blob;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.CanvasResizer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Does Connected Component Labeling 
 * @author Thorsten Wagner
 * 
 */
class ConnectedComponentLabeler {
	
	private ImagePlus imp;
	private ImageProcessor labledImage;
	private int NOLABEL = 0;
	private int labelCount = 1;
	private int BACKGROUND = 255;
	private int OBJECT = 0;
	private ManyBlobs allBlobs;
	private boolean removeBorder = false;
	private int offSetX = 0;
	private int offsetY = 0;
	private boolean isBinary = true;
	/*
	 * 
	 * The read-order of the neighberhood of p.
	 *
	 * 5 * 6 * 7 
	 * 4 * p * 0 
	 * 3 * 2 * 1 
	 */
	int iterationorder[] = { 5, 4, 3, 6, 2, 7, 0, 1 };
	
	/**
	 * @param allBlobs A ManyBlobs Object where the Blobs has to be stored
	 * @param imp The image 
	 */
	public ConnectedComponentLabeler(ManyBlobs allBlobs, ImagePlus imp, int BACKGROUND, int OBJECT) {
		this.allBlobs = allBlobs;
		this.imp = imp;
		this.BACKGROUND = BACKGROUND;
		this.OBJECT = OBJECT;
		ImageStatistics stats = imp.getStatistics();
		
		this.isBinary = (stats.histogram[0] + stats.histogram[255]) == stats.pixelCount;
	}
	
	public void doConnectedComponents() {
		if(!this.isBinary) {
			BACKGROUND = 0;
			OBJECT = 255;

			ImagePlus helpimp = imp.duplicate();
			helpimp.setCalibration(imp.getCalibration());
			
			ImageStatistics stats = helpimp.getStatistics();
			int min_label = (int)stats.min+1;
			int max_label = (int)stats.max;

			for(int threshold = min_label; threshold <= max_label; threshold++) {
				ImageProcessor hlp = helpimp.getProcessor().duplicate();
				hlp.setThreshold(threshold, threshold);
				ByteProcessor mask = hlp.createMask();
				ImagePlus mask_imp = new ImagePlus("", mask);
				mask_imp.setCalibration(imp.getCalibration());
				addWhiteBorder(mask_imp);
				if(labledImage == null) {
					labledImage = new ColorProcessor(this.imp.getWidth(), this.imp.getHeight());
				}
	
				doConnectedComponents2(mask_imp);
				
				System.out.println("count"+this.labelCount+" asd"+allBlobs.size());
			}
		}
		else {
			addWhiteBorder(imp);
			labledImage = new ColorProcessor(this.imp.getWidth(), this.imp.getHeight());
			doConnectedComponents2(imp);
		}
		
	}
	
	/**
	 * Start the Connected Component Algorithm
	 * @see  F. Chang, A linear-time component-labeling algorithm using contour tracing technique, Computer Vision and Image Understanding, vol. 93, no. 2, pp. 206-220, 2004.
	 */
	public void doConnectedComponents2(ImagePlus imp) {
		
		ImageProcessor ip = imp.getProcessor();
		Calibration c = imp.getCalibration();

		ByteProcessor proc = (ByteProcessor) ip;
		byte[] pixels = (byte[]) proc.getPixels();
		int w = proc.getWidth();
		
		Rectangle roi = ip.getRoi();
		int value;
		for (int i = roi.y; i < roi.y + roi.height; ++i) {
			int offset = i * w;
			for (int j = roi.x; j < roi.x + roi.width; ++j) {
				value = pixels[offset + j] & 255;
				//System.out.println("Value "+value+" pixel "+pixels[offset + j]);
				if (value == OBJECT) {
					
					if (isNewExternalContour(j, i, proc) && hasNoLabel(j, i)) {
			
						labledImage.set(j, i, labelCount);
						Polygon outerContour = traceContour(j, i, proc,
								labelCount, 1);
						outerContour.translate(offSetX, offsetY);
					
						allBlobs.add(new Blob(outerContour, labelCount,c));
						++labelCount;

					}
					if (isNewInternalContour(j, i, proc)) {
						int label = labledImage.get(j, i);
						if (hasNoLabel(j, i)) {
							//printImage(labledImage);
							label = labledImage.get(j-1, i);
							labledImage.set(j, i, label);

						}
						try{
						Polygon innerContour = traceContour(j, i, proc, label,
								2);
						innerContour.translate(offSetX, offsetY);
						getBlobByLabel(label).addInnerContour(innerContour);
						}catch(Exception e){
						  
							IJ.log("x " + j + " y " +i + " label " + label);
						}

					} else if (hasNoLabel(j, i)) {
					
						int precedinglabel = labledImage.get(j - 1, i);
						labledImage.set(j, i, precedinglabel);
					}

				}
			}
		}
		if(removeBorder){
			removeBorder(imp);
		}
		//printImage(labledImage);
	}
	
	@SuppressWarnings("unused")
	private void printImage(ImageProcessor img){
		System.out.println("=================");
		ImageProcessor proc = img;
		
		for(int y = 0; y < proc.getHeight(); y++){
			String oneline="";
			int numberSpaces = 0;
			for(int x = 0; x < proc.getWidth(); x++){
				String pixel = "" + proc.getPixel(x, y);
				
				for(int i = 0; i < numberSpaces; i++){
					oneline += " ";
				}
				oneline += "" + pixel;
				numberSpaces = 8-pixel.length()%8;
			}
			System.out.println(oneline);
		   
		}
	}

	public ImagePlus getLabledImage() {
		ImagePlus img = new ImagePlus("Labeled", labledImage);
		ColorProcessor proc = (ColorProcessor) img.getProcessor();
		int[] pixels = (int[]) proc.getPixels();
		int w = proc.getWidth();
		int h = proc.getHeight();
		int value;
		for (int i = 0; i < h; ++i) {
			int offset = i * w;
			for (int j = 0; j < w; ++j) {
				value = pixels[offset + j];
				if(value==-1){
					pixels[offset + j] = BACKGROUND;
				}
			}
		}
		if(removeBorder){
			removeBorder(img);
		}
		return img;
	}
	
	

	private Polygon traceContour(int x, int y, ByteProcessor proc, int label,
			int start) {

		Polygon contour = new Polygon();
		Point startPoint = new Point(x, y);
		contour.addPoint(x, y);

		Point nextPoint = nextPointOnContour(startPoint, proc, start);
		
		if (nextPoint.x == -1) {
			// Point is isolated;
			return contour;
		}
		Point T =  new Point(nextPoint.x,nextPoint.y);
		boolean equalsStartpoint = false;
		do {
			contour.addPoint(nextPoint.x, nextPoint.y);
			labledImage.set(nextPoint.x, nextPoint.y, label);
			equalsStartpoint = nextPoint.equals(startPoint);
			nextPoint = nextPointOnContour(nextPoint, proc, -1);
		} while (!equalsStartpoint || !nextPoint.equals(T));

		return contour;
	}

	Point prevContourPoint;

	// start = 1 -> External Contour
	// start = 2 -> Internal Contour
	private final Point nextPointOnContour(Point startPoint, ByteProcessor proc,
			int start) {

		/*
		 ************
		 *5 * 6 * 7 * 
		 *4 * p * 0 * 
		 *3 * 2 * 1 * 
		 ************
		 */
		Point[] helpindexToPoint = new Point[8];

		int[] neighbors = new int[8]; // neighbors of p
		int x = startPoint.x;
		int y = startPoint.y;

		int I = 2;
		int k = I - 1;
		
		int u = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int window_x = (x - k + i);
				int window_y = (y - k + j);
				if (window_x != x || window_y != y) {
					neighbors[iterationorder[u]] = proc.get(window_x, window_y);
					helpindexToPoint[iterationorder[u]] = new Point(window_x,
							window_y);
					u++;
				}
			}
		}
		ArrayList<Point> indexToPoint = new ArrayList<Point>(
				Arrays.asList(helpindexToPoint));

		final int NOSTARTPOINT = -1;
		final int STARTEXTERNALCONTOUR = 1;
		final int STARTINTERNALCONTOUR = 2;

		switch (start) {
		case NOSTARTPOINT:
			int prevContourPointIndex = indexToPoint.indexOf(prevContourPoint);
			start = (prevContourPointIndex + 2) % 8;
			break;
		case STARTEXTERNALCONTOUR:
			start = 7;
			break;
		case STARTINTERNALCONTOUR:
			start = 3;
	
			break;
		}

		int counter = start;
		int pos = -2;

		Point returnPoint = null;
		while (pos != start) {
			pos = counter % 8;
			if (neighbors[pos] == OBJECT) {
				prevContourPoint = startPoint;
				returnPoint = indexToPoint.get(pos);
				return returnPoint;
			}
			Point p = indexToPoint.get(pos);
			if (neighbors[pos] == BACKGROUND) {
				try {
					labledImage.set(p.x, p.y, -1);
				} catch (Exception e) {
					IJ.log("x " + p.x + " y " + p.y);
				}
			}

			counter++;
			pos = counter % 8;
		}
		
		Point isIsolated = new Point(-1, -1);
		return isIsolated;
	}

	private boolean isNewExternalContour(int x, int y, ByteProcessor proc) {
		return isBackground(x, y - 1, proc);
	}
	
	private boolean hasNoLabel(int x, int y) {
		int label = labledImage.get(x, y);
		return label == NOLABEL;
	}

	private boolean isMarked(int x, int y) {
		return labledImage.get(x, y) == -1;
	}

	private boolean isBackground(int x, int y, ByteProcessor proc) {
		return (proc.get(x, y) == BACKGROUND);
	}

	private boolean isNewInternalContour(int x, int y, ByteProcessor proc) {
		return isBackground(x, y + 1, proc) && !isMarked(x, y + 1);
	}
	
	private Blob getBlobByLabel(int label) {
		for (int i = 0; i < allBlobs.size(); i++) {
			if (allBlobs.get(i).getLabel() == label) {
				return allBlobs.get(i);
			}
		}
		return null;
	}
	
	private void addWhiteBorder(ImagePlus img) {
		offSetX=0;
		offsetY=0;
		boolean hasWhiteBorder = true;
		ImageProcessor oldip = img.getProcessor();
		ByteProcessor oldproc = (ByteProcessor) oldip;
		byte[] pixels = (byte[]) oldproc.getPixels();
		int w = oldproc.getWidth();
		for (int i = 0; i < oldproc.getHeight(); i++) {
			
			int offset = i * w;
			//First and last Scanrow
			if (i == 0 || i == oldproc.getHeight()-1) {

				for (int j = 0; j < oldproc.getWidth(); j++) {
					int value = pixels[offset + j] & 255;
					if (value == OBJECT) {
						hasWhiteBorder = false;
					}
				}
			}
			// First and last Pixel per scan row
			int firstvalue = pixels[offset + 0] & 255;
			int lastvalue = pixels[offset + oldproc.getWidth() - 1] & 255;
			if (firstvalue == OBJECT || lastvalue == OBJECT) {
				hasWhiteBorder = false;
			}

			if (!hasWhiteBorder) {
				i = oldproc.getHeight(); // Stop searching
			}
		}
		//hasWhiteBorder=false;
		if (!hasWhiteBorder) 
		{
			offSetX=-1;
			offsetY=-1;
			removeBorder=true;
			CanvasResizer resizer = new CanvasResizer();
			Color oldbg = Toolbar.getBackgroundColor();
			Prefs.set("resizer.zero", false);

			if(BACKGROUND==255){
				Color bgcolor = (img.isInvertedLut()) ? Color.BLACK : Color.WHITE;
				Toolbar.setBackgroundColor(bgcolor);

				
			}else{
				Color bgcolor = (img.isInvertedLut()) ? Color.WHITE : Color.BLACK;
				Toolbar.setBackgroundColor(bgcolor);
			}

			img.setProcessor(resizer.expandImage(img.getProcessor(), img.getWidth()+2, img.getHeight()+2, 1, 1));
			Toolbar.setBackgroundColor(oldbg);
		} else
		{
			imp = img;
		}
	}
	
	public void removeBorder(ImagePlus img) {
		CanvasResizer resizer = new CanvasResizer();
/*
		if(BACKGROUND==255){
			Color bgcolor = (img.isInvertedLut()) ? Color.BLACK : Color.WHITE;
			Toolbar.setBackgroundColor(bgcolor);
		}else{
			Color bgcolor = (img.isInvertedLut()) ? Color.WHITE : Color.BLACK;
			Toolbar.setBackgroundColor(bgcolor);
		}
	*/
		img.setProcessor(resizer.expandImage(img.getProcessor(), img.getWidth()-2, img.getHeight()-2, -1, -1));
	}
	

}
