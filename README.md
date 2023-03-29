# CCDetector is a tool based on NiCad, suitable for Python projects, implementing clone type separation as well as co-changed clone detection.

## Prerequisites

1. [NiCad-6.2](https://www.txl.ca/txl-nicaddownload.html)

2. Java17

## Clone Type Separation

This process is implemented by  *CloneSeparator.java*.

- Input : *InputCS*.If you don't have this folder, you need to create it by yourself and then, put the NiCad result files in this folder.As shown in the figure below.Each project detects three clone types.

![Image](https://user-images.githubusercontent.com/105061953/168297290-74acbdfd-d80c-4656-9a94-029fc576155d.png)

- Output : *InputCS/Results/“......clone-abstract”*.

## Co-changed Clone Detection

This process is implemented by *FindCoChangeClone.java*.

- Input  : *Input*.If you don't have this folder, you need to create it by yourself. Your operation should look like the following figure.

![Image](https://user-images.githubusercontent.com/105061953/168298321-0143f7b3-8975-4fbd-a841-e1f75be4cc74.png)

- Output : *InputCS/Results/“......clone-abstract”*.

## Co-changed Clone Type Separation

This process is implemented by *CCloneSeparator.java*.

- Input 1 : *InputCCTA*.If you don't have this folder, you need to create it by yourself and then, put the *Co-changed Clone Detection* result files in this folder.As shown in the figure below.

![Image](https://user-images.githubusercontent.com/105061953/168298657-c6745cbf-4349-4d47-96b4-f70c6d52fdd8.png)

- Input 2 : *InputCS*.If you don't have this folder, you need to create it by yourself and then, put the *Clone Type Separation* result files in this folder.As shown in the figure below.

![Image](https://user-images.githubusercontent.com/105061953/168298875-e27b5f34-a7e0-49ff-89b8-42d49add2d37.png)

- Output.The result is written into the *FinalResults* file in the *Co-changed Clone Detection* result files.

## Bug-prone Co-changed Clone Detection

This process is implemented by *FindBuggyCClone.java*.
