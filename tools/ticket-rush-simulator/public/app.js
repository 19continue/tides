const form = document.querySelector('#run-form');
const startButton = document.querySelector('#start-button');
const generateUsersButton = document.querySelector('#generate-users-button');
const generateResult = document.querySelector('#user-generate-result');
const apiPrefix = window.location.pathname.startsWith('/simulator') ? '/simulator/api' : '/api';
let eventSource;

const eventNameMap = {
  run_started: '开始',
  create_failed: '下单失败',
  attempt_error: '尝试异常',
  order_created: '下单成功',
  order_paid: '支付推进',
  run_failed: '运行失败',
};

const latencyNameMap = {
  createOrder: '抢票接口',
  asyncOrderCache: '异步订单',
  simulatePay: '模拟支付',
  endToEnd: '端到端',
};

const qualityNameMap = {
  createSuccessRateGte95: '抢票成功率 >= 95%',
  paidSuccessRateGte99: '支付推进率 >= 99%',
  endToEndP99Lt10s: '端到端 P99 < 10s',
};

init();

function apiPath(path) {
  return `${apiPrefix}${path}`;
}

async function init() {
  const defaults = await fetchJson(apiPath('/defaults'));
  const config = defaults.config || {};
  form.configPath.placeholder = defaults.configPath || '';
  form.usersPath.value = defaults.usersPath || '';
  form.targetsPath.placeholder = defaults.targetsPath || '';
  form.targetsJson.value = defaults.targetsText || '';

  form.userBaseUrl.value = config.serviceBaseUrls?.user || 'http://127.0.0.1:6082';
  form.programBaseUrl.value = config.serviceBaseUrls?.program || 'http://127.0.0.1:6086';
  form.orderBaseUrl.value = config.serviceBaseUrls?.order || 'http://127.0.0.1:8081';
  form.simulatorPayToken.value = config.simulatorPayToken || 'tides-demo-local';
  form.simulatorUserToken.value = config.simulatorUserToken || config.simulatorPayToken || 'tides-demo-local';

  form.userPrepareEndpoint.value = config.endpoints?.simulatorUserPrepare || '/user/simulator/prepare';
  form.createOrderEndpoint.value = config.endpoints?.createOrder || '/program/order/create/v4';
  form.orderCacheEndpoint.value = config.endpoints?.orderCache || '/order/get/cache';
  form.simulatePayEndpoint.value = config.endpoints?.simulatePay || '/order/simulate/pay';
  form.movieScreeningEndpoint.value = config.endpoints?.movieScreeningList || '/movie/screening/list';
  form.movieSeatEndpoint.value = config.endpoints?.movieSeatInfo || '/movie/screening/seat/info';

  const generator = config.userGenerator || {};
  form.generatorCount.value = generator.count ?? 100;
  form.generatorStartIndex.value = generator.startIndex ?? 1;
  form.generatorMobilePrefix.value = generator.mobilePrefix || '19099';
  form.generatorEmailDomain.value = generator.emailDomain || 'sandbox.local';
  form.generatorPassword.value = generator.password || '111111';
  form.generatorTicketUserCount.value = generator.ticketUserCount ?? 1;
  form.generatorRelNamePrefix.value = generator.relNamePrefix || '压测购票人';
  form.generatorOutputPath.value = generator.outputPath || 'users.generated.json';
  form.generatorOverwritePassword.checked = Boolean(generator.overwritePassword);

  form.n.value = config.rush?.users ?? 100;
  form.rushConcurrency.value = config.rush?.concurrency ?? 100;
  form.payConcurrency.value = config.payment?.concurrency ?? 50;
  form.attemptsPerTarget.value = config.rush?.attemptsPerTarget ?? 2;
  form.defaultTicketCount.value = config.defaultTicketCount ?? 1;
  form.pollIntervalMs.value = config.poll?.intervalMs ?? 200;
  form.pollTimeoutMs.value = config.poll?.timeoutMs ?? 15000;
  form.httpTimeoutMs.value = config.http?.timeoutMs ?? 15000;
  form.httpRetries.value = config.http?.retries ?? 1;
  form.paymentEnabled.checked = config.payment?.enabled !== false;
  form.reuseUsers.checked = Boolean(config.reuseUsers);
}

generateUsersButton.addEventListener('click', async () => {
  generateUsersButton.disabled = true;
  setGenerateResult('正在创建用户和购票人...', 'pending');
  try {
    const payload = {
      ...collectBasePayload(),
      userGenerator: collectUserGenerator(),
    };
    const response = await fetchJson(apiPath('/users/generate'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    form.usersPath.value = response.outputPath;
    const summary = response.summary || {};
    setGenerateResult(
      `已写入 ${response.outputPath}，新建 ${summary.createdCount ?? 0} 个，复用 ${summary.reusedCount ?? 0} 个，新增购票人 ${summary.ticketUserCreatedCount ?? 0} 个。`,
      'ok',
    );
  } catch (error) {
    setGenerateResult(error.message, 'error');
  } finally {
    generateUsersButton.disabled = false;
  }
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  startButton.disabled = true;
  setState('running', '启动中');
  const payload = {
    ...collectBasePayload(),
    usersPath: form.usersPath.value.trim(),
    targetsPath: form.targetsPath.value.trim(),
    targetsJson: form.targetsJson.value.trim(),
    n: numberValue('n'),
    rushConcurrency: numberValue('rushConcurrency'),
    payConcurrency: numberValue('payConcurrency'),
    attemptsPerTarget: numberValue('attemptsPerTarget'),
    defaultTicketCount: numberValue('defaultTicketCount'),
    pollIntervalMs: numberValue('pollIntervalMs'),
    pollTimeoutMs: numberValue('pollTimeoutMs'),
    httpTimeoutMs: numberValue('httpTimeoutMs'),
    httpRetries: numberValue('httpRetries'),
    startAt: form.startAt.value.trim(),
    paymentEnabled: form.paymentEnabled.checked,
    reuseUsers: form.reuseUsers.checked,
  };
  try {
    const response = await fetchJson(apiPath('/run'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    connectEvents(response.runId);
  } catch (error) {
    startButton.disabled = false;
    setState('failed', error.message);
  }
});

function collectBasePayload() {
  return {
    configPath: form.configPath.value.trim(),
    simulatorPayToken: form.simulatorPayToken.value.trim(),
    simulatorUserToken: form.simulatorUserToken.value.trim(),
    config: {
      serviceBaseUrls: {
        user: form.userBaseUrl.value.trim(),
        program: form.programBaseUrl.value.trim(),
        order: form.orderBaseUrl.value.trim(),
      },
      endpoints: {
        simulatorUserPrepare: form.userPrepareEndpoint.value.trim(),
        createOrder: form.createOrderEndpoint.value.trim(),
        orderCache: form.orderCacheEndpoint.value.trim(),
        simulatePay: form.simulatePayEndpoint.value.trim(),
        movieScreeningList: form.movieScreeningEndpoint.value.trim(),
        movieSeatInfo: form.movieSeatEndpoint.value.trim(),
      },
      simulatorPayToken: form.simulatorPayToken.value.trim(),
      simulatorUserToken: form.simulatorUserToken.value.trim(),
      defaultTicketCount: numberValue('defaultTicketCount'),
      rush: {
        users: numberValue('n'),
        concurrency: numberValue('rushConcurrency'),
        attemptsPerTarget: numberValue('attemptsPerTarget'),
        startAt: form.startAt.value.trim() || null,
      },
      payment: {
        enabled: form.paymentEnabled.checked,
        mode: 'simulate',
        concurrency: numberValue('payConcurrency'),
      },
      poll: {
        intervalMs: numberValue('pollIntervalMs'),
        timeoutMs: numberValue('pollTimeoutMs'),
      },
      http: {
        timeoutMs: numberValue('httpTimeoutMs'),
        retries: numberValue('httpRetries'),
      },
    },
  };
}

function collectUserGenerator() {
  return {
    count: numberValue('generatorCount'),
    startIndex: numberValue('generatorStartIndex'),
    mobilePrefix: form.generatorMobilePrefix.value.trim(),
    emailDomain: form.generatorEmailDomain.value.trim(),
    password: form.generatorPassword.value,
    ticketUserCount: numberValue('generatorTicketUserCount'),
    relNamePrefix: form.generatorRelNamePrefix.value.trim(),
    overwritePassword: form.generatorOverwritePassword.checked,
    outputPath: form.generatorOutputPath.value.trim(),
  };
}

function connectEvents(runId) {
  if (eventSource) {
    eventSource.close();
  }
  document.querySelector('#run-id').textContent = runId;
  eventSource = new EventSource(apiPath(`/runs/${runId}/events`));
  ['state', 'progress', 'event', 'finished', 'failed'].forEach((eventName) => {
    eventSource.addEventListener(eventName, (message) => {
      const data = JSON.parse(message.data);
      renderRun(data);
      if (eventName === 'finished' || eventName === 'failed') {
        startButton.disabled = false;
        eventSource.close();
      }
    });
  });
  eventSource.onerror = () => {
    setState('failed', '连接中断');
    startButton.disabled = false;
  };
}

function renderRun(run) {
  setState(run.status, stateText(run));
  if (run.summary) {
    renderSummary(run.summary);
  }
  renderEvents(run.recentEvents || []);
  if (run.error) {
    document.querySelector('#failure-list').innerHTML = `<div class="empty">${escapeHtml(run.error.message)}</div>`;
  }
}

function renderSummary(summary) {
  const total = summary.totalUsers || 0;
  const completed = summary.completedUsers || 0;
  const progress = total ? Math.min(100, (completed / total) * 100) : 0;
  document.querySelector('#progress-text').textContent = `${completed} / ${total}`;
  document.querySelector('#progress-bar').style.width = `${progress}%`;
  text('#metric-total', total);
  text('#metric-completed', completed);
  text('#metric-success-rate', `${formatNumber(summary.successRate)}%`);
  text('#metric-paid-rate', `${formatNumber(summary.paidRate)}%`);
  text('#metric-tps', formatNumber(summary.throughput?.successfulOrdersPerSecond));
  text('#metric-p99', formatMs(summary.latencyMs?.endToEnd?.p99));
  renderLatency(summary.latencyMs || {});
  renderQuality(summary.qualityGates || {});
  renderDistribution('#target-list', summary.byTarget || {}, 'target');
  renderDistribution('#failure-list', summary.failures || {}, 'failure');
}

function renderLatency(latency) {
  const entries = Object.entries(latency);
  const container = document.querySelector('#latency-list');
  if (!entries.length) {
    container.className = 'latency-list empty';
    container.textContent = '等待运行数据';
    return;
  }
  const max = Math.max(1, ...entries.map(([, value]) => Number(value.p99 || value.max || 0)));
  container.className = 'latency-list';
  container.innerHTML = entries.map(([key, value]) => {
    const width = Math.min(100, Number(value.p99 || 0) / max * 100);
    return `
      <div class="latency-row">
        <div class="latency-name">${latencyNameMap[key] || key}</div>
        <div class="latency-track"><div class="latency-fill" style="width:${width}%"></div></div>
        <div class="latency-values">
          <span>P50 ${formatMs(value.p50)}</span>
          <span>P90 ${formatMs(value.p90)}</span>
          <span>P95 ${formatMs(value.p95)}</span>
          <span>P99 ${formatMs(value.p99)}</span>
        </div>
      </div>
    `;
  }).join('');
}

function renderQuality(quality) {
  const entries = Object.entries(quality);
  const container = document.querySelector('#quality-list');
  if (!entries.length) {
    container.className = 'quality-list empty';
    container.textContent = '等待运行数据';
    return;
  }
  container.className = 'quality-list';
  container.innerHTML = entries.map(([key, ok]) => `
    <div class="quality-item">
      <span>${qualityNameMap[key] || key}</span>
      <span class="badge ${ok ? 'ok' : 'warn'}">${ok ? '通过' : '观察'}</span>
    </div>
  `).join('');
}

function renderDistribution(selector, data, type) {
  const container = document.querySelector(selector);
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  if (!entries.length) {
    container.className = 'bar-list empty';
    container.textContent = '等待运行数据';
    return;
  }
  const max = Math.max(...entries.map(([, value]) => Number(value || 0)), 1);
  container.className = 'bar-list';
  container.innerHTML = entries.map(([name, value]) => {
    const color = type === 'failure' ? '#b42318' : '#12805c';
    return `
      <div class="bar-row">
        <div class="bar-label"><span>${escapeHtml(name)}</span></div>
        <div class="bar-track"><div class="bar-fill" style="width:${Number(value) / max * 100}%;background:${color}"></div></div>
        <div class="bar-value">${value}</div>
      </div>
    `;
  }).join('');
}

function renderEvents(events) {
  const body = document.querySelector('#event-body');
  if (!events.length) {
    body.innerHTML = '<tr><td colspan="6">等待运行数据</td></tr>';
    return;
  }
  body.innerHTML = events.slice(0, 80).map((event) => `
    <tr>
      <td>${formatTime(event.time)}</td>
      <td>${eventNameMap[event.type] || event.type}</td>
      <td>${escapeHtml(event.userId ?? '-')}</td>
      <td>${escapeHtml(event.target ?? '-')}</td>
      <td>${escapeHtml(event.orderNumber ?? '-')}</td>
      <td>${formatMs(event.latencyMs ?? event.asyncOrderLatencyMs)}</td>
    </tr>
  `).join('');
}

function stateText(run) {
  if (run.status === 'starting') return '准备中';
  if (run.status === 'running') return '运行中';
  if (run.status === 'finished') return '已完成';
  if (run.status === 'failed') return run.error?.message || '失败';
  return '未开始';
}

function setState(state, label) {
  const dot = document.querySelector('#state-dot');
  dot.className = `state-dot ${state || 'idle'}`;
  document.querySelector('#state-text').textContent = label || '未开始';
}

function setGenerateResult(message, state) {
  generateResult.textContent = message;
  generateResult.className = `inline-status ${state || ''}`;
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || `HTTP ${response.status}`);
  }
  return data;
}

function numberValue(name) {
  const raw = form[name].value;
  if (raw === '') return undefined;
  return Number(raw);
}

function text(selector, value) {
  document.querySelector(selector).textContent = value ?? '-';
}

function formatNumber(value) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '-';
  return Number(value).toFixed(Number(value) >= 100 ? 0 : 2).replace(/\.00$/, '');
}

function formatMs(value) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '-';
  const number = Number(value);
  return `${number >= 1000 ? (number / 1000).toFixed(2) : number.toFixed(0)}${number >= 1000 ? 's' : 'ms'}`;
}

function formatTime(value) {
  if (!value) return '-';
  return new Date(value).toLocaleTimeString('zh-CN', { hour12: false });
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
