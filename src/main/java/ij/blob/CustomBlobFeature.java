package ij.blob;

/**
 * Abstract class for define own features.
 * @author Thorsten Wagner
 */
public abstract class CustomBlobFeature {

	private Blob blob;
	
	
	void setup(Blob blob){
		this.blob = blob;
	}
	
	/**
	 * Getter method for the blob.
	 * @return The reference to the blob
	 */
	public Blob getBlob(){
		return blob;
	}	
}
