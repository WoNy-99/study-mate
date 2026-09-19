# Study Mate

강의 녹음과 필기 자료를 텍스트로 변환하고, AI 요약·퀴즈·복습 일정으로 연결하는 Android 학습 도우미 프로젝트입니다.

## 주요 기능

- 강의 녹음과 Google Speech-to-Text 변환
- 이미지·PDF의 Azure OCR
- 녹음문과 필기 내용을 결합한 AI 요약
- 요약 기반 객관식 퀴즈 생성
- 시간표와 복습 알림

## 담당 내용

- 팀장으로 계획, 구현, 설계 전반 주도
- 메인 백엔드 코드 작성

## 현재 저장소 범위

당시 보존된 `app/src/main` 소스 스냅샷입니다. Gradle 루트 파일이 없어 `app/build.gradle.example`을 참고해 Android Studio 프로젝트에 모듈을 연결해야 합니다. PDF 기능은 `com.tom-roush:pdfbox-android:2.0.27.0` 의존성을 사용합니다.

외부 서비스 키는 `BuildConfig` 값으로 주입하도록 정리했습니다. `gradle.properties.example`을 개인 `~/.gradle/gradle.properties` 또는 안전한 로컬 설정으로 옮기고 실제 값을 입력하세요. 서비스 계정 개인키는 앱에 포함하지 말고 서버에서만 사용해야 합니다.

원본 서비스 계정 JSON, 하드코딩 API 키, 서명 URL, 팀원 개인 정보와 캐시는 제외했습니다.

