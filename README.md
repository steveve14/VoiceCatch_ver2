#  AI 분석 Text 기반 보이스 피싱 감지 앱 
개인 프로젝트

## 데이터 수집 
### 보이스 피싱
#### KT 통신 빅데이터 플랫폼 - [휴대전화 스팸트랩 음성 수집 내역](https://bdp.kt.co.kr/invoke/SOKBP2602/)
#### 금융 감독원 - [보이스피싱 체험관](https://www.fss.or.kr/fss/bbs/B0000203/list.do?menuNo=200686):
whisper 를 통한 stt 로 만들어서 사용

### 일반 데이터
#### AIHUB - [감정 분류를 위한 대화 음성 데이터셋] (https://aihub.or.kr/aihubdata/data/view.do?currMenu=115&topMenu=100&dataSetSn=263)
## 모델
```
  
  
  모델 보이스 피싱 구분 - distilkobert 파인 튜닝을 하여 사용
  
  모델 whisper - base - 양자화 - 한글 커스텀 모델
    코드 Whisper_Ko.ipynb
  
  안드로이드 whisper 작동 코드 
    https://github.com/vilassn/whisper_android
    참조 2025-8 기준 java 버전 오류로 인해 cpp 버전 사용 
```
  ### EX
```
  보이스 피싱 데이터의 부족으로 인한 모델 정확도 보장 x

  사이드 탭에 모델 작동 코드 존제 
  실시간 기능 없음 녹음 기능 및 파일 불러오기 기능 존제 하지만 
  특정 wav 파일만 가능하므로 나중에 수정 필요
  모든 기능은 온보드로만 작동하도록 되어있음 서버 연결 필요 X
```
