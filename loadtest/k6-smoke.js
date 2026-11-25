import http from 'k6/http';
import { check, sleep } from 'k6';

// CI 冒烟：1~2 VU、30 秒，只验证"核心链路没坏 + 性能没明显回退"。
// 阈值一旦触发即刻中止（abortOnFail），让流水线快速失败而不是等跑完。
//
//   k6 run loadtest/k6-smoke.js
//   k6 run -e BASE_URL=http://localhost:8081 loadtest/k6-smoke.js

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
  vus: 2,
  duration: '30s',
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '10s' }],
    'http_req_duration{scenario:default}': [
      { threshold: 'p(95)<800', abortOnFail: true, delayAbortEval: '10s' },
    ],
  },
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const res = http.post(`${BASE_URL}/login`, JSON.stringify({ username: 'admin', password: '123456' }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  check(res, { 'login 成功': (r) => r.status === 200 && r.json('code') === 1 });
  return { token: res.json('data.token') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  ['/depts', '/students?pageNum=1&pageSize=10', '/report/clazzStudentCount'].forEach((path) => {
    const res = http.get(`${BASE_URL}${path}`, { headers, tags: { name: path.split('?')[0] } });
    check(res, { [`${path} 200`]: (r) => r.status === 200 });
  });

  // 每个迭代打一次 AI，覆盖拦截器 → Redis 记忆 → RAG 检索 → SSE 全链路
  const chat = http.post(
    `${BASE_URL}/ai/chat`,
    JSON.stringify({ conversationId: 'k6-smoke', message: '系统初始有哪些部门？' }),
    { headers: { 'Content-Type': 'application/json', ...headers }, timeout: '30s', tags: { name: 'ai_chat' } },
  );
  check(chat, { 'ai chat 200': (r) => r.status === 200 && r.body && r.body.length > 0 });

  sleep(2);
}
