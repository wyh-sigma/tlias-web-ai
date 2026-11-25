import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// 业务接口压测：阶梯加压找拐点，而不是一次 10 VU 跑 40 秒就下结论。
//
// 用法（默认打 8081 的压测实例，避免影响正常实例；也可以指向 8080）：
//   k6 run loadtest/k6.js
//   k6 run -e BASE_URL=http://localhost:8080 loadtest/k6.js
//
// 判据看 p95/p99 与错误率随负载变化的拐点，服务端分位数见 Grafana / Prometheus。

const BASE_URL = __ENV.TLIAS_BASE_URL || __ENV.BASE_URL || 'http://localhost:8081';
// k6 默认把系统环境变量并入 __ENV，而 Windows 自带 USERNAME（登录名），
// 直接用 __ENV.USERNAME 会取到系统用户名，所以统一加 TLIAS_ 前缀。
const USERNAME = __ENV.TLIAS_USER || 'admin';
const PASSWORD = __ENV.TLIAS_PASSWORD || '123456';
const STAGE = __ENV.STAGE || 'standard'; // standard | spike | soak

const slowApi = new Trend('tlias_slow_api_ms', true);
const businessError = new Rate('tlias_business_error_rate');

const stagesByMode = {
  // 标准阶梯：50 → 100 → 200，找吞吐拐点
  standard: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '2m', target: 100 },
    { duration: '1m', target: 200 },
    { duration: '2m', target: 200 },
    { duration: '30s', target: 0 },
  ],
  // 尖刺：验证突发流量下的降级与恢复
  spike: [
    { duration: '30s', target: 20 },
    { duration: '10s', target: 300 },
    { duration: '1m', target: 300 },
    { duration: '20s', target: 20 },
  ],
  // 浸泡：验证内存/连接泄漏，默认 10 分钟（CI 里不要跑）
  soak: [
    { duration: '1m', target: 50 },
    { duration: '10m', target: 50 },
    { duration: '1m', target: 0 },
  ],
};

export const options = {
  scenarios: {
    browse: {
      executor: 'ramping-vus',
      exec: 'browse',
      startVUs: 0,
      stages: stagesByMode[STAGE] || stagesByMode.standard,
      gracefulRampDown: '20s',
    },
  },
  thresholds: {
    // 失败率：只统计预期内的 HTTP 失败
    http_req_failed: ['rate<0.01'],
    // 业务接口：p95 < 300ms、p99 < 800ms（按你机器实测基线调整）
    'http_req_duration{scenario:browse}': ['p(95)<300', 'p(99)<800'],
    tlias_business_error_rate: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

/** 只登录一次，所有 VU 共用访问令牌 */
export function setup() {
  const res = http.post(`${BASE_URL}/login`, JSON.stringify({ username: USERNAME, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  check(res, {
    'login 状态 200': (r) => r.status === 200,
    'login code=1': (r) => r.json('code') === 1,
  });
  return { token: res.json('data.token') };
}

const BROWSE_ENDPOINTS = [
  '/depts',
  '/clazzs?pageNum=1&pageSize=10',
  '/students?pageNum=1&pageSize=10',
  '/report/empJobData',
  '/report/studentEducationData',
  '/report/clazzStudentCount',
  '/report/clazzFinishWarning',
  '/report/violationRanking',
  '/report/highRiskStudents',
  '/report/masterStudentCount',
];

export function browse(data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  BROWSE_ENDPOINTS.forEach((path) => {
    const res = http.get(`${BASE_URL}${path}`, {
      headers,
      tags: { name: path.split('?')[0] },
    });
    const ok = check(res, {
      [`${path} 状态 200`]: (r) => r.status === 200,
      [`${path} code=1`]: (r) => r.json('code') === 1,
    });
    businessError.add(!ok);
    slowApi.add(res.timings.duration, { api: path.split('?')[0] });
  });

  // 模拟真人思考间隔，避免把 VU 变成纯机器空转
  sleep(Math.random() * 1.5 + 0.5);
}
