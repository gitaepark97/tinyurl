// BusinessException/ErrorCode/Page/PageParam/ApiResponse 등 전 모듈이 공유하는 순수 공용
// 타입이라 common과 동일하게 OPEN으로 선언한다.
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.hugo.tinyurl.support;
