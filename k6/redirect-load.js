import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './lib/config.js';
import { seedShortKeys } from './lib/seed.js';

const SEED_COUNT = 200;

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

export function setup() {
    return { keys: seedShortKeys(SEED_COUNT) };
}

export default function (data) {
    const key = data.keys[Math.floor(Math.random() * data.keys.length)];
    const res = http.get(`${BASE_URL}/${key}`, { redirects: 0 });

    check(res, { 'status is 302': (r) => r.status === 302 });
}
