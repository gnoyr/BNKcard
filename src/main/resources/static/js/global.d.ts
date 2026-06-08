// global.d.ts
interface BnkErrorUtil {
    extract(data: unknown, fallback?: string): string;
    handle(res: { ok: boolean; status: number; data?: unknown }, errEl?: HTMLElement | null, custom?: Record<number, string>): boolean;
}

interface BnkAPIUtil {
    get(url: string, timeout?: number): Promise<{ ok: boolean; status: number; data: unknown }>;
    post(url: string, body?: unknown, timeout?: number): Promise<{ ok: boolean; status: number; data: unknown }>;
    put(url: string, body?: unknown, timeout?: number): Promise<{ ok: boolean; status: number; data: unknown }>;
    patch(url: string, body?: unknown, timeout?: number): Promise<{ ok: boolean; status: number; data: unknown }>;
    del(url: string, timeout?: number): Promise<{ ok: boolean; status: number; data: unknown }>;
}

interface BnkToastUtil {
    success(msg: string, ms?: number): void;
    error(msg: string, ms?: number): void;
    warning(msg: string, ms?: number): void;
    info(msg: string, ms?: number): void;
}

interface BnkDOMUtil {
    $id(id: string): HTMLElement | null;
    showError(el: HTMLElement | null, msg: string): void;
    hideError(el: HTMLElement | null): void;
    btnLoading(btn: HTMLElement | null, loading: boolean, loadingText?: string): void;
    on(id: string, event: string, handler: EventListener): void;
}

interface Window {
    BNK: {
        API:   BnkAPIUtil;
        Error: BnkErrorUtil;
        Toast: BnkToastUtil;
        DOM:   BnkDOMUtil;
    };
    BnkAPI:   BnkAPIUtil;
    BnkError: BnkErrorUtil;
    BnkToast: BnkToastUtil;
    BnkDOM:   BnkDOMUtil;
    showToast(msg: string, type?: 'success' | 'error' | 'warning' | 'info'): void;
}