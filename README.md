# CCDetector is a tool based on NiCad, detecting python code, implementing clone type separation as well as co-change detection.

## Prerequisites
You need to use [NiCad-6.2](https://www.txl.ca/txl-nicaddownload.html) to detect the clones of your projects and get the normalized result files.You can't modify any normalized files.

## How to run CCDetector?

### 1. Clone Separation
This process is implemented by Class *CloneSeparator*.
- Input : *InputCS*.If you don't have this folder, you need to create it by yourself and then, put the NiCad result files in this folder.As shown in the figure below.Each project detects three clone types.

![Image](https://user-images.githubusercontent.com/105061953/168297290-74acbdfd-d80c-4656-9a94-029fc576155d.png)
- Output : *InputCS/Results/“......clone-abstract”*.

### 2. Co-change Detection
This process is implemented by Class *Main*.

- Input  : *Input*.If you don't have this folder, you need to create it by yourself.Just several versions of only a project can be checked at a time.Your operation should look like the following figure.

![Image](https://user-images.githubusercontent.com/105061953/168298321-0143f7b3-8975-4fbd-a841-e1f75be4cc74.png)

- Output : *InputCS/Results/“......clone-abstract”*.

### 3.  Co-changed Clone Separation
This process is implemented by Class *CCCloneSeparator*.
- Input1 : *InputCCTA*.If you don't have this folder, you need to create it by yourself and then, put the *Co-change Detection* result files in this folder.As shown in the figure below.

![Image](https://user-images.githubusercontent.com/105061953/168298657-c6745cbf-4349-4d47-96b4-f70c6d52fdd8.png)

- Input2 : *InputCS*.If you don't have this folder, you need to create it by yourself and then, put the *Clone Separation* result files in this folder.As shown in the figure below.

![Image](https://user-images.githubusercontent.com/105061953/168298875-e27b5f34-a7e0-49ff-89b8-42d49add2d37.png)

- Output.The result is written into the FinalResults in the *Co-change Detection* result files.

