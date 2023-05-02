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

<div align="left">
<img width="100%" src="https://github.com/iSPD/STUDYnet/blob/main/images/datasetExample2.PNG"/>
</div>

---

## Optical Character Recognition

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
<img width="45%" src="https://github.com/iSPD/STUDYnet/blob/main/images/%EC%9E%90%EB%8F%99%EC%B1%84%EC%A0%90.gif"/>
</div>

---
## LICENSE
- [MIT](https://github.com/iSPD/STUDYnet/blob/main/LICENSE.md)

---
## 문의 사항
- (주)iSPD 정한별 대표
- ispd_paul@outlook.kr
- 010-9930-1791
