## Handwriting Optical Character Recognition (Korean, English)

- deep-text-recognition-benchmark 참고하여 한국어, 영어 필기 글자체 인식 모델 생성

- 안드로이드 모바일용으로 모델 변환

- 한글 필기체 데이터셋 : AI-Hub 한글 손글씨 데이터셋 50만여개 가공, 손글씨 수집데이터(3천여개), 가공데이터(4백만개)

- 영문 필기체 데이터셋 : IAM Handwrite 데이터셋 10만여개, 실제 손글씨 수집데이터(7천여개), 자체 제작 가공데이터(4백만개)

### Dataset

  - 한국어 필기체 데이터 : 가공데이터 생성 + AI-Hub 데이터 와 실제 손글씨 수집 데이터 혼합 트레이닝.
  
    (1) 가공데이터 - 한글 필기체 폰트 174개로 구성한 한글 단어 5만여개, 총 4백만개 단어 이미지 데이터셋
  
    (2) AI-Hub 한글 손글씨 데이터셋 50만여개 가공(노이즈, 배경합성)
  
    (3) 실제 손글씨 수집데이터(3천여개) 에서 단어 bbox 작업 후 이미지 생성.
  
    (4) 가공데이터, AI-Hub 데이터 와 실제 손글씨 수집데이터를 혼합해서 annotation 파일 작성.
  
  - 영어 필기체 데이터 : 가공데이터 생성 + IAM 데이터 와 실제 손글씨 수집 데이터 혼합 트레이닝.
  
    (1) 가공데이터 - 영어 필기체 폰트 182개로 구성한 영어 단어 2만여개로 밑줄, 기호, 노이즈 합성. 총 4백만개 단어 이미지 데이터셋	
  
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
  - [TorchScript](https://pytorch.org/docs/stable/jit.html#torchscript)
  
    ```
    CUDA_VISIBLE_DEVICES=-1 python3 demo_for_torchscript.py --Transformation None --FeatureExtraction VGG --SequenceModeling None --Prediction CTC --image_folder ./demo_image --PAD --saved_model ./saved_models/handwrite_eng/best_accuracy.pth
    ```

## Dataset 가공
  - font file, word list, 합성할 배경이미지 준비
  - 다양한 글씨체, 농도, 배경, 노이즈, 음영 적용
  
  <div align="left">
  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/25589_102_ft1.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/25594_112_ft1.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/485704_964_ft19.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/485771_1098_ft19.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51076_1_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51106_61_ft2.jpg"/>
  <br>
  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51128_105_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51158_165_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51491_831_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51664_1177_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/51905_1659_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/kor/76827_426_ft3.jpg"/>
  
</div>
  <div align="left">
  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/103109_59752_ft4.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/44153_270_ft2.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/661339_226_ft30.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/661349_266_ft30.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/661562_1118_ft30.jpg"/> 
  <br>
  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/66185_227_ft3.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/741531_56481_ft33.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/741532_56485_ft33.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/88273_408_ft4.jpg"/>  <img src="https://github.com/iSPD/STUDYnet/blob/main/images/data/eng/88312_564_ft4.jpg"/>
  </div>
