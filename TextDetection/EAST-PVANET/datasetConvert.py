# coding:utf-8
import glob
import csv
import cv2
import time
import os
import numpy as np
import scipy.optimize
import matplotlib.pyplot as plt
import matplotlib.patches as Patches
from shapely.geometry import Polygon

errorCount = 0

def get_images():
    files = []
    for ext in ['txt']:
        files.extend(glob.glob(
            os.path.join('icdar2015', '*.{}'.format(ext))))
    return files

image_list = np.array(get_images())
print('{} training images in {}'.format(image_list.shape[0], 'icdar2015'))
index = np.arange(0, image_list.shape[0])
print('index : ', index)
for i in index:
    im_fn = image_list[i]
    print('original : ', im_fn)
    im_fn2 = im_fn.replace('gt_', '')
    print('rename : ', im_fn2)
    os.rename(im_fn, im_fn2)	