import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, jsonHeaders } from './lib/config.js';

export const options = {
    stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 300 },
        { duration: '1m', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
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
