import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, jsonHeaders } from './lib/config.js';

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '3m', target: 20 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<300', 'p(99)<500'],
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
