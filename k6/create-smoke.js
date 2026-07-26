import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, jsonHeaders } from './lib/config.js';

export const options = {
    vus: 2,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate==0'],
        http_req_duration: ['p(95)<200'],
    },
};

export default function () {
    const res = http.post(
        `${BASE_URL}/api/v1/urls`,
        JSON.stringify({ originalUrl: `https://example.com/${__VU}-${__ITER}` }),
        jsonHeaders()
    );

    check(res, { 'status is 201': (r) => r.status === 201 });
}
