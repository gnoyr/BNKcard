package com.bnk.domain.admin.service;

import com.bnk.domain.admin.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogMapper logMapper;
    private static final int PAGE_SIZE = 20;

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminActivityLogs(
            String adminName, String roleCode, String result,
            String from, String to, int page) {

        int offset = page * PAGE_SIZE;
        List<Map<String, Object>> content =
                logMapper.findAdminActivityLogs(
                        adminName, roleCode, result, from, to, offset, PAGE_SIZE);
        long total = logMapper.countAdminActivityLogs(
                        adminName, roleCode, result, from, to);

        return buildPage(content, total, page);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditLogs(
            String action, String result, String ip,
            String from, String to, int page) {

        int offset = page * PAGE_SIZE;
        List<Map<String, Object>> content =
                logMapper.findAuditLogs(
                        action, result, ip, from, to, offset, PAGE_SIZE);
        long total = logMapper.countAuditLogs(
                        action, result, ip, from, to);

        return buildPage(content, total, page);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserActivityLogs(
            String action, String result,
            String from, String to, int page) {

        int offset = page * PAGE_SIZE;
        List<Map<String, Object>> content =
                logMapper.findUserActivityLogs(
                        action, result, from, to, offset, PAGE_SIZE);
        long total = logMapper.countUserActivityLogs(
                        action, result, from, to);

        return buildPage(content, total, page);
    }

    private Map<String, Object> buildPage(
            List<Map<String, Object>> content, long total, int page) {
        Map<String, Object> result = new HashMap<>();
        result.put("content",    content);
        result.put("totalCount", total);
        result.put("page",       page);
        result.put("size",       PAGE_SIZE);
        return result;
    }
}