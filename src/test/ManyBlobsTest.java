package test;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import ij.IJ;
import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;

import org.junit.Test;
public class ManyBlobsTest {
	@Test
	public void testFilterBlobs () {
		URL url = this.getClass().getResource("3blobs.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		ManyBlobs t = mb.filterBlobs(0.9, 1, Blob.GETTHINNESRATIO);
		assertEquals(1, t.size(),0);
	}
	@Test
	public void testFilterBlobs_NoUpperLimit () {
		URL url = this.getClass().getResource("3blobs.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		ManyBlobs t = mb.filterBlobs(0.9, Blob.GETTHINNESRATIO);
		assertEquals(1, t.size(),0);
	}
	
	@Test
	public void testBlobsOnBorder() {
		URL url = this.getClass().getResource("squaresOnBoarder.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		
		mb.findConnectedComponents();
		assertEquals(4, mb.size(),0);
	}
	
	@Test
	public void testBlobsOnBorderInvertedLUT() {
		URL url = this.getClass().getResource("squaresOnBoarder.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ip.getProcessor().invertLut();
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		assertEquals(4, mb.size(),0);
	}
	
	@Test
	public void testBlobsOnBorderInverted() {
		URL url = this.getClass().getResource("squaresOnBoarderInv.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.setBackground(0);
		mb.findConnectedComponents();
		assertEquals(4, mb.size(),0);
	}
	
	@Test 
	public void testBlackBackground() {
		URL url = this.getClass().getResource("3blobs.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ip.getProcessor().invert();
		ManyBlobs mb = new ManyBlobs(ip);
		mb.setBackground(0);
		mb.findConnectedComponents();
		assertEquals(3, mb.size(),0);
	}
	
	@Test
	public void testGetSpecificBlobNotFound() {
		URL url = this.getClass().getResource("squares_20x20_30x30.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		Blob resultBlob = mb.getSpecificBlob(125, 84);
		assertEquals(null, resultBlob);
	}
	
	@Test
	public void testGetSpecificBlob() {
		URL url = this.getClass().getResource("squares_20x20_30x30.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		Blob resultBlob = mb.getSpecificBlob(21, 41);
		assertEquals(mb.get(0), resultBlob);
	}
	
	@Test
	public void testBlobOnBorder_right() {
		
		URL url = this.getClass().getResource("squareOnBoarder_right.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		mb.findConnectedComponents();
		assertEquals(1, mb.size(),0);
	}
	
	@Test (expected=RuntimeException.class)
	public void testNewObject_findConnectedComponents() {
		ManyBlobs t = new ManyBlobs();
		t.findConnectedComponents();
	}
	
	@Test (expected=RuntimeException.class)
	public void testNewObject_getLabeledImage() {
		ManyBlobs t = new ManyBlobs();
		t.findConnectedComponents();
	}
	
	@Test
	public void testComplexImageNoException() {
		URL url = this.getClass().getResource("complexImage.tif");
		ImagePlus ip = new ImagePlus(url.getPath());
		ManyBlobs mb = new ManyBlobs(ip);
		try{
			mb.findConnectedComponents();
		}
		catch(Exception ex){
			assertEquals("Error on complex image: " + ex.getMessage(),false,true);
		}
	}

}
