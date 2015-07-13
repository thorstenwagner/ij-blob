# IJBlob
The IJBlob library indentifying connected components in binary images. The algorithm used for connected component labeling is:

Chang, F. (2004). A linear-time component-labeling algorithm using contour tracing technique. Computer Vision and Image Understanding, 93(2), 206–220. doi:10.1016/j.cviu.2003.09.002

A connected component is a set of pixels which are connected by its 8-neigherhood and is often called a "blob". An Example:

![](https://dl.dropbox.com/u/560426/components2.jpg)

The image above contains 8 marked blobs. Also the holes (and the contours of the holes) of the two Bs and the O are identified. It is also possible to get a color labeled image:

![](https://dl.dropbox.com/u/560426/imagej/ijbloblabeled.jpg)

In addition nested objects are identified:

![](https://dl.dropbox.com/u/560426/components3.jpg)

The ImageJ Shape Filter Plugin (see downloads) uses this library for flitering the blobs by its shape.

If you are using IJBlob in a scientific publication, please cite:

Wagner, T and Lipinski, H 2013. IJBlob: An ImageJ Library for Connected Component Analysis and Shape Analysis. Journal of Open Research Software 1(1):e6, DOI: http://dx.doi.org/10.5334/jors.ae

##Features of IJBlob
IJBlob 1.1 introduces a filter and extension framework! Please see the HowToUSE for more information

* Extract the outer contour of each blob.
* Extracts also all inner contours of each blob (holes).
* Detects also nested objects (blob in blob).
* Calculates BasicFeatures of the blob.
  * Center of Gravity
  * Enclosed Area
  * Area Convex Hull
  * Perimeter
  * Perimeter of the convex hull
  * Cicularity
  * Thinnes Ratio
  * Feret Diameter
  * Min. Feret Diameter
  * Long Side Min. Bounding Rect
  * Short Side Min. Bounding Rect
  * Aspect Ratio
  * Area/Perimeter Ratio
  * Temperatur of the outer contour
  * Fractal Box Dimension
  * Region Based Moments
  * Central Region Based Moments
  * Eigenvalue Major Axis
  * Eigenvalue Minor Axis
  * Elongation
  * Convexity
  * Solidity
  * Orientation
  * Outer Contour as Freeman-Chain-Code
* Rendering of Blobs and its Convex Hull

## Features of Shape Filter Plugin
Remove objects by some basic features (see the bold features above).

![](https://dl.dropbox.com/u/560426/imagej/gui_.png)

### Restrictions:
The object/background has to be black (0) or white (255)

### Contact
If you miss some features or want to contribute to the project, do not hesitate to contact me at: wagner@biomedical-imaging.de
