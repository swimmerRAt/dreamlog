# 실행 방법
프로젝트 경로에서 터미널을 열고 아래의 명령어를 순차적으로 입력하면 됨
1. 첫번째 터미널
```/Users/suyeong/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1```
2. 두번째 터미널
```/Users/suyeong/Library/Android/sdk/platform-tools/adb wait-for-device```
```./gradlew :app:installDebug```
```/Users/suyeong/Library/Android/sdk/platform-tools/adb shell am start -n com.example.basicandroidapp/.MainActivity```