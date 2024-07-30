# DWT Compression for Image Encoding and Decoding

This project provides an understanding of image compression using wavelets. The implementation involves reading an RGB image file, converting the image pixels to a Discrete Wavelet Transform (DWT) representation for each channel, and decoding the representations using specified levels of low-pass coefficients.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Usage](#usage)
- [Implementation Details](#implementation-details)
  - [Encoding](#encoding)
  - [Decoding](#decoding)
  - [Progressive Decoding](#progressive-decoding)
- [Examples](#examples)

## Overview

This program reads an RGB file, converts the image pixels to a DWT representation for each channel, and decodes the representations using specified levels of low-pass coefficients. The input image is of size 512x512, making it easy to encode and decode.

## Features

- Encoding of RGB images using DWT.
- Decoding using specified levels of low-pass coefficients.
- Progressive decoding to visualize the improvement of image details.

## Usage
- java MyExe Image.rgb n
Image.rgb: The input image file in RGB format.
n: An integer from 0 to 9 that defines the low-pass level for decoding. A value of -1 triggers progressive decoding.

## Implementation Details
### Encoding
Convert each row (for each channel) into low-pass and high-pass coefficients.
Apply the same process to each column on the output of the row processing.
Recurse through the process for rows first, then columns, at each iteration, operating on the low-pass section until the appropriate level is reached.

### Decoding
Zero out all the high-pass coefficients once the appropriate level is reached.
Perform a recursive Inverse DWT (IDWT) from the encoded level up to level 9 (the image level).
Zero out the unrequested coefficients and then perform the IDWT.

### Progressive Decoding
When n = -1:
Create the entire DWT representation up to level 0.
Recursively decode each level and display the output.
The image will progressively improve with details from level 0 to level 9.

## Examples
- javac .\CompressImage.java; java CompressImage .\roses_image_512x512.rgb 8; // 75 percent compression
- javac .\CompressImage.java; java CompressDWT .\roses_image_512x512.rgb -1 // Progressive reconstruction
