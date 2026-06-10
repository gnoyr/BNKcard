package com.bnk.global.log.annotation;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {
    String eventType();
    String targetType();
    String actionDetail() default "";
    String cardIdParam() default "cardId";  // ← 추가: cardId 파라미터 이름 명시
}