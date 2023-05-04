## Text Detection

- Tensorflow 기반, EAST-PVANET 을 참고하여 한국어, 영어 책표지 단어 Dectection 모델 생성

- 안드로이드 모바일용으로 모델 변환

- Detection 용 Synthetic Data Generation Tool 제작

### Dataset

  - 인조 데이터 생성 
    
    * 다양한 배경, 165개 글씨체, 글자색상, 글자 굵기, 음영, 테두리, 기울기, Curved, Vertical을 적용한 20만개 데이터 생성 
    
  - 한국어+영어 책표지 실제 데이터 약 12만개

  - [ICDAR2015 Scene Text](https://rrc.cvc.uab.es/?ch=4&com=downloads)
  
### Train
  
  - Train
  
``` Python
$ python multigpu_train.py \
--gpu_list=0 \
--input_size=512 \
--batch_size_per_gpu=8 \
--checkpoint_path=east_synth_synth2_pvanet_rbox_addVert/ \
--text_scale=512 \
--training_data_path=synthDataset/synthDataset2 \
--geometry=RBOX \
--learning_rate=0.0001 \
--num_readers=24 \
--pretrained_model_path=checkpoint/PVA9.1_ImgNet_COCO_VOC0712plus_compressed.ckpt
```
  - Test
  
  ``` Python
  $ python eval.py --test_data_path=../dataset/picturebookKor_covers2 --gpu_list=1 --checkpoint_path=east_synth_synth2_pvanet_rbox_addVert/ --checkpoint_path_more=model.ckpt-301991 --output_dir=output_picturebookKor_covers2_2
  ```
  
  - 모델 변환 pb -> tflite
  
  ``` Python
  $ tflite_convert \
  --output_file=frozen_east_pvanet3_model.tflite \
  --graph_def_file=frozen_east_pvanet_sun_model.pb \
  --input_arrays=input_images \
  --output_arrays='feature_fusion/Conv_7/Sigmoid','feature_fusion/concat_3' \
  --input_shapes=1,320,320,3 \
  --inference_input_type=FLOAT \
  --inference_type=FLOAT \
  --allow_custom_ops \
  --mean_values=128 \
  --std_dev_values=128 \
  --default_ranges_min=0 \
  --default_ranges_max=6

  ```
  
  
## Handwriting Optical Character Recognition (Korean, English)

- Pytorch 기반, deep-text-recognition-benchmark(clovaai) 참고하여 한국어, 영어 필기 글자체 인식 모델 생성

- 안드로이드 모바일용으로 모델 변환

- Recognition Training을 위한 Synthetic Data Generation Tool 제작

- 한글 필기체 데이터셋 : AI-Hub 한글 손글씨 데이터셋 50만여개 가공, 손글씨 수집데이터(3천여개), 인조 데이터(4백만개)

- 영문 필기체 데이터셋 : IAM Handwrite 데이터셋 10만여개, 손글씨 수집데이터(7천여개), 인조 데이터(4백만개)

### Dataset

  - 한국어 필기체 데이터 : 인조 데이터 생성 + AI-Hub 데이터 와 실제 손글씨 수집 데이터
  
    (1) 인조 데이터 - 한글 필기체 폰트 174개로 구성한 한글 단어 5만여개, 총 4백만개 단어 이미지 데이터셋
  
    (2) AI-Hub 한글 손글씨 데이터셋 50만여개 가공(노이즈, 배경합성)
  
    (3) 실제 손글씨 수집데이터(3천여개) 에서 단어 bbox 작업 후 이미지 생성.
  
    (4) 인조 데이터, AI-Hub 데이터 와 실제 손글씨 수집데이터를 혼합해서 annotation 파일 작성.
  
  - 영어 필기체 데이터 : 인조 데이터 생성 + IAM 데이터 와 실제 손글씨 수집 데이터 
  
    (1) 인조 데이터 - 영어 필기체 폰트 182개로 구성한 영어 단어 2만여개로 밑줄, 기호, 노이즈 합성. 총 4백만개 단어 이미지 데이터셋	
  
    (2) 웹에서 수집한 영문 필기 데이터(일기,에세이 등) 에서 단어 bbox 작업 후 이미지 생성.
  
    (3)	인조 데이터, IAM 데이터 와 실제 손글씨 수집데이터를 혼합해서 annotation 파일 작성.
    
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
