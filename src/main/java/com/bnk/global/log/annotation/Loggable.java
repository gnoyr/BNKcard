package com.bnk.global.log.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {
    String eventType();               // "CARD_VIEW", "TERMS_AGREE", "CHAT_QUERY"
    String targetType();              // "CARD", "TERMS", "CHAT"
    String actionDetail() default ""; // 고정 텍스트 (단순 기능용)
}
