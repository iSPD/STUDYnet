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
    for ext in ['jpg', 'png', 'jpeg', 'JPG']:
        files.extend(glob.glob(
            os.path.join('synthDataset/synthDataset', '*.{}'.format(ext))))
    return files

def load_annoataion(p):
    '''
    load annotation from the text file
    :param p:
    :return:
    '''
    text_polys = []
    text_tags = []
    if not os.path.exists(p):
        return np.array(text_polys, dtype=np.float32)
    with open(p, 'r') as f:
        #firstReader = csv.reader(f)
        #print(len(firstReader))

        #if len(firstReader) > 1:
        if True:
            reader = csv.reader(f)
            for line in reader:
                if len(line) < 2:
                    return np.array(text_polys, dtype=np.float32)

                #print('line1 : ',line)
                label = line[-1]
                #print('label : ', label)
                # strip BOM. \ufeff for python3,  \xef\xbb\bf for python2
                line = [i.strip('\ufeff').strip('\xef\xbb\xbf') for i in line]
                #print('line2 : ',line)
                #print('line3 : ',len(line))

                x1, y1, x2, y2, x3, y3, x4, y4 = list(map(float, line[:8]))
                text_polys.append([[x1, y1], [x2, y2], [x3, y3], [x4, y4]])
                if label == '*' or label == '###':
                    text_tags.append(True)
                else:
                    text_tags.append(False)
        else:
            line = f.readline()  
            print('line1 : ',line)
            line = line.split(' ')
            label = line[4]
            print('label : ', label)
            # strip BOM. \ufeff for python3,  \xef\xbb\bf for python2
            #line = [i.strip('\ufeff').strip('\xef\xbb\xbf') for i in line]
            print('line2 : ',line)
            #print('line3 : ',len(line))

            x1, y1, x2, y2, x3, y3, x4, y4 = list(map(float, line[:8]))
            text_polys.append([[x1, y1], [x2, y2], [x3, y3], [x4, y4]])
            if label == '*' or label == '###':
                text_tags.append(True)
            else:
                text_tags.append(False)

        return np.array(text_polys, dtype=np.float32), np.array(text_tags, dtype=np.bool)


def polygon_area(poly):
    '''
    compute area of a polygon
    :param poly:
    :return:
    '''
    edge = [
        (poly[1][0] - poly[0][0]) * (poly[1][1] + poly[0][1]),
        (poly[2][0] - poly[1][0]) * (poly[2][1] + poly[1][1]),
        (poly[3][0] - poly[2][0]) * (poly[3][1] + poly[2][1]),
        (poly[0][0] - poly[3][0]) * (poly[0][1] + poly[3][1])
    ]
    return np.sum(edge)/2.


def check_and_validate_polys(polys, tags, xxx_todo_changeme, filename, index):
    '''
    check so that the text poly is in the same direction,
    and also filter some invalid polygons
    :param polys:
    :param tags:
    :return:
    '''

    global errorCount
    (h, w) = xxx_todo_changeme
    if polys.shape[0] == 0:
        return polys
    polys[:, :, 0] = np.clip(polys[:, :, 0], 0, w-1)
    polys[:, :, 1] = np.clip(polys[:, :, 1], 0, h-1)

    validated_polys = []
    validated_tags = []

    isError = False 	

    for poly, tag in zip(polys, tags):
        p_area = polygon_area(poly)
        if abs(p_area) < 1:
            # print poly
            print('invalid poly : ', filename)
            isError = True
            continue
        if p_area > 0:
            print('poly in wrong direction : ', filename)
            poly = poly[(0, 3, 2, 1), :]
            isError = True
            continue			
        validated_polys.append(poly)
        validated_tags.append(tag)

    if isError == True:
        errorCount = errorCount + 1
        print('index : ', index)
        print('errorCount : ', errorCount)

    return np.array(validated_polys), np.array(validated_tags)

image_list = np.array(get_images())
print('{} training images in {}'.format(image_list.shape[0], 'synthDataset/synthDataset'))
index = np.arange(0, image_list.shape[0])
print('index : ', index)
for i in index:
    try:
        im_fn = image_list[i]
        im = cv2.imread(im_fn)
        # print im_fn
        h, w, _ = im.shape
        txt_fn = im_fn.replace(os.path.basename(im_fn).split('.')[1], 'txt')

        if not os.path.exists(txt_fn):
            print('text file {} does not exists'.format(txt_fn))
            continue

        text_polys, text_tags = load_annoataion(txt_fn)

        text_polys, text_tags = check_and_validate_polys(text_polys, text_tags, (h, w), txt_fn, i)
    except Exception as e:
                import traceback
                traceback.print_exc()
                continue