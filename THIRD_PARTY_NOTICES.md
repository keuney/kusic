# 제3자 라이선스 고지

이 문서는 Keuney Music이 배포물에 포함하는 오픈소스 구성 요소와 그 라이선스를 적는다.

## 이 목록을 만든 방법

`gradlew :app:dependencies --configuration releaseRuntimeClasspath`로 **release 빌드에 실제로 들어가는**
의존성 전체(전이 의존성 포함)를 뽑았다. 각 아티팩트의 라이선스는 기억이나 짐작이 아니라 Gradle 캐시에
내려온 **POM 파일의 licenses 항목**에서 읽었다. POM에 없으면 같은 버전의 부모 POM을 따라갔다.

빌드에만 쓰이는 것(KSP 처리기, 단위·계측 검사 라이브러리, Gradle 플러그인)은 배포물에 들어가지 않으므로
여기 적지 않는다.

## 요약

| 라이선스 | 아티팩트 수 |
| --- | --- |
| Apache-2.0 | 236 |
| BSD-3-Clause | 1 |
| MIT | 1 |
| **합계** | **238** |

## GPL 계열 의존성 상태

**GPL·LGPL·AGPL 라이선스를 가진 의존성은 없다.** 238개 아티팩트의 POM 라이선스 항목을 모두 확인했고
GPL·LGPL·AGPL에 해당하는 것은 하나도 나오지 않았다. 위 표의 세 라이선스가 전부다.

이 사실은 확인 시점(2026-09-04)의 의존성 구성에 대한 것이다. 의존성을 더하거나 올릴 때 이 문서를 다시
확인한다. 특히 새 의존성을 넣을 때는 그 POM의 라이선스를 먼저 본다.

## 배포 형태와 라이선스 의무

이 앱은 개인용 sideload 앱이며 제3자에게 재배포하지 않는다(PRD 1·9). 그래서 라이선스 원문을 배포물에
함께 넣지 않고 여기에 라이선스 종류와 원문 위치만 적는다.

**재배포하게 되면** Apache-2.0의 4(a)·4(d)에 따라 라이선스 사본과 해당 NOTICE 파일을 배포물에 포함해야
하고, BSD-3-Clause와 MIT의 저작권 표시와 허가 문구도 함께 넣어야 한다. 그때는 이 문서만으로 부족하다.

- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
- BSD 3-Clause: https://opensource.org/license/bsd-3-clause
- MIT: https://opensource.org/license/mit

## Apache-2.0

Android Jetpack(androidx.*), Media3, Compose, Room, DataStore, Lifecycle, Navigation, Hilt/Dagger,
Kotlin 표준 라이브러리와 kotlinx, Ktor, OkHttp/Okio, Coil, Guava가 여기 속한다.

목록의 `*-bom` 세 개(`compose-bom`, `kotlinx-coroutines-bom`, `kotlinx-serialization-bom`)는 버전만
정하는 플랫폼이며 코드가 들어가지 않는다. 의존성 구성을 그대로 적기 위해 함께 남긴다.

- `androidx.activity:activity:1.13.0`
- `androidx.activity:activity-compose:1.13.0`
- `androidx.activity:activity-ktx:1.13.0`
- `androidx.annotation:annotation:1.9.1`
- `androidx.annotation:annotation-experimental:1.5.0`
- `androidx.annotation:annotation-jvm:1.9.1`
- `androidx.appcompat:appcompat-resources:1.7.1`
- `androidx.arch.core:core-common:2.2.0`
- `androidx.arch.core:core-runtime:2.2.0`
- `androidx.autofill:autofill:1.0.0`
- `androidx.collection:collection:1.5.0`
- `androidx.collection:collection-jvm:1.5.0`
- `androidx.collection:collection-ktx:1.5.0`
- `androidx.compose:compose-bom:2025.08.00`
- `androidx.compose.animation:animation:1.10.5`
- `androidx.compose.animation:animation-android:1.10.5`
- `androidx.compose.animation:animation-core:1.10.5`
- `androidx.compose.animation:animation-core-android:1.10.5`
- `androidx.compose.foundation:foundation:1.10.5`
- `androidx.compose.foundation:foundation-android:1.10.5`
- `androidx.compose.foundation:foundation-layout:1.10.5`
- `androidx.compose.foundation:foundation-layout-android:1.10.5`
- `androidx.compose.material:material-icons-core:1.7.8`
- `androidx.compose.material:material-icons-core-android:1.7.8`
- `androidx.compose.material:material-ripple:1.9.0`
- `androidx.compose.material:material-ripple-android:1.9.0`
- `androidx.compose.material3:material3:1.3.2`
- `androidx.compose.material3:material3-android:1.3.2`
- `androidx.compose.runtime:runtime:1.11.2`
- `androidx.compose.runtime:runtime-android:1.11.2`
- `androidx.compose.runtime:runtime-annotation:1.11.2`
- `androidx.compose.runtime:runtime-annotation-android:1.11.2`
- `androidx.compose.runtime:runtime-retain:1.11.2`
- `androidx.compose.runtime:runtime-retain-android:1.11.2`
- `androidx.compose.runtime:runtime-saveable:1.11.2`
- `androidx.compose.runtime:runtime-saveable-android:1.11.2`
- `androidx.compose.ui:ui:1.11.2`
- `androidx.compose.ui:ui-android:1.11.2`
- `androidx.compose.ui:ui-geometry:1.11.2`
- `androidx.compose.ui:ui-geometry-android:1.11.2`
- `androidx.compose.ui:ui-graphics:1.11.2`
- `androidx.compose.ui:ui-graphics-android:1.11.2`
- `androidx.compose.ui:ui-text:1.11.2`
- `androidx.compose.ui:ui-text-android:1.11.2`
- `androidx.compose.ui:ui-unit:1.11.2`
- `androidx.compose.ui:ui-unit-android:1.11.2`
- `androidx.compose.ui:ui-util:1.11.2`
- `androidx.compose.ui:ui-util-android:1.11.2`
- `androidx.concurrent:concurrent-futures:1.2.0`
- `androidx.core:core:1.18.0`
- `androidx.core:core-ktx:1.18.0`
- `androidx.core:core-viewtree:1.0.0`
- `androidx.customview:customview:1.0.0`
- `androidx.customview:customview-poolingcontainer:1.0.0`
- `androidx.datastore:datastore-core:1.2.1`
- `androidx.datastore:datastore-core-android:1.2.1`
- `androidx.datastore:datastore-core-okio:1.2.1`
- `androidx.datastore:datastore-core-okio-jvm:1.2.1`
- `androidx.datastore:datastore-preferences-core:1.2.1`
- `androidx.datastore:datastore-preferences-core-android:1.2.1`
- `androidx.datastore:datastore-preferences-proto:1.2.1`
- `androidx.documentfile:documentfile:1.0.0`
- `androidx.dynamicanimation:dynamicanimation:1.0.0`
- `androidx.emoji2:emoji2:1.4.0`
- `androidx.exifinterface:exifinterface:1.4.2`
- `androidx.fragment:fragment:1.5.1`
- `androidx.graphics:graphics-path:1.0.1`
- `androidx.interpolator:interpolator:1.0.0`
- `androidx.legacy:legacy-support-core-utils:1.0.0`
- `androidx.lifecycle:lifecycle-common:2.11.0`
- `androidx.lifecycle:lifecycle-common-java8:2.11.0`
- `androidx.lifecycle:lifecycle-common-jvm:2.11.0`
- `androidx.lifecycle:lifecycle-livedata:2.11.0`
- `androidx.lifecycle:lifecycle-livedata-core:2.11.0`
- `androidx.lifecycle:lifecycle-livedata-core-ktx:2.11.0`
- `androidx.lifecycle:lifecycle-process:2.11.0`
- `androidx.lifecycle:lifecycle-runtime:2.11.0`
- `androidx.lifecycle:lifecycle-runtime-android:2.11.0`
- `androidx.lifecycle:lifecycle-runtime-compose:2.11.0`
- `androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0`
- `androidx.lifecycle:lifecycle-runtime-ktx-android:2.11.0`
- `androidx.lifecycle:lifecycle-service:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-android:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose-android:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.11.0`
- `androidx.loader:loader:1.0.0`
- `androidx.localbroadcastmanager:localbroadcastmanager:1.0.0`
- `androidx.media:media:1.7.0`
- `androidx.media3:media3-common:1.11.0`
- `androidx.media3:media3-container:1.11.0`
- `androidx.media3:media3-database:1.11.0`
- `androidx.media3:media3-datasource:1.11.0`
- `androidx.media3:media3-decoder:1.11.0`
- `androidx.media3:media3-exoplayer:1.11.0`
- `androidx.media3:media3-extractor:1.11.0`
- `androidx.media3:media3-session:1.11.0`
- `androidx.navigation:navigation-common:2.10.0`
- `androidx.navigation:navigation-common-android:2.10.0`
- `androidx.navigation:navigation-compose:2.10.0`
- `androidx.navigation:navigation-compose-android:2.10.0`
- `androidx.navigation:navigation-runtime:2.10.0`
- `androidx.navigation:navigation-runtime-android:2.10.0`
- `androidx.navigationevent:navigationevent:1.1.2`
- `androidx.navigationevent:navigationevent-android:1.1.2`
- `androidx.navigationevent:navigationevent-compose:1.1.2`
- `androidx.navigationevent:navigationevent-compose-android:1.1.2`
- `androidx.print:print:1.0.0`
- `androidx.profileinstaller:profileinstaller:1.4.1`
- `androidx.room:room-common:2.8.4`
- `androidx.room:room-common-jvm:2.8.4`
- `androidx.room:room-runtime:2.8.4`
- `androidx.room:room-runtime-android:2.8.4`
- `androidx.savedstate:savedstate:1.5.0`
- `androidx.savedstate:savedstate-android:1.5.0`
- `androidx.savedstate:savedstate-compose:1.5.0`
- `androidx.savedstate:savedstate-compose-android:1.5.0`
- `androidx.savedstate:savedstate-ktx:1.5.0`
- `androidx.sqlite:sqlite:2.6.2`
- `androidx.sqlite:sqlite-android:2.6.2`
- `androidx.sqlite:sqlite-framework:2.6.2`
- `androidx.sqlite:sqlite-framework-android:2.6.2`
- `androidx.startup:startup-runtime:1.1.1`
- `androidx.tracing:tracing:1.2.0`
- `androidx.transition:transition:1.6.0`
- `androidx.vectordrawable:vectordrawable:1.1.0`
- `androidx.vectordrawable:vectordrawable-animated:1.1.0`
- `androidx.versionedparcelable:versionedparcelable:1.1.1`
- `androidx.viewpager:viewpager:1.0.0`
- `androidx.window:window:1.5.0`
- `androidx.window:window-core:1.5.0`
- `androidx.window:window-core-android:1.5.0`
- `com.google.accompanist:accompanist-drawablepainter:0.37.3`
- `com.google.code.findbugs:jsr305:3.0.2`
- `com.google.dagger:dagger:2.60.1`
- `com.google.dagger:dagger-lint-aar:2.60.1`
- `com.google.dagger:hilt-android:2.60.1`
- `com.google.dagger:hilt-core:2.60.1`
- `com.google.guava:failureaccess:1.0.2`
- `com.google.guava:guava:33.3.1-android`
- `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.squareup.okhttp3:okhttp-sse:4.12.0`
- `com.squareup.okio:okio:3.16.4`
- `com.squareup.okio:okio-jvm:3.16.4`
- `io.coil-kt.coil3:coil:3.4.0`
- `io.coil-kt.coil3:coil-android:3.4.0`
- `io.coil-kt.coil3:coil-compose:3.4.0`
- `io.coil-kt.coil3:coil-compose-android:3.4.0`
- `io.coil-kt.coil3:coil-compose-core:3.4.0`
- `io.coil-kt.coil3:coil-compose-core-android:3.4.0`
- `io.coil-kt.coil3:coil-core:3.4.0`
- `io.coil-kt.coil3:coil-core-android:3.4.0`
- `io.coil-kt.coil3:coil-network-core:3.4.0`
- `io.coil-kt.coil3:coil-network-core-android:3.4.0`
- `io.coil-kt.coil3:coil-network-okhttp:3.4.0`
- `io.coil-kt.coil3:coil-network-okhttp-android:3.4.0`
- `io.ktor:ktor-client-content-negotiation:3.2.3`
- `io.ktor:ktor-client-content-negotiation-jvm:3.2.3`
- `io.ktor:ktor-client-core:3.2.3`
- `io.ktor:ktor-client-core-jvm:3.2.3`
- `io.ktor:ktor-client-okhttp:3.2.3`
- `io.ktor:ktor-client-okhttp-jvm:3.2.3`
- `io.ktor:ktor-events:3.2.3`
- `io.ktor:ktor-events-jvm:3.2.3`
- `io.ktor:ktor-http:3.2.3`
- `io.ktor:ktor-http-cio:3.2.3`
- `io.ktor:ktor-http-cio-jvm:3.2.3`
- `io.ktor:ktor-http-jvm:3.2.3`
- `io.ktor:ktor-io:3.2.3`
- `io.ktor:ktor-io-jvm:3.2.3`
- `io.ktor:ktor-network:3.2.3`
- `io.ktor:ktor-network-jvm:3.2.3`
- `io.ktor:ktor-serialization:3.2.3`
- `io.ktor:ktor-serialization-jvm:3.2.3`
- `io.ktor:ktor-serialization-kotlinx:3.2.3`
- `io.ktor:ktor-serialization-kotlinx-json:3.2.3`
- `io.ktor:ktor-serialization-kotlinx-json-jvm:3.2.3`
- `io.ktor:ktor-serialization-kotlinx-jvm:3.2.3`
- `io.ktor:ktor-sse:3.2.3`
- `io.ktor:ktor-sse-jvm:3.2.3`
- `io.ktor:ktor-utils:3.2.3`
- `io.ktor:ktor-utils-jvm:3.2.3`
- `io.ktor:ktor-websocket-serialization:3.2.3`
- `io.ktor:ktor-websocket-serialization-jvm:3.2.3`
- `io.ktor:ktor-websockets:3.2.3`
- `io.ktor:ktor-websockets-jvm:3.2.3`
- `jakarta.inject:jakarta.inject-api:2.0.1`
- `javax.inject:javax.inject:1`
- `org.jetbrains:annotations:23.0.0`
- `org.jetbrains.androidx.lifecycle:lifecycle-common:2.9.6`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime:2.9.6`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.6`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.6`
- `org.jetbrains.androidx.savedstate:savedstate:1.3.6`
- `org.jetbrains.androidx.savedstate:savedstate-compose:1.3.6`
- `org.jetbrains.compose.animation:animation:1.9.3`
- `org.jetbrains.compose.animation:animation-core:1.9.3`
- `org.jetbrains.compose.annotation-internal:annotation:1.9.3`
- `org.jetbrains.compose.collection-internal:collection:1.9.3`
- `org.jetbrains.compose.foundation:foundation:1.9.3`
- `org.jetbrains.compose.foundation:foundation-layout:1.9.3`
- `org.jetbrains.compose.runtime:runtime:1.9.3`
- `org.jetbrains.compose.runtime:runtime-saveable:1.9.3`
- `org.jetbrains.compose.ui:ui:1.9.3`
- `org.jetbrains.compose.ui:ui-geometry:1.9.3`
- `org.jetbrains.compose.ui:ui-graphics:1.9.3`
- `org.jetbrains.compose.ui:ui-text:1.9.3`
- `org.jetbrains.compose.ui:ui-unit:1.9.3`
- `org.jetbrains.compose.ui:ui-util:1.9.3`
- `org.jetbrains.kotlin:kotlin-stdlib:2.3.21`
- `org.jetbrains.kotlin:kotlin-stdlib-common:2.3.21`
- `org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.21`
- `org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.21`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0`
- `org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:0.7.0`
- `org.jetbrains.kotlinx:kotlinx-io-core:0.7.0`
- `org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.7.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json-io:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json-io-jvm:1.9.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0`
- `org.jspecify:jspecify:1.0.0`

## BSD-3-Clause

DataStore가 쓰는 protobuf 런타임을 다시 묶은 것이다. protobuf 자체의 라이선스를 따른다.

- `androidx.datastore:datastore-preferences-external-protobuf:1.2.1`

## MIT

Ktor가 로그 출력에 쓰는 API다. 이 앱은 로그 구현체를 넣지 않으므로 실제로 기록되는 것은 없다.
`slf4j-api`의 POM에는 라이선스 항목이 없고 같은 버전의 `slf4j-bom`이 MIT로 밝히고 있다.

- `org.slf4j:slf4j-api:2.0.17`

## 음원 제공자에 대한 고지

이 앱은 오픈소스 라이브러리 외에 외부 음원 제공자의 공개 웹 프로토콜을 관찰해 사용한다(ADR-029·037).
그것은 오픈소스 라이선스의 대상이 아니며 이 문서가 다루는 범위도 아니다.
