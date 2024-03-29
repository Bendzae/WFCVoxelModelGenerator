# WFC Voxel Model Generator  
This project contains a 3D-Implementation of the WFC Algorithm by Max Gumin 
(https://github.com/mxgmn/WaveFunctionCollapse) and a UI that allows
loading example Voxel Models, manipulating Parameters of the algorithm aswell 
as export the generated Models. This application was developed as part of a Bachelor-Thesis
in CS at HAW-Hamburg, in cooperation with Prof. Dr. Philip Jenke.

Bachelor-Thesis: https://users.informatik.haw-hamburg.de/~abo781/abschlussarbeiten/ba_dzaebel.pdf

![i1](./example.png)
## Setup
### Prerequisites
* **Java JDK 16** 
* **Maven** 
* **Intellij Idea** (recommended but not  required)
### Project Setup
(1) First clone the master-branch of this Repository:  
```
git clone https://github.com/Bendzae/WFCVoxelModelGenerator.git
```
(2) Move into the project directory and install dependencies with Maven
```
cd wfc
mvn install
```
(3) If you see `BUILD SUCCESS` in the output the installation was successful and you
start the application using:
```
mvn javafx:run
```
If you are using an IDE like IntelliJ Idea step 2 and 3 can be skipped. Simply wait until
the IDE has automatically installed all dependencies and run the Main-Class App.java in:
```
src/main/java/org.example/view/App.java
```
## Usage 
![test](./Screenshot.png)
### Selecting and Importing Example Models
In Area (1) you can select a loaded model in the dropdown or import your own using the
Import Model Button. The only supported Format is `.vox`. Recommended Voxel Editors are 
Magicka Voxel(https://ephtracy.github.io/) and Goxel(https://goxel.xyz/). 
### Parameters
In area (2) you can modify parameters of the algortihm:
* **Output Size:** (X, Y, Z) size of the output model. (actual voxel size is this times pattern size)
* **Pattern Size:** N x N x N size of patterns that should be extracted from the input model. 
For good results try to match this to the size of recognizable patterns in your model.
* **Rotation:** Should rotated versions of the input be used? (Usually leads to more interesting results)
* **Avoid Empty Pattern:** The close to 1, the more the algortihm tries to fill the given space
* **Seed:** If you generated a model that you like you can use its seed to recreate it
(with the exact same parameters).   

Note: After each generation the parameters for the currently used input model are stored in a json file
so they will be remembered the next time you load this model.
### Generation
If an input model is loaded you can simply click the **Generate** button in (3) to start generation.
### Export
To export the last generated model in .vox format, click **Export Model** in (3).
### 3D Viewer
Navigation in the 3D-Viewer (4): Left-or right-click and drag to rotate, mousewheel to zoom.

## More Examples
![i2](./tower_result.png)
![i3](./tree_result.png)
![i4](./river_result.png)