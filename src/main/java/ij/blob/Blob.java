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
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.filter.EDM;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.EllipseFitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.PolygonFiller;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

//import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Represents a connected component - a so called "blob".
 * @author Thorsten Wagner
 */
public class Blob {
	
	public final static int DRAW_HOLES = 1;
	public final static int DRAW_CONVEX_HULL = 2;
	public final static int DRAW_LABEL = 4;
	private static Color defaultColor = Color.black;
	

	private Polygon outerContour;
	private ArrayList<Polygon> innerContours; //Holes
	private int label;
	
	//Features
	private Point2D  centerOfGrafity = null;
	private double perimeter = -1;
	private double perimeterConvexHull = -1;
	private double enclosedArea = -1;

	private double circularity = -1;
	private double thinnesRatio = -1;
	private double areaToPerimeterRatio = -1;
	private double temperature = -1;
	private double fractalBoxDimension = -1;
	private double fractalDimensionGoodness = -1;
	private double elongation = -1;
	private double eigenMajor = -1;
	private double eigenMinor = -1;
	private double orientation = -1;
	private double convexity = -1;
	private double solidity = -1;
	private double areaConvexHull = -1;
	private Calibration cal = new Calibration();
	private double[][] centralMomentsLUT = {{-1,-1,-1},{-1,-1,-1},{-1,-1,-1}};
	private double[][] momentsLUT = {{-1,-1,-1},{-1,-1,-1},{-1,-1,-1}};
	EllipseFitter fittedEllipse = null;
    static ArrayList<CustomBlobFeature> customFeatures = new ArrayList<CustomBlobFeature>();

	public Blob(Polygon outerContour, int label) {
		this.outerContour = outerContour;
		this.label = label;
		innerContours = new ArrayList<Polygon>();
	}
	
	/**
	 * @param outerContour Contur of the blob
	 * @param label Its unique label
	 * @param cal The blob will use the image calibration
	 */
	public Blob(Polygon outerContour, int label, Calibration cal) {
		this.outerContour = outerContour;
		this.label = label;
		innerContours = new ArrayList<Polygon>();
		this.cal = cal;
	}
	
	public void setCalibration(Calibration  cal){
		this.cal = cal;
	}
	
	public static void addCustomFeature(CustomBlobFeature feature) {
		customFeatures.add(feature);
	}
	/**
	 * Changes the default blob color.
	 * @param defaultColor The default color.
	 */
	public static void setDefaultColor(Color defaultColor) {
		Blob.defaultColor = defaultColor;
	}
	
	/**
	 * Evaluates the Custom Feature and return its value
	 * @param Method name The method name of the method in the feature class
	 * @param params the parameters of the method specified by the method name
	 * @throws NoSuchMethodException 
	 */
	public Object evaluateCustomFeature(String methodName, Object... params) throws NoSuchMethodException {
		Boolean methodfound = false;
		int featureIndex = -1;
		for(int i = 0; i < customFeatures.size(); i++){
			Method customMethods[] = customFeatures.get(i).getClass().getDeclaredMethods();
			for(int j = 0; j < customMethods.length; j++){
				if(customMethods[j].getName() == methodName){
					
					methodfound = true;
					featureIndex = i;
					break;
				}
			}
			if(methodfound){break;}
		}
		@SuppressWarnings("rawtypes")
		Class classparams[] = {};
		if(params.length >0){
			classparams = new Class[params.length];
			for(int i = 0; i< params.length; i++){
				classparams[i] = params[i].getClass();
			}
		}
		Object value=0;
		try {
			customFeatures.get(featureIndex).setup(this);
			Method m = customFeatures.get(featureIndex).getClass().getMethod(methodName, classparams);
			
			value = m.invoke((customFeatures.get(featureIndex)), params);
			
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodException("The method " + methodName + " was not found");
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return value;
	}
	
	void draw(ImageProcessor ip, int options, Color col){
		ip.setColor(col);
		fillPolygon(ip, outerContour, false);
		
		
		if((options&DRAW_HOLES)>0){
			for(int i = 0; i < innerContours.size(); i++) {
				if(defaultColor==Color.white){
					ip.setColor(Color.BLACK);
				}
				else
				{
					ip.setColor(Color.white);
				}
				fillPolygon(ip, innerContours.get(i), true);
				if(defaultColor==Color.white){
					ip.setColor(Color.white);
				}
				else
				{
					ip.setColor(Color.black);
				}
				ip.drawPolygon(innerContours.get(i));
				
			}
		}
		
		if((options&DRAW_CONVEX_HULL)>0){
			ip.setColor(Color.RED);
			ip.drawPolygon(getConvexHull());
		}
		
		if((options&DRAW_LABEL)>0){
			Point2D cog = getCenterOfGravity();
			ip.setColor(Color.MAGENTA);
			ip.drawString(""+getLabel(), (int)cog.getX(), (int)cog.getX());
		}
	}
	
	/**
	 * Draws the Blob with or without its holes.
	 * @param ip The ImageProcesser in which the blob has to be drawn.
	 * @param options Drawing Options are DRAW_HOLES, DRAW_CONVEX_HULL, DRAW_LABEL. Combinations with | are possible.
	 */
	public void draw(ImageProcessor ip, int options){
		draw(ip, options, defaultColor);
	}
	
	void draw(ImageProcessor ip, int options, int deltax, int deltay){
		ip.setColor(Color.BLACK);
		Polygon p = new Polygon(outerContour.xpoints,outerContour.ypoints,outerContour.npoints);
		p.translate(deltax, deltay);
		fillPolygon(ip, p, false);
		
		
		if((options&DRAW_HOLES)>0){
			for(int i = 0; i < innerContours.size(); i++) {
				ip.setColor(Color.WHITE);
				p = new Polygon(innerContours.get(i).xpoints,innerContours.get(i).ypoints,innerContours.get(i).npoints);
				p.translate(deltax, deltay);
				fillPolygon(ip, p, true);
			}
		}
		if((options&DRAW_CONVEX_HULL)>0){
			ip.setColor(Color.RED);
			ip.drawPolygon(getConvexHull());
		}
		
		if((options&DRAW_LABEL)>0){
			Point2D cog = getCenterOfGravity();
			ip.setColor(Color.MAGENTA);
			ip.drawString(""+getLabel(), (int)cog.getX(), (int)cog.getY());
		}
	}
	

	
	/**
	 * Draws the Blob with its holes.
	 * @param ip The ImageProcesser in which the blob has to be drawn.
	 */
	public void draw(ImageProcessor ip){
		draw(ip,DRAW_HOLES);
	}
	
	void drawLabels(ImageProcessor ip, Color col) {
		draw(ip,DRAW_HOLES,col);
	}
	
	@SuppressWarnings("unused")
	private final double getArea(Polygon p) {
		if (p==null) return Double.NaN;
		
		int carea = 0;
		int iminus1;
		for (int i=0; i<p.npoints; i++) {
			iminus1 = i-1;
			if (iminus1<0) iminus1=p.npoints-1;
			carea += (p.xpoints[i]+p.xpoints[iminus1])*(p.ypoints[i]-p.ypoints[iminus1]);
		}

		return (Math.abs(carea/2.0));
		
	}
		
	/**
	 * Return the geometric center of gravity of the blob. It
	 * is calculated by the outer contour without consider possible
	 * holes.
	 * @return Geometric center of gravity of the blob
	 */
	public Point2D getCenterOfGravity() {
		
		if(centerOfGrafity != null){
			return centerOfGrafity;
		}
		centerOfGrafity = new Point2D.Float();
	    
	    int[] x = outerContour.xpoints;
	    int[] y = outerContour.ypoints;
	    int sumx = 0;
	    int sumy = 0;
	    double A = 0;

	    for(int i = 0; i < outerContour.npoints-1; i++){
	    	int cross = (x[i]*y[i+1]-x[i+1]*y[i]);
	    	sumx = sumx + (x[i]+x[i+1])*cross;
	    	sumy = sumy + (y[i]+y[i+1])*cross;
	    	A = A + x[i]*y[i+1]-x[i+1]*y[i];
	    }
	    A = 0.5*A;
	    
	    centerOfGrafity.setLocation(cal.getX(sumx/(6*A)),cal.getY(sumy/(6*A)));
		if(getEnclosedArea()==1){
			centerOfGrafity.setLocation(cal.getX(x[0]),cal.getY(y[0]));
		}

		return centerOfGrafity;
	}
	
	/**
	 * Method name of getFeretDiameter (for filtering).
	 */
	public final static String GETFERETDIAMETER = "getFeretDiameter";
	/**
	 * Calculates the feret diameter of the outer contour
	 * @return The feret diameter of the outer contour.
	 */
	public double getFeretDiameter() {
		PolygonRoi proi = new PolygonRoi(outerContour, PolygonRoi.POLYLINE);
		ImagePlus imp = new ImagePlus();
		imp.setCalibration(cal);
		proi.setImage(imp);
		
		return proi.getFeretsDiameter();
	}
	
	/**
	 * Method name of getMinFeretDiameter (for filtering).
	 */
	public final static String GETMINFERETDIAMETER = "getMinFeretDiameter";
	/**
	 * Calculates the min feret diameter of the outer contour
	 * @return The feret diameter of the outer contour.
	 */
	public double getMinFeretDiameter() {
		PolygonRoi proi = new PolygonRoi(outerContour, PolygonRoi.POLYLINE);
		ImagePlus imp = new ImagePlus();
		imp.setCalibration(cal);
		proi.setImage(imp);
		return proi.getFeretValues()[2];
	}
	
	/**
	 * Region-Based Moments Definition of "Gorman et. al. Practical Algorithms for Image Analysis" (p. 157).
	 * (computational expensive!)
	 * @return Region-Based Moments of order (p + q)
	 * @param q (order = (p + q))
	 * @param p (order = (p + q))
	 */
	public double getMoment(int p, int q) {
		 if(p<=2 && q<=2){
			 if(momentsLUT[p][q]!=-1){
				 return momentsLUT[p][q];
			 }
		 }
		 double moment = 0;
	
		Rectangle bounds = outerContour.getBounds();
		for(int x = bounds.x; x < bounds.x+bounds.width+1;x++){
			for(int y = bounds.y; y < bounds.y+bounds.height+1;y++){

				if(outerContour.contains((double)x,(double)y)){
					moment += Math.pow(cal.getX(x), p) * Math.pow(cal.getY(y), q);
				}
			} 
		}
		 momentsLUT[p][q] = moment;

		 return moment;
	}
	
	/**
	 * Central Moments Definition of "Gorman et. al. Practical Algorithms for Image Analysis" (p. 158).
	 * (computational expensive!)
	 * @return Central Moment of Order (p + q)
	 * @param q (order = (p + q))
	 * @param p (order = (p + q))
	 */
	public double getCentralMoments(int p, int q){
		
		 if(p<=2 && q<=2){
			 if(centralMomentsLUT[p][q]!=-1){
				 return centralMomentsLUT[p][q];
			 }
		 }
		
		double centralMoment = 0;
		double m00 = getMoment(0,0);
		double xc = getMoment(1,0)/m00; //Centroid x
		double yc = getMoment(0,1)/m00; //Centroid y
		
		if(p==0 && q == 0){
			centralMoment = m00;
		}
		
		else if(( p==0 && q==1) || ( p==1 && q==0)){
			centralMoment = 0;
		}
		
		else if(p==1 && q == 1) {
			centralMoment = getMoment(1, 1)-yc*getMoment(1, 0);
		}
		
		else if(p==2 && q==0){
			centralMoment = (getMoment(p, q) - xc*(getMoment(1, 0)));
		}
		
		else if(p==0 && q==2){
			centralMoment = (getMoment(p, q) - yc*(getMoment(0, 1)));
		}
		else
		{
			Rectangle bounds = outerContour.getBounds();
			for(int x = bounds.x; x < bounds.x+bounds.width;x++){
				for(int y = bounds.y; y < bounds.y+bounds.height;y++){
					if(outerContour.contains(x, y)){
						centralMoment += Math.pow(cal.getX(x)-xc, p) * Math.pow(cal.getY(y)-yc, q);
					}
				} 
			}
		
			
		}

		centralMomentsLUT[p][q] = centralMoment;
		return centralMoment;
	}
	
	public final static String GETORIENTATIONMAJORAXIS = "getOrientationMajorAxis";
	/**
	 * @return The Orientation of the Major Axis from the Blob in grad (measured counter clockwise from the positive x axis).
	 */
	public double getOrientationMajorAxis(){
		if(orientation!=-1){
			return orientation;
		}
		fitEllipse();
		orientation = fittedEllipse.angle; 
		if(Math.abs(orientation-180)<0.01){
			orientation = 0;
		}
		return orientation;
	}
	
	public final static String GETORIENTATIONMINORAXIS = "getOrientationMinorAxis";
	/**
	 * @return The Orientation of the Major Axis from the Blob in grad (measured counter clockwise from the positive x axis).
	 */
	public double getOrientationMinorAxis(){
		if(orientation!=-1){
			return orientation-90;
		}
		orientation = getOrientationMajorAxis();
		return orientation-90;
	}
	
	private double getEigenvalue(boolean major) {
		double c00 = getCentralMoments(0, 0);
		double c20 = getCentralMoments(2,0)/c00;
		double c02 = getCentralMoments(0,2)/c00;
		double c11 = getCentralMoments(1,1)/c00;

		double valuea = 0.5*(c20+c02)+0.5*Math.sqrt(4*Math.pow(c11, 2)+Math.pow(c20-c02, 2));
		double valueb = 0.5*(c20+c02)-0.5*Math.sqrt(4*Math.pow(c11, 2)+Math.pow(c20-c02, 2));
		double value = valuea;
		if(major){
			if(valuea<valueb){
				value = valueb;
			}
		}
		else
		{
			if(valuea>valueb){
				value = valueb;
			}
		}
		return value;
	}
	
	/**
	 * Calculates Eigenvalue from the major axis using the moments of the boundary
	 * @return Return the Eigenvalue from the major axis (computational expensive!)
	 */
	public double getEigenvalueMajorAxis() {
		if(eigenMajor!=-1){
			return eigenMajor;
		}
		eigenMajor = getEigenvalue(true);
		return eigenMajor;
	}
	
	/**
	 * Calculates Eigenvalue from the minor axis using the moments of the boundary
	 * @return Return the Eigenvalue from the minor axis (computational expensive!)
	 */
	public double getEigenvalueMinorAxis() {
		if(eigenMinor!=-1){
			return eigenMinor;
		}
		eigenMinor = getEigenvalue(false);
		return eigenMinor;
	}
	
	/**
	 * Method name of getElongation (for filtering).
	 */
	public final static String GETELONGATION = "getElongation";
	
	/**
	 * The Elongation of the Blob based on a fitted ellipse (1 - minor axis / major axis)
	 * @return The Elongation (normed between 0 and 1)
	 */
	public double getElongation() {
		if(elongation!= -1){
			return elongation;
		}
		fitEllipse();
		elongation = 1- fittedEllipse.minor/fittedEllipse.major;
		elongation = Math.sqrt(elongation);

		return elongation;
	}
	
	public Point[] getMinimumBoundingRectangle(){
		int[] xp = new int[getOuterContour().npoints];
		int[] yp = new int[getOuterContour().npoints];
		for(int i = 0; i < getOuterContour().npoints; i++){
			xp[i] = getOuterContour().xpoints[i];
			yp[i] = getOuterContour().ypoints[i];
		}
		Point2D.Double[] mbr;
		try{
			mbr = RotatingCalipers.getMinimumBoundingRectangle(xp, yp);
		}
		catch(IllegalArgumentException e){
			return null;
		}
		Point[] p = new Point[4];
		for(int i = 0; i < mbr.length; i++){
			//IJ.log("i " + i);
			p[i] = new Point();
			p[i].x = (int)mbr[i].x;
			p[i].y = (int)mbr[i].y;
		}
		return p;
		
	}
	
	/**
	 * Method name of getLongSideMBR (for filtering).
	 */
	public final static String GETLONGSIDEMBR = "getLongSideMBR";
	
	/**
	 * @return The long side length of the minimum enclosing rectangle
	 */
	public double getLongSideMBR(){
		Point[] mbr = getMinimumBoundingRectangle();
		
		if(mbr == null){
			return Double.NaN;
		}
		
		double firstSide = Math.sqrt(Math.pow(cal.getX(mbr[1].x) -cal.getX(mbr[0].x),2)+Math.pow(cal.getY(mbr[1].y) - cal.getY(mbr[0].y),2));
		double secondSide = Math.sqrt(Math.pow(cal.getX(mbr[1].x) -cal.getX(mbr[2].x),2)+Math.pow(cal.getY(mbr[1].y) -cal.getY(mbr[2].y),2));
		
		return firstSide>secondSide?firstSide:secondSide;
	}
	
	/**
	 * Method name of getLongSideMBR (for filtering).
	 */
	public final static String GETSHORTSIDEMBR = "getShortSideMBR";
	
	/**
	 * @return The short side length of the minimum enclosing rectangle
	 */
	public double getShortSideMBR(){
		Point[] mbr = getMinimumBoundingRectangle();
		if(mbr == null){
			return Double.NaN;
		}
		double firstSide = Math.sqrt(Math.pow(cal.getX(mbr[1].x) -cal.getX(mbr[0].x),2)+Math.pow(cal.getY(mbr[1].y) - cal.getY(mbr[0].y),2));
		double secondSide = Math.sqrt(Math.pow(cal.getX(mbr[1].x) -cal.getX(mbr[2].x),2)+Math.pow(cal.getY(mbr[1].y) -cal.getY(mbr[2].y),2));
		
		return firstSide<secondSide?firstSide:secondSide;
	}
	
	/**
	 * Method name of getLongSideMBR (for filtering).
	 */
	public final static String GETASPECTRATIO = "getAspectRatio";
	
	/**
	 * @return The aspect ratio of the minimum enclosing rectangle
	 */
	public double getAspectRatio(){
		
		
		return getLongSideMBR()/getShortSideMBR();
	}
	
	private void fitEllipse(){
		if(fittedEllipse==null){
			fittedEllipse = new EllipseFitter();
			Rectangle r = outerContour.getBounds();

			ImagePlus help = NewImage.createByteImage("", r.width+1, r.height+1, 1, NewImage.FILL_WHITE);
			ByteProcessor ip =  (ByteProcessor) help.getProcessor();
			ip.setColor(Color.black);
			Polygon p = new Polygon(outerContour.xpoints, outerContour.ypoints, outerContour.npoints);
			p.translate(-r.x, -r.y);
			ip.resetRoi();
			ip.setRoi(p);	
			

			fittedEllipse.fit(ip, null);
		}
		
	}

	private void fillPolygon(ImageProcessor ip, Polygon p, boolean internContour) {
		PolygonRoi proi = new PolygonRoi(p, PolygonRoi.POLYGON);
		Rectangle r = proi.getBounds();
		PolygonFiller pf = new PolygonFiller();
		pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
		ip.setRoi(r);
		ImageProcessor objectMask = pf.getMask(r.width, r.height);
		ip.fill(objectMask);
		if(!internContour){
			ip.drawPolygon(p);
		}
	}
	
	/**
	 * The outer contour of an object (polygon points are pixel indicies)
	 * @return The outer contour of an object (polygon points are pixel indicies)
	 */
	public Polygon getOuterContour() {
		return outerContour;
	}
	/**
	 * Calculates the freeman chain code the outer contour
	 * @return The outer contour as freeman chain code
	 */
	public int[] getOuterContourAsChainCode(){
		return contourToChainCode(getOuterContour());
	}
	
	
	/**
	 * Return all inner contours (holes) of the blob.
	 * @return Arraylist of the inner contours.
	 */
	public ArrayList<Polygon> getInnerContours() {
		return innerContours;
	}
	
	/**
	 * Adds an inner contour (hole) to blob.
	 * @param contour Contour of the hole.
	 */
	void addInnerContour(Polygon contour) {
		innerContours.add(contour);
	}

	/**
	 * Return the label of the blob in the labeled image
	 * @return Return blob's label in the labeled image
	 */
	public int getLabel() {
		return label;
	}
	
	/**
	 * Method name of getPerimeter (for filtering).
	 */
	public final static String GETPERIMETER = "getPerimeter";
	/**
	 * Calculates the perimeter of the outer contour using its chain code
	 * @return The perimeter of the outer contour.
	 */
	public double getPerimeter() {
		
		if(perimeter!=-1){
			return perimeter;
		}
		
		return getPerimeterOfContour(getOuterContour());
	}
	
	private double getPerimeterOfContour(Polygon contour){
		double peri = 0;
		
		if(contour.npoints == 1)
		{
			peri=1;
			return peri;
		}
		int[] cc = contourToChainCode(contour);
		int sum_gerade= 0;
		for(int i = 0; i < cc.length;i++){
			if(cc[i]%2 == 0){
				sum_gerade++;
			}
		}
		
		peri = sum_gerade*0.948 + (cc.length-sum_gerade)*1.340;
		

		
		PolygonRoi roi = new PolygonRoi(outerContour, Roi.POLYLINE);
		ImagePlus dummy = new ImagePlus();
		dummy.setCalibration(cal);
		roi.setImage(dummy);

		
		return peri;
	}
	
	private int[] contourToChainCode(Polygon contour) {
		int[] chaincode = new int[contour.npoints-1];
		for(int i = 1; i <contour.npoints; i++){
			int dx = contour.xpoints[i] - contour.xpoints[i-1];
			int dy = contour.ypoints[i] - contour.ypoints[i-1];
			
			if(dx==1 && dy==0){
				chaincode[i-1] = 0;
			}
			else if(dx==1 && dy==1){
				chaincode[i-1] = 7;
			}
			else if(dx==0 && dy==1){
				chaincode[i-1] = 6;
			}
			else if(dx==-1 && dy==1){
				chaincode[i-1] = 5;
			}
			else if(dx==-1 && dy==0){
				chaincode[i-1] = 4;
			}
			else if(dx==-1 && dy==-1){
				chaincode[i-1] = 3;
			}
			else if(dx==0 && dy==-1){
				chaincode[i-1] = 2;
			}
			else if(dx==1 && dy==-1){
				chaincode[i-1] = 1;
			}
		}
		
		return chaincode;
	}
	
	/**
	 * Method name of getPerimeterConvexHull (for filtering).
	 */
	public final static String GETPERIMETERCONVEXHULL = "getPerimeterConvexHull";
	
	/**
	 * Calculates the perimeter of the convex hull
	 * @return The perimeter of the convex hull
	 */
	public double getPerimeterConvexHull() {
		if(perimeterConvexHull!=-1){
			return perimeterConvexHull;
		}
		PolygonRoi convexRoi = null;
		
		Polygon hull = getConvexHull();
		perimeterConvexHull = 0;
		try {
		convexRoi = new PolygonRoi(hull, Roi.POLYGON);
		ImagePlus dummy = new ImagePlus();
		dummy.setCalibration(cal);
		convexRoi.setImage(dummy);
		perimeterConvexHull = convexRoi.getLength();
		}catch(Exception e){
			perimeterConvexHull = getPerimeter();
			IJ.log("Blob ID: "+ getLabel() +" Error calculating the perimeter of the convex hull. Returning the regular perimeter");
		}
		
		return perimeterConvexHull;
	}
	
	/**
	 * Method name of getConvexity (for filtering).
	 */
	public final static String GETCONVEXITY = "getConvexity";
	
	/**
	 * @return convex hull perimeter/actual perimeter
	 */
	public double getConvexity(){
		if(convexity!=-1){
			return convexity;
		}
		convexity = getPerimeterConvexHull()/getPerimeter();
		if(convexity>1){
			convexity=1;
		}
		return convexity;
	}
	
	/**
	 * Checks if the blob is on the edge of the image.
	 * @param ip The imageprocesser which contains the blob
	 * @return true if the blob is on a edge.
	 */
	public boolean isOnEdge(ImageProcessor ip){
		
		Polygon p = getOuterContour();
		for(int i = 0; i < p.npoints; i++){
			int x = p.xpoints[i];
			int y = p.ypoints[i];
			if(x == 0 || y == 0 || x == (ip.getWidth()-1) || y == (ip.getHeight()-1)){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Method name of getSolidity (for filtering).
	 */
	public final static String GETSOLIDITY = "getSolidity";
	/**
	 * @return enclosed area / enclosed of the convex hull
	 */
	public double getSolidity() {
		if(solidity!=-1){
			return solidity;
		}
		solidity = getEnclosedArea()/getAreaConvexHull();
		if(solidity>1){
			solidity=1;
		}
		return solidity;
	}
	
	/**
	 * Returns the convex hull of the blob.
	 * @return The convex hull as polygon
	 */

	public Polygon getConvexHull() {
		PolygonRoi roi = new PolygonRoi(outerContour, Roi.POLYGON);
		Polygon hull = roi.getConvexHull();
		if(hull==null){
			return getOuterContour();
		}
		return hull;
	}
	
	private double getAreaOfChainCode(int[] cc){
		int B = 1;
		double A = 0;
		for(int i = 0; i < cc.length; i++){
			switch(cc[i]){
			
			case 0:
				A -= B;
				break;
			case 1:
				B += 1;
				A += -(B + 0.5);
				break;
			case 2:
				B += 1;
				break;
			case 3:
				B += 1;
				A += B+0.5;
				break;
			case 4:
				A += B;
				break;
			case 5:
				B += -1;
				A += B - 0.5;
				break;
			case 6:
				B += -1;
				break;
			case 7:
				B += -1;
				A += -(B-0.5);
				break;
			}
		}
		
		double area = Math.abs(A);

		if(area==0){
			area=1;
		}
		return area;
	}
	
	/**
	 * Method name of getEnclosedArea (for filtering).
	 */
	public final static String GETENCLOSEDAREA = "getEnclosedArea";
	/**
	 * Calculates the enclosed are of the outer contour without subsctracting possible holes.
	 * @return The enclosed area of the outer contour (without substracting the holes).
	 */
	public double getEnclosedArea() {
		if(enclosedArea!=-1){
			return enclosedArea;
		}
		/*
		int[] cc = contourToChainCode(getOuterContour());
		enclosedArea = getAreaOfChainCode(cc)*cal.pixelHeight*cal.pixelWidth;
		*/
		
		//enclosedArea = getArea(getOuterContour())*cal.pixelHeight*cal.pixelWidth;
		
		ImagePlus imp = generateBlobImage(this);
		enclosedArea = imp.getStatistics().histogram[0]*cal.pixelHeight*cal.pixelWidth;
		
		return enclosedArea;
	}
	
	
	/**
	 * Method name of getAreaConvexHull (for filtering).
	 */
	public final static String GETAREACONVEXHULL = "getAreaConvexHull";
	/**
	 * @return Area of the convex hull
	 */
	public double getAreaConvexHull(){
		if(areaConvexHull!=-1){
			return areaConvexHull;
		}
		Polygon polyPoints = getConvexHull();
		/*
		int i, j, n = polyPoints.npoints;
		areaConvexHull = 0;

		for (i = 0; i < n; i++) {
			j = (i + 1) % n;
			areaConvexHull += polyPoints.xpoints[i] * polyPoints.ypoints[j];
			areaConvexHull -= polyPoints.xpoints[j] * polyPoints.ypoints[i];
		}
		areaConvexHull /= 2.0;
		areaConvexHull = Math.abs(areaConvexHull)*cal.pixelHeight*cal.pixelWidth;;
		*/
		
		Blob helpblob = new Blob(polyPoints, -1);
		ImagePlus imp = generateBlobImage(helpblob);
		areaConvexHull = imp.getStatistics().getHistogram()[0];
		//areaConvexHull = getArea(polyPoints)*cal.pixelHeight*cal.pixelWidth;
		return areaConvexHull;
	}
	
	/**
	 * Method name of getCircularity (for filtering).
	 */
	public final static String GETCIRCULARITY = "getCircularity";
	/**
	 * Calculates the circularity of the outer contour: (perimeter*perimeter) / (enclosed area). If the value approaches 0.0, it indicates that the polygon is increasingly elongated.
	 * @return Circularity (perimeter*perimeter) / (enclosed area)
	 */
	public double getCircularity() {
		if(circularity!=-1){
			return circularity;
		}
		double perimeter = getPerimeter();
		double size = getEnclosedArea();
		circularity = (perimeter*perimeter) / size;
		return circularity;
	}
	/**
	 * Method name of getThinnesRatio (for filtering).
	 */
	public final static String GETTHINNESRATIO = "getThinnesRatio";
	/**
	 * The Thinnes Ratio of the blob (normed). A circle has a thinnes ratio of 1. 
	 * @return Thinnes Ratio defined as: (4*PI)/Circularity
	 */
	public double getThinnesRatio() {
		if(thinnesRatio!=-1){
			return thinnesRatio;
		}
		thinnesRatio = (4*Math.PI)/getCircularity();
		thinnesRatio = (thinnesRatio>1)?1:thinnesRatio;
		return thinnesRatio;
	}
	
	/**
	 * Method name of getAreaToPerimeterRatio (for filtering).
	 */
	public final static String GETAREATOPERIMETERRATIO = "getAreaToPerimeterRatio";
	/**
	 * Area/Perimeter
	 * @return Area to perimeter ratio
	 */
	public double getAreaToPerimeterRatio() {
		if(areaToPerimeterRatio != -1){
			return areaToPerimeterRatio;
		}
		areaToPerimeterRatio = getEnclosedArea()/getPerimeter();
		return areaToPerimeterRatio;
	}
	
	/**
	 * Method name of getContourTemperature (for filtering).
	 */
	public final static String GETCONTOURTEMPERATURE = "getContourTemperature";
	/**
	 * Calculates the Contour Temperatur. It has a strong relationship to the fractal dimension.
	 * @return Contour Temperatur
	 * @see Datails in Luciano da Fontoura Costa, Roberto Marcondes Cesar,
	 * Jr.Shape Classification and Analysis: Theory and Practice, Second Edition, 2009, CRC Press 
	 */
	public double getContourTemperature() {
		if(temperature!=-1){
			return temperature;
		}
		double chp = getPerimeterConvexHull();
		double peri = getPerimeter();
		temperature = 1/(Math.log((2*peri)/(Math.abs(peri-chp)))/Math.log(2));
		return temperature;
	}
	
	/**
	 * Box Dimension of the blob boundary.
	 * @return Calculates the fractal box dimension of the blob.
	 * @param boxSizes ordered array of Box-Sizes
	 */
	public double getFractalBoxDimension(int[] boxSizes) {
		if(fractalBoxDimension !=-1){
			return fractalBoxDimension;
		}
		FractalBoxCounterBlob boxcounter = new FractalBoxCounterBlob();
		boxcounter.setBoxSizes(boxSizes);
		double[] FDandGOF = boxcounter.getFractcalDimension(this);
		fractalBoxDimension = FDandGOF[0];
		fractalDimensionGoodness = FDandGOF[1];
		return fractalBoxDimension;
	}
	
	/**
	 * Method name of getMaximumInscribedCircle (for filtering).
	 */
	public final static String GETMAXIMUMINSCRIBEDCIRCLE = "getMaximumInscribedCircle";
	public double getDiamaterMaximumInscribedCircle() {
		ImagePlus help = generateBlobImage(this);
		ImageProcessor ipHelp = help.getProcessor();
		ipHelp.invert();
		EDM dm = new EDM();
		FloatProcessor fp = dm.makeFloatEDM (ipHelp, 0, false);
		
		MaximumFinder mf  = new MaximumFinder();
		ByteProcessor bp = mf.findMaxima(fp, 0.5, ImageProcessor.NO_THRESHOLD, MaximumFinder.SINGLE_POINTS, false, true);
		Polygon pl = mf.getMaxima(bp, 0, true);
		return fp.getf(pl.xpoints[0], pl.ypoints[0])*2*cal.getX(1);
		
	}
	
	public static ImagePlus generateBlobImage(Blob b){
		Rectangle r = b.getOuterContour().getBounds();
		r.setBounds(r.x, r.y, (int)r.getWidth()+1, (int)r.getHeight()+1);
		ImagePlus help = NewImage.createByteImage("", r.width, r.height, 1, NewImage.FILL_WHITE);
		ImageProcessor ip = help.getProcessor();
		b.draw(ip, Blob.DRAW_HOLES, -r.x, -r.y);
		help.setProcessor(ip);
		return help;
	}
	
	/**
	 * Method name of getContourTemperature (for filtering).
	 */
	public final static String GETFRACTALBOXDIMENSION = "getFractalBoxDimension";
	/**
	 * @return The fractal box dimension of the blob.
	 */
	public double getFractalBoxDimension() {
		if(fractalBoxDimension !=-1){
			return fractalBoxDimension;
		}
		FractalBoxCounterBlob boxcounter = new FractalBoxCounterBlob();
		double[] FDandGOF  = boxcounter.getFractcalDimension(this);
		fractalBoxDimension = FDandGOF[0];
		fractalDimensionGoodness = FDandGOF[1];
		return fractalBoxDimension;
	}
	
	/**
	 * The goodness of the "best fit" line of the fractal box dimension estimation.
	 * @return The goodness of the "best fit" line of the fractal box dimension estimation.
	 */
	public double getFractalDimensionGoodness(){
		return fractalDimensionGoodness;
	}
	
	/**
	 * Method name of getNumberofHoles (for filtering).
	 */
	public final static String GETNUMBEROFHOLES = "getNumberofHoles";
	/**
	 * The number of inner contours (Holes) of a blob.
	 * @return The number of inner contours (Holes) of a blob.
	 */
	public int getNumberofHoles() {
		return innerContours.size();
	}
}
