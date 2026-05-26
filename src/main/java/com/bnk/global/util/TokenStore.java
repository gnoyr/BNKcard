package com.bnk.global.util;

public interface TokenStore {
    void   set(String key, String value, long ttlMinutes);
    String get(String key);
    void   delete(String key);
}