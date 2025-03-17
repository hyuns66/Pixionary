# 🚀 Pixionary
갤럭시 기기의 이미지를 검색할 수 있는 서비스 입니다.
특정 단어나 태그 기반이 아닌 문장 기반 검색이므로 찾고싶은 이미지를 묘사하는 어떠한 문장을 입력만 하면 비슷한 이미지들을 검색해서 찾아드립니다.

![Project Description](https://github.com/user-attachments/assets/724c4fce-5258-419d-8dc7-89fb93a51a71)

## 📖 개요 (Overview)
[이미지 검색]  
https://github.com/openai/CLIP
CLIP 모델을 활용하여 이미지와 텍스트를 하나의 벡터공간으로 임베딩 시킵니다.
앱의 초기화 단계에서 갤러리의 모든 이미지에 대한 임베딩 과정을 거치며 임베딩이 완료된 이미지에 한해 검색기능을 수행할 수 있습니다.
 
[문서 검색]  
MLKit에서 제공하는 텍스트 인식 모델을 활용하여 텍스트위주의 문서검색을 수행할 수 있습니다.
카메라 모듈을 통해 문서 사진을 촬영하고, 텍스트를 인식하여 데이터베이스에 저장합니다.
이후 특정 키워드를 입력함으로써 해당 키워드가 포함된 문서를 검색할 수 있습니다.


## 🔧 기술 스택 (Tech Stack)
- **언어(Language)**: `Kotlin`
- **Android SDK**
  - Compile SDK Version: 34
  - Min SDK Version: 26
  - Target SDK Version: 34
- **Gradle**: 8.0 이상
- **데이터베이스(Database)**: `ObjectBox`, `DataStore`
- **의존성 주입**: `Hilt`
- **기타**: `Pytorch`, `ONNX`, `MLKit`, `WorkManager`, `CameraX`


## 설치 및 실행 방법 (Installation & Run) 
> 다량의 이미지 분석시 발생할 수 있는 발열문제를 해결한 이후 배포할 예정입니다.
