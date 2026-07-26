import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './lib/config.js';
import { seedShortKeys } from './lib/seed.js';

const SEED_COUNT = 200;

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

export function setup() {
    return { keys: seedShortKeys(SEED_COUNT) };
}

export default function (data) {
    const key = data.keys[Math.floor(Math.random() * data.keys.length)];
    const res = http.get(`${BASE_URL}/${key}`, { redirects: 0 });

    check(res, { 'status is 302': (r) => r.status === 302 });
}
