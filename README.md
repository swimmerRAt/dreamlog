# 안드로이드 페이지 구조 안내
이 프로젝트는 Java 기반 Android 앱이며, 화면 UI는 `app/src/main/java/com/example/basicandroidapp/` 경로에 페이지별 Java 파일로 분리해서 관리합니다.
## 디렉토리 구조
```app/src/main/java/com/example/basicandroidapp/
├── MainActivity.java
├── Login.java
├── Homepage.java
└── Mypage.java
```
## 페이지 파일 위치
새 페이지를 추가하거나 기존 페이지를 수정할 때는 아래 경로에서 작업합니다.

```app/src/main/java/com/example/basicandroidapp/```

예를 들어 설정 페이지를 추가한다면 다음처럼 새 파일을 만듭니다.

```app/src/main/java/com/example/basicandroidapp/Settings.java```

Java 클래스명은 파일명과 맞추고, 대문자로 시작하는 PascalCase를 사용합니다.
## 각 페이지 역할
* Login.java
* Homepage.java
* Mypage.java
* MainActivity.java : 페이지 연결, 화면 전환, 하단 네비게이션 로직 작성
## 새 페이지 추가 방법
1. app/src/main/java/com/example/basicandroidapp/ 경로에 새 Java 파일을 추가합니다.
2. 파일명과 클래스명을 대문자로 시작하게 작성합니다.
3. 페이지 UI는 해당 파일 안에서 작성합니다.
4. MainActivity.java의 Page enum에 새 페이지 값을 추가합니다.
5. showPage() 메서드에서 새 페이지 클래스를 연결합니다.
6. 필요하면 하단 네비게이션 버튼도 추가합니다.
# 실행 방법
프로젝트 경로에서 터미널을 열고 아래의 명령어를 순차적으로 입력하면 됨
1. 첫번째 터미널
- 안드로이드 핸드폰 에뮬레이터 실행하는 명령어

```/Users/suyeong/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1```

2. 두번째 터미널
- 앱 설치하는 명령어, 앱 설치하는 명령어로 맨 처음, 앱 수정 시 재실행해줘야됨

```/Users/suyeong/Library/Android/sdk/platform-tools/adb wait-for-device```

```./gradlew :app:installDebug```

```/Users/suyeong/Library/Android/sdk/platform-tools/adb shell am start -n com.example.basicandroidapp/.MainActivity```
