import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// AI 链路压测（三段式），对应 README 第 9.3 节：
//
//   1) mock  —— 打 loadtest profile 的假模型实例（8081），可以高并发，不花钱、不受供应商限流
//               用途：压自己代码（拦截器、Redis 记忆、RAG 检索、SSE 转发、线程池）
//   2) real  —— 打正常实例（8080），1~3 VU，测端到端 TTFT / p95 和真实成本
//   3) fault —— 故障注入（429 / 超时 / 断流），验证兜底、重试与错误指标是否生效
//
// 用法：
//   k6 run -e MODE=mock  loadtest/k6-ai.js
//   k6 run -e MODE=real -e BASE_URL=http://localhost:8080 loadtest/k6-ai.js
//   k6 run -e MODE=fault loadtest/k6-ai.js
//
// TTFT 要以服务端指标为准（/actuator/prometheus 里的 tlias_ai_chat_ttft_seconds），
// 客户端这里用 res.timings.waiting（首字节）近似。

const MODE = __ENV.MODE || 'mock';
const BASE_URL = __ENV.TLIAS_BASE_URL || __ENV.BASE_URL || (MODE === 'real' ? 'http://localhost:8080' : 'http://localhost:8081');
// k6 默认把系统环境变量并入 __ENV，而 Windows 自带 USERNAME（登录名），
// 直接用 __ENV.USERNAME 会取到系统用户名，所以统一加 TLIAS_ 前缀。
const USERNAME = __ENV.TLIAS_USER || 'admin';
const PASSWORD = __ENV.TLIAS_PASSWORD || '123456';
// 故障模式：429（限流）/ timeout（超时）/ abort（断流）
const FAULT = __ENV.FAULT || '429';

// mock 模式并发可放大；real 模式必须小，否则烧钱 + 触发供应商限流
const VUS = Number(__ENV.VUS || (MODE === 'mock' ? 20 : MODE === 'real' ? 2 : 1));
const DURATION = __ENV.DURATION || (MODE === 'mock' ? '3m' : '1m');

const ttft = new Trend('ai_ttft_ms', true);
const total = new Trend('ai_total_ms', true);
const streamAborted = new Counter('ai_stream_aborted');

export const options = {
  scenarios: {
    ai: {
      executor: 'constant-vus',
      exec: 'chat',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: buildThresholds(),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function buildThresholds() {
  if (MODE === 'fault') {
    // 故障注入时错误是预期行为，不设错误率阈值，只看是否能及时返回
    return { 'http_req_duration{scenario:ai}': ['p(95)<5000'] };
  }
  if (MODE === 'real') {
    // 真实模型：首字 5s、整段 60s 以内
    return {
      ai_ttft_ms: ['p(95)<5000'],
      ai_total_ms: ['p(95)<60000'],
      'http_req_duration{scenario:ai}': ['p(95)<60000'],
    };
  }
  // mock：应用层开销，首字 500ms、整段 3s 以内
  return {
    ai_ttft_ms: ['p(95)<500'],
    ai_total_ms: ['p(95)<3000'],
    'http_req_duration{scenario:ai}': ['p(95)<3000'],
    http_req_failed: ['rate<0.01'],
  };
}

export function setup() {
  const res = http.post(`${BASE_URL}/login`, JSON.stringify({ username: USERNAME, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  if (!check(res, { 'login 成功': (r) => r.status === 200 && r.json('code') === 1 })) {
    fail(`登录失败，BASE_URL=${BASE_URL}，请确认压测实例已启动`);
  }
  const token = res.json('data.token');

  if (MODE === 'fault') {
    // 通过压测专用开关注入故障，避免依赖消息内容（RAG 查询改写会把标记吃掉）
    const setFault = http.post(`${BASE_URL}/loadtest/fault?mode=${FAULT}`, null, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { name: 'set_fault' },
    });
    check(setFault, { '故障注入开关生效': (r) => r.status === 200 });
  }

  return { token };
}

export function teardown(data) {
  if (MODE === 'fault') {
    http.post(`${BASE_URL}/loadtest/fault/clear`, null, {
      headers: { Authorization: `Bearer ${data.token}` },
      tags: { name: 'clear_fault' },
    });
  }
}

export function chat(data) {
  const conversationId = `k6-${MODE}-vu-${__VU}`;
  let message = '系统初始有哪些部门？';

  if (MODE === 'fault') {
    // 故障由 /loadtest/fault 开关注入，这里用普通问题即可
    message = `故障注入验证 ${__ITER}`;
  } else if (__ITER % 3 === 0) {
    // 多轮追问：验证记忆与查询改写链路
    message = '我刚才问了你什么？请结合上下文回答';
  }

  const res = http.post(
    `${BASE_URL}/ai/chat`,
    JSON.stringify({ conversationId, message }),
    {
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` },
      timeout: MODE === 'real' ? '120s' : '30s',
      tags: { name: 'ai_chat' },
    },
  );

  if (MODE === 'fault') {
    // 期望：要么错误码（4xx/5xx），要么返回兜底文案；二者都说明故障处理生效
    check(res, {
      '故障请求有明确响应': (r) => r.status >= 400 || (r.body && r.body.includes('暂时不可用')),
    });
    total.add(res.timings.duration);
    streamAborted.add(res.status >= 500);
    sleep(0.5);
    return;
  }

  check(res, {
    'ai chat 状态 200': (r) => r.status === 200,
    'ai chat 有响应内容': (r) => r.body && r.body.length > 0,
  });

  ttft.add(res.timings.waiting);
  total.add(res.timings.duration);

  // mock 模式放开跑；真实模型留间隔，避免触发供应商限流
  sleep(MODE === 'real' ? 6 : Math.random() * 0.5);
}
