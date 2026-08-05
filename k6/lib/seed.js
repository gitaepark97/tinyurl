import http from 'k6/http';
import { BASE_URL, jsonHeaders } from './config.js';

export function seedShortKeys(count) {
    const keys = [];
    for (let i = 0; i < count; i++) {
        const res = http.post(
            `${BASE_URL}/api/v1/urls`,
            JSON.stringify({ originalUrl: `https://example.com/seed-${i}` }),
            jsonHeaders()
        );
        keys.push(res.json('data.shortKey'));
    }
    return keys;
}

export function seedShortUrl(originalUrl) {
    const res = http.post(`${BASE_URL}/api/v1/urls`, JSON.stringify({ originalUrl }), jsonHeaders());
    return { id: res.json('data.id'), shortKey: res.json('data.shortKey') };
}

export function seedClickEvents(shortKey, count) {
    for (let i = 0; i < count; i++) {
        http.get(`${BASE_URL}/${shortKey}`, { redirects: 0 });
    }
}
