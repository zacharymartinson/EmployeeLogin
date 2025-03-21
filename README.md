## Employee Login Demo using Face Detection and Face Recognition.
Using Face Detection and Recognition in Android (CameraX and Tensorflow Lite)
Uses ML Kit Face Detection -> MobileFaceNet_TF (Sirius Weights) Face Recognition.

## How it works
Uses ML Kit to detect a face and then we use a recognition based on user ID's embeddings (which you can add to with a list, its iterative). Checks using cosine singularity and anything above 0.8 is good. Then it checks for conccurent frames.


## Demonstration (I got a haircut since!)

![Demo GIF](https://github.com/zacharymartinson/EmployeeLogin/blob/master/showcase.gif)
