package com.bnk.global.migration;

import com.bnk.domain.user.mapper.UserMapper;
import com.bnk.global.util.CiValueGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 기존 회원 CI 일괄 재생성.
 *
 * CI 공식이 v3(이름+주민번호앞6+성별+주소) → v4(이름+생년월일YYMMDD+전화번호) 로 변경됨에 따라,
 * 저장돼 있는 birth_date / phone 만으로 모든 회원의 ci_value 를 새 공식으로 다시 계산한다.
 * (성별코드/주소는 USERS 에 저장돼 있지 않으므로 v3 재현은 불가 → v4 로 전환)
 *
 * [멱등성] 같은 입력이면 항상 같은 CI → 재실행 안전.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CiRecomputeMigrationService {

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private final CiRecomputeMigrationMapper ciMapper;
    private final UserMapper                 userMapper;
    private final CiValueGenerator           ciValueGenerator;

    public record MigrationResult(int successCount, int failCount, int skipCount) {}

    @Transactional
    public MigrationResult migrateAll() {
        List<CiMigrationRow> rows = ciMapper.selectAllForCi();
        int ok = 0, fail = 0, skip = 0;

        for (CiMigrationRow r : rows) {
            // birth_date 또는 phone 이 없으면 CI 재계산 불가 → skip
            if (r.getName() == null || r.getBirthDate() == null
                    || r.getPhone() == null || r.getPhone().isBlank()) {
                skip++;
                continue;
            }
            try {
                String yymmdd = r.getBirthDate().format(YYMMDD);
                String ci     = ciValueGenerator.generate(r.getName(), yymmdd, r.getPhone());
                userMapper.updateCiValue(r.getUserId(), ci);
                ok++;
            } catch (Exception e) {
                log.warn("[CiMigration] userId={} CI 재계산 실패: {}", r.getUserId(), e.getMessage());
                fail++;
            }
        }
        return new MigrationResult(ok, fail, skip);
    }
}
