# STUDYnet
**비대면 AI 학습지 · 동화책 솔루션**
- 인공지능 영상 · 문자 인식기술을 이용하여, 태블릿 또는 스마트폰 전 · 후방 카메라로 학습지·동화책의 캐릭터 및 TEXT를 실시간 인식하여 각 페이지에 매칭되는 음원, AR, 동영상 등을 자동 재생해주며, 책 위에서의 손가락 MOTION 인식과 (일반)펜 필기인식 기술로, 교사 방문을 대체하는 비대면 AI · 학습지 · 동화책 솔루션.

<div align="center">
<img width="70%" src="https://github.com/iSPD/STUDYnet/blob/main/images/introduce2.png"/>
</div>

## 스마트폰 솔루션
- 인공지능 영상/문자 인식기술을 이용, 스마트폰 카메라로 동화책의 캐릭터, 글자를 실시간으로 인식하여 각 페이지에 매칭되는 소리동화, 동영상, AR자료 등을 자동 재생해주는 기술.

<div align="center">
<img width="70%" src="https://github.com/iSPD/STUDYnet/blob/main/images/mommyBook.png"/>
</div>

## 테블릿 솔루션
- 비대면 AI 학습지 솔루션. 책 위에서 손가락 Motion인식과 (일반)펜 필기인식 기술 적용.

<div align="center">
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook1.png"/><img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook2.png"/>

<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook3.png"/><img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook4.png"/>

<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook5.png"/><img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook6.png"/>

<img width="90%" src="https://github.com/iSPD/STUDYnet/blob/main/images/studyBook7.png"/>
</div>

---

## 🕰️ **개발 기간**

- 2020년 12월 31일 ~ 2022년 12월 30일

---

## 제품 성능

<div align="center">
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%EC%8B%9C%ED%97%98%EC%84%B1%EC%A0%81%EC%84%9C1.PNG"/> <img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%EC%8B%9C%ED%97%98%EC%84%B1%EC%A0%81%EC%84%9C2.PNG"/>
</div>

---

## Classfication AI Model
동화책 및 학습지의 각 페이지를 가공하여, 여러개의 데이터셋으로 만든 후 스마트폰 및 테블릿 전,후방 카메라에서 페이식 인식을 위해 Classification Model에서 Training

### 사용모델

[MobileNet_v2_1.4_224](https://github.com/tensorflow/models/tree/master/research/slim)

### 데이터셋 가공
- 휴대폰 및 테블릿 카메라로 동화책 및 학습지를 인식 시 일어날수 있는 환경변수들을 감안하여 데이터셋 가공. 아래와 같이 가공
  - Scale(크기)
  
  - Bright(밝기)
  
  - Contrast(대비)
  
  - Rotation(회전)
  
  - Blur
  
  - 빛반사
  
  - 그림자
  
  - 색감변경

- tfrecord 변환 예제
```
python download_and_convert_data_custom.py --dataset_dir=dataBook2/KOR_R/dataset_black
```

<div align="center">
<img width="100%" src="https://github.com/iSPD/STUDYnet/blob/main/images/datasetExample2.PNG"/>
<b><420개로 가공. 위 사진은 예시></b>
</div>
  
### Train 예제
```
  CUDA_VISIBLE_DEVICES=0 python train_image_classifier.py
    --alsologtostderr \
    --checkpoint_path=english/trainEnglish \
    --dataset_dir=datasets/english/recordEnglish \
    --dataset_name=book \
    --dataset_split_name=train \
    --model_name=mobilenet_v2_140
```
  
### TFLite 변환 예제
```
  python3 export_inference_graph.py \
  --alsologtostderr \
  --model_name=mobilenet_v2_140 \
  --image_size=224 \
  --output_file=studyNet/result_korean/mobilenet_v2_224_14.pb
```
  
```
  bazel-bin/tensorflow/python/tools/freeze_graph \
    --input_graph=/home/khkim/tensorflow/tensorflow-recent/tensorflow/book/trains/train_thumb/mobilenet_v2_224_14.pb \
    --input_checkpoint=/home/khkim/tensorflow/tensorflow-recent/tensorflow/bookDemo/trains/train_thumb/model.ckpt-50000 \
    --input_binary=true --output_graph=/home/khkim/tensorflow/tensorflow-recent/tensorflow/bookDemo/trains/train_thumb/book_thumb_big_mobilenet_v2_14.pb \
    --output_node_names=MobilenetV2/Predictions/Reshape_1
    --output_node_names=MobilenetV1/Sigmoid
```  
  
```
  ./bazel-bin/tensorflow/lite/python/tflite_convert \
  --output_file=bookDemo/trains/train_thumb/book_thumb_big_mobilenet_v2_14.tflite \
  --graph_def_file=bookDemo/trains/train_thumb/book_thumb_big_mobilenet_v2_14.pb \
  --input_arrays=input \
  --output_arrays=MobilenetV2/Predictions/Reshape_1 \
  --input_shapes=1,224,224,3 \
  --inference_input_type=FLOAT \
  --inference_type=FLOAT \
  --allow_custom_ops
```  
  
---

## Handwriting Optical Character Recognition (Korean, English)

- [deep-text-recognition-benchmark](https://github.com/clovaai/deep-text-recognition-benchmark) 참고하여 한국어, 영어 필기 글자체 인식 모델 생성

- 안드로이드 모바일용으로 모델 변환

- 한글 필기체 데이터셋 : AI-Hub 한글 손글씨 데이터셋 50만여개 가공, 손글씨 수집데이터(3천여개), 가공데이터(4백만개)

- 영문 필기체 데이터셋 : IAM Handwrite 데이터셋 10만여개, 실제 손글씨 수집데이터(7천여개), 자체 제작 가공데이터(4백만개)

### Dataset

  - 한국어 필기체 데이터 : 
  
    (1) 가공데이터(한글 필기체 폰트 174개로 구성한 한글 단어 5만여개, 총 4백만개 단어 이미지 데이터셋)
  
    (2) AI-Hub 한글 손글씨 데이터셋 50만여개 가공(노이즈, 배경합성)
  
    (3) 실제 손글씨 수집데이터(3천여개)
  
    (4) 가공데이터, AI-Hub 데이터 와 실제 손글씨 수집데이터를 혼합해서 annotation 파일 작성.
  
  - 영어 필기체 데이터 : 가공데이터 생성 + IAM 데이터 와 실제 손글씨 수집 데이터 혼합 트레이닝.
  
    (1) 가공데이터 - 영어 필기체 폰트 182개로 구성한 영어 단어 2만여개로 밑줄, 기호, 노이즈 합성. 총 4백만개 단어 이미지 데이터셋)	
  
    (2) 웹에서 수집한 영문 필기 데이터(일기,에세이 등) 에서 단어 bbox 작업 후 이미지 생성.
  
    (3)	가공데이터, IAM 데이터 와 실제 손글씨 수집데이터를 혼합해서 annotation 파일 작성.
    
### Train

  - LMDB 변환
    ```
    python3 create_lmdb_dataset.py --inputPath ../dataset/handwrite_eng/ --gtFile ../dataset/handwrite_eng/annotation_train.txt --outputPath ../dataset/handwrite_eng/lmdb_train
    ```
  - Train
    ```
    CUDA_VISIBLE_DEVICES=0 python3 train.py --experiment_name handwrite_eng --train_data ../dataset/lmdb_hw_eng/lmdb_train --valid_data ../dataset/lmdb_hw_eng/lmdb_val --select_data / --batch_ratio 1 --Transformation None --FeatureExtraction VGG --SequenceModeling None --Prediction CTC --valInterval 500 --manualSeed 2223 --PAD --num_iter 300000 --output_channel 512 --hidden_size 256
    ```
  - Test
    ```
    CUDA_VISIBLE_DEVICES=0 python3 test.py --eval_data ../dataset/lmdb_hw_eng/lmdb_test --Transformation None --FeatureExtraction VGG --SequenceModeling None --Prediction CTC --saved_model ./saved_models/handwrite_eng/best_accuracy.pth --PAD --output_channel 512 --hidden_size 256
    ```
    
### 모델 변환
  - Torch Script
  
    ```
    CUDA_VISIBLE_DEVICES=-1 python3 demo_for_torchscript.py --Transformation None --FeatureExtraction VGG --SequenceModeling None --Prediction CTC --image_folder ./demo_image --PAD --saved_model ./saved_models/handwrite_eng/best_accuracy.pth
    ```
---

## Motion Recognition

<div align="center">
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/hand_landmark.png"/> <img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%ED%95%99%EC%8A%B5%ED%99%9C%EB%8F%99.gif"/>
</div>

<div align="center">
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/rockPaper.gif"/> <img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/earthView.gif"/>
</div>

- 구글에서 제공하는 AI Framework인 [MediaPipe](https://github.com/google/mediapipe)에서 Hand landmarks detection 사용

- <b>STUDYnet</b>에서는 위와 같이 손가락 인식 및 Tracking 하여 학습지 터치 활동, 가위바위보 게임 같은 Activity에 적용.

- MediaPipe에서는 다양한 비전 AI기능을 파이프라인 형태로 손쉽게 사용할 수 있도록 프레임워크를 제공. 인체를 대상으로 하는 Detect(인식)에 대해서 얼굴인식, 포즈, 객체감지, 모션트레킹 등 다양한 형태의 기능과 모델을 제공함. python등 다양한 언어을 지원하며, <b>STUDYnet</b>에서는 C++코드를 사용하며, <b>OpenGL ES2.0(Shader)</b>과 연결해서 사용

---

## AI Auto Scoring Solution

<div align="center">
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%EC%9E%90%EB%8F%99%EC%B1%84%EC%A0%90.gif"/> <img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%EC%9E%90%EB%8F%99%EC%B1%84%EC%A0%90%EB%85%B9%ED%99%94.gif"/>
</div>

---
## LICENSE
- [MIT](https://github.com/iSPD/STUDYnet/blob/main/LICENSE.md)

---
## 문의 사항
- (주)iSPD 정한별 대표
- ispd_paul@outlook.kr
- 010-9930-1791
