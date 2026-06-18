package com.bnk.domain.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper {

    /** 관리자 활동 로그 목록 */
    List<Map<String, Object>> findAdminActivityLogs(
            @Param("adminName") String adminName,
            @Param("roleCode")  String roleCode,
            @Param("result")    String result,
            @Param("from")      String from,
            @Param("to")        String to,
            @Param("offset")    int offset,
            @Param("size")      int size);

    long countAdminActivityLogs(
            @Param("adminName") String adminName,
            @Param("roleCode")  String roleCode,
            @Param("result")    String result,
            @Param("from")      String from,
            @Param("to")        String to);

    /** 감사 로그 목록 */
    List<Map<String, Object>> findAuditLogs(
            @Param("action") String action,
            @Param("result") String result,
            @Param("ip")     String ip,
            @Param("from")   String from,
            @Param("to")     String to,
            @Param("offset") int offset,
            @Param("size")   int size);

    long countAuditLogs(
            @Param("action") String action,
            @Param("result") String result,
            @Param("ip")     String ip,
            @Param("from")   String from,
            @Param("to")     String to);

    /** 회원 활동 로그 목록 */
    List<Map<String, Object>> findUserActivityLogs(
            @Param("action") String action,
            @Param("result") String result,
            @Param("from")   String from,
            @Param("to")     String to,
            @Param("offset") int offset,
            @Param("size")   int size);

    long countUserActivityLogs(
            @Param("action") String action,
            @Param("result") String result,
            @Param("from")   String from,
            @Param("to")     String to);
}