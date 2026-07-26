export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export function jsonHeaders() {
    return { headers: { 'Content-Type': 'application/json' } };
}
