import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './lib/config.js';
import { seedShortUrl, seedClickEvents } from './lib/seed.js';

const CLICK_EVENT_COUNT = 3000;
const PAGE_SIZE = 20;

export const options = {
    vus: 2,
    duration: '30s',
    setupTimeout: '3m',
    thresholds: {
        http_req_failed: ['rate==0'],
        http_req_duration: ['p(95)<200'],
    },
};

export function setup() {
    const shortUrl = seedShortUrl('https://example.com/click-events-seed');
    seedClickEvents(shortUrl.shortKey, CLICK_EVENT_COUNT);
    return { shortUrlId: shortUrl.id };
}

export default function (data) {
    let cursor = null;
    let hasNext = true;

    while (hasNext) {
        const query = cursor ? `cursor=${cursor}&size=${PAGE_SIZE}` : `size=${PAGE_SIZE}`;
        const res = http.get(`${BASE_URL}/api/v1/urls/${data.shortUrlId}/click-events?${query}`);

        check(res, { 'status is 200': (r) => r.status === 200 });

        const content = res.json('data.content');
        hasNext = res.json('data.hasNext');
        cursor = content.length > 0 ? content[content.length - 1].id : null;
    }
}
