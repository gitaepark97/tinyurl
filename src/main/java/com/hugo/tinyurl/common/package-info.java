// 어느 도메인에도 속하지 않는 공용 포트/구현체라 전체를 OPEN으로 선언해 모든 모듈이 자유롭게 참조할 수 있게 한다.
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.hugo.tinyurl.common;
