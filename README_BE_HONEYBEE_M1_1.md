# BE HONEYBEE — M1.1 (Bee Art + Wing Animation)

기존 M1의 물리·맵·카메라·일시정지 구조를 유지하고 픽셀 아트 일벌을 추가했습니다.

## 추가된 부분
- `assets/bee_flight.png` : 192 × 40 RGBA 스프라이트 시트(48 × 40 크기의 4프레임). 직접 제작한 프로젝트 전용 원본입니다.
- `Main.java` : `Texture` → `TextureRegion.split()` → `Animation<TextureRegion>` → `SpriteBatch`로 애니메이션 표시.
- 바라보는 방향에 따라 좌우 반전, 게임 일시정지 시 애니메이션 정지.
- 기존 플레이어 AABB(28 × 24)는 유지하며 그림(48 × 40)만 충돌체보다 크게 표시.

## 사용 방법
Android Studio에서 이 폴더(안에 `settings.gradle`이 있는 폴더)를 열고 Gradle 동기화 후
`lwjgl3/src/main/java/io/jbnu/test/lwjgl3/Lwjgl3Launcher.java`의 `main()`을 실행하세요.

조작: A/D/방향키 이동, Space 반복 날갯짓, P/ESC 일시정지.

## 참고
프로젝트 ZIP은 정적 코드와 에셋으로 구성되며 이 환경에서는 Gradle 의존성 다운로드가 제한되어 실제 실행 테스트는 못 했습니다.
현재 버전은 기초 비행 애니메이션이며 날개 프레임은 이동 여부와 무관하게 계속 반복됩니다.
