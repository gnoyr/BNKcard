'use strict';
/* ================================================================
   device-id.js  |  브라우저 기기 식별자 (신뢰 기기 판정용)
   전역 BnkDevice 제공. 로그인/기기 인증 요청에 device 컨텍스트를 실어 보낸다.
   - id       : localStorage 에 영구 저장하는 UUID (서버는 SHA-256 해시로만 저장)
   - name     : 브라우저·OS 기반 표시용 이름 (예: "Chrome · Windows")
   - platform : 'WEB'
   ================================================================ */
window.BnkDevice = (function () {
    const KEY = 'bnk_device_id';

    function uuidV4() {
        if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
        // 폴백: crypto.getRandomValues 기반 v4
        const b = new Uint8Array(16);
        (window.crypto || {}).getRandomValues
            ? crypto.getRandomValues(b)
            : b.forEach((_, i) => (b[i] = Math.floor(Math.random() * 256)));
        b[6] = (b[6] & 0x0f) | 0x40;
        b[8] = (b[8] & 0x3f) | 0x80;
        const h = [...b].map(x => x.toString(16).padStart(2, '0'));
        return `${h[0]}${h[1]}${h[2]}${h[3]}-${h[4]}${h[5]}-${h[6]}${h[7]}-${h[8]}${h[9]}-${h[10]}${h[11]}${h[12]}${h[13]}${h[14]}${h[15]}`;
    }

    function getId() {
        let id = null;
        try { id = localStorage.getItem(KEY); } catch (e) { /* private mode */ }
        if (!id) {
            id = uuidV4();
            try { localStorage.setItem(KEY, id); } catch (e) { /* ignore */ }
        }
        return id;
    }

    function browserName(ua) {
        if (/Edg\//.test(ua))    return 'Edge';
        if (/OPR\//.test(ua))    return 'Opera';
        if (/Chrome\//.test(ua)) return 'Chrome';
        if (/Firefox\//.test(ua))return 'Firefox';
        if (/Safari\//.test(ua)) return 'Safari';
        return '브라우저';
    }

    function osName(ua) {
        if (/Windows/.test(ua))            return 'Windows';
        if (/Mac OS X|Macintosh/.test(ua)) return 'macOS';
        if (/Android/.test(ua))            return 'Android';
        if (/iPhone|iPad|iPod/.test(ua))   return 'iOS';
        if (/Linux/.test(ua))              return 'Linux';
        return '';
    }

    function deviceName() {
        const ua = navigator.userAgent || '';
        const os = osName(ua);
        return os ? `${browserName(ua)} · ${os}` : browserName(ua);
    }

    /** { id, name, platform } */
    function context() {
        return { id: getId(), name: deviceName(), platform: 'WEB' };
    }

    return { context, getId, deviceName };
})();
