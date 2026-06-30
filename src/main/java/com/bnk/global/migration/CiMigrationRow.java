package com.bnk.global.migration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * CI 재생성 마이그레이션용 행 DTO.
 * birth_date/phone 는 매퍼 XML 에서 AES 복호화(typeHandler)된 평문으로 매핑된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CiMigrationRow {
    private Long      userId;
    private String    name;
    private LocalDate birthDate;
    private String    phone;
}
