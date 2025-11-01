#!/usr/bin/env node

import { createWriteStream } from 'node:fs';
import fs from 'node:fs/promises';
import { createServer } from 'node:http';
import path from 'node:path';
import { performance } from 'node:perf_hooks';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const TOOL_ROOT = path.resolve(__dirname, '..');

const DEFAULT_CONFIG = {
  serviceBaseUrls: {
    user: 'http://127.0.0.1:6082',
    program: 'http://127.0.0.1:6086',
    order: 'http://127.0.0.1:8081',
  },
  endpoints: {
    login: '/user/login',
    ticketUserList: '/ticket/user/list',
    ticketUserAdd: '/ticket/user/add',
    programDetail: '/program/detail/v2',
    programSeatInfo: '/seat/relate/info',
    movieScreeningList: '/movie/screening/list',
    movieSeatInfo: '/movie/screening/seat/info',
    createOrder: '/program/order/create/v4',
    orderCache: '/order/get/cache',
    simulatePay: '/order/simulate/pay',
    simulatorUserPrepare: '/user/simulator/prepare',
  },
  simulatorPayToken: 'tides-demo-local',
  simulatorUserToken: 'tides-demo-local',
  orderVersion: 4,
  defaultTicketCount: 1,
  reuseUsers: false,
  userGenerator: {
    count: 100,
    startIndex: 1,
    mobilePrefix: '19099',
    emailDomain: 'sandbox.local',
    password: '111111',
    ticketUserCount: 1,
    relNamePrefix: '压测购票人',
    overwritePassword: false,
    outputPath: 'users.generated.json',
  },
  prepare: {
    concurrency: 50,
    autoCreateTicketUser: false,
  },
  rush: {
    users: 100,
    concurrency: 100,
    attemptsPerTarget: 2,
    startAt: null,
  },
  payment: {
    enabled: true,
    mode: 'simulate',
    concurrency: 50,
  },
  poll: {
    intervalMs: 200,
    timeoutMs: 15000,
  },
  http: {
    timeoutMs: 15000,
    retries: 1,
  },
  reportDir: 'reports',
};

class ApiError extends Error {
  constructor(message, detail = {}) {
    super(message);
    this.name = 'ApiError';
    this.detail = detail;
  }
}

class Semaphore {
  constructor(limit) {
    this.limit = Math.max(1, Number(limit) || 1);
    this.active = 0;
    this.queue = [];
  }

  async run(fn) {
    await this.acquire();
    try {
      return await fn();
    } finally {
      this.release();
    }
  }

  acquire() {
    if (this.active < this.limit) {
      this.active += 1;
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      this.queue.push(resolve);
    }).then(() => {
      this.active += 1;
    });
  }

  release() {
    this.active -= 1;
    const next = this.queue.shift();
    if (next) {
      next();
    }
  }
}

class Reporter {
  constructor(reportDir, runId, onEvent = null) {
    this.reportDir = reportDir;
    this.runId = runId;
    this.onEvent = onEvent;
    this.stream = null;
    this.eventsFile = path.join(reportDir, `${runId}.events.jsonl`);
    this.summaryFile = path.join(reportDir, `${runId}.summary.json`);
  }

  async open() {
    await fs.mkdir(this.reportDir, { recursive: true });
    this.stream = createWriteStream(this.eventsFile, { flags: 'a', encoding: 'utf8' });
  }

  write(type, data = {}) {
    const event = { time: new Date().toISOString(), type, ...data };
    if (this.stream) {
      this.stream.write(`${JSON.stringify(event)}\n`);
    }
    if (this.onEvent) {
      this.onEvent(event);
    }
  }

  async close(summary) {
    await new Promise((resolve, reject) => {
      this.stream.end((error) => (error ? reject(error) : resolve()));
    });
    await fs.writeFile(this.summaryFile, `${JSON.stringify(summary, null, 2)}\n`, 'utf8');
  }
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 2; i < argv.length; i += 1) {
    const current = argv[i];
    if (!current.startsWith('--')) {
      args._.push(current);
      continue;
    }
    const eqIndex = current.indexOf('=');
    if (eqIndex > -1) {
      args[current.slice(2, eqIndex)] = current.slice(eqIndex + 1);
      continue;
    }
    const key = current.slice(2);
    const next = argv[i + 1];
    if (next && !next.startsWith('--')) {
      args[key] = next;
      i += 1;
    } else {
      args[key] = true;
    }
  }
  return args;
}

function deepMerge(base, override) {
  if (!override || typeof override !== 'object' || Array.isArray(override)) {
    return base;
  }
  const result = { ...base };
  for (const [key, value] of Object.entries(override)) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      result[key] = deepMerge(result[key] || {}, value);
    } else {
      result[key] = value;
    }
  }
  return result;
}

async function loadJson(file) {
  const text = await fs.readFile(path.resolve(file), 'utf8');
  return JSON.parse(text);
}

async function loadConfig(args) {
  const fileConfig = args.config ? await loadJson(args.config) : {};
  const config = deepMerge(DEFAULT_CONFIG, fileConfig);
  if (args.n) config.rush.users = Number(args.n);
  if (args.concurrency) config.rush.concurrency = Number(args.concurrency);
  if (args['pay-concurrency']) config.payment.concurrency = Number(args['pay-concurrency']);
  if (args['start-at']) config.rush.startAt = args['start-at'];
  if (args['no-pay']) config.payment.enabled = false;
  if (args['reuse-users']) config.reuseUsers = true;
  return config;
}

function isMeaningful(value) {
  if (value === null || value === undefined) return false;
  const text = String(value).trim();
  return text !== '' && !text.startsWith('请替换');
}

function asId(value, fieldName) {
  if (!isMeaningful(value)) return undefined;
  if (typeof value === 'number' && !Number.isSafeInteger(value)) {
    console.warn(`[WARN] ${fieldName} 超过 JavaScript 安全整数范围，请在配置中用字符串写这个 ID。`);
  }
  return String(value);
}

function asIdList(value, fieldName) {
  if (!Array.isArray(value)) return [];
  return value.map((item) => asId(item, fieldName)).filter(Boolean);
}

function normalizeUser(raw) {
  return {
    ...raw,
    userId: asId(raw.userId, 'userId'),
    ticketUserIds: asIdList(raw.ticketUserIds || raw.ticketUserIdList || [], 'ticketUserIds'),
  };
}

function normalizeTarget(raw, index) {
  const type = raw.type || (raw.screeningId || raw.screeningIds ? 'movie' : 'program');
  return {
    ...raw,
    name: raw.name || `${type}-${index + 1}`,
    type,
    programId: asId(raw.programId, 'programId'),
    screeningId: asId(raw.screeningId, 'screeningId'),
    screeningIds: asIdList(raw.screeningIds || [], 'screeningIds'),
    cinemaId: asId(raw.cinemaId, 'cinemaId'),
    areaId: asId(raw.areaId, 'areaId'),
    ticketCategoryId: asId(raw.ticketCategoryId, 'ticketCategoryId'),
    ticketCount: Number(raw.ticketCount || DEFAULT_CONFIG.defaultTicketCount),
    seatMode: raw.seatMode || (type === 'movie' ? 'manual' : 'auto'),
  };
}

async function loadUsers(file) {
  const resolved = path.resolve(file);
  if (resolved.endsWith('.csv')) {
    const text = await fs.readFile(resolved, 'utf8');
    const [headerLine, ...lines] = text.split(/\r?\n/).filter(Boolean);
    const headers = headerLine.split(',').map((item) => item.trim());
    return lines.map((line) => {
      const values = line.split(',').map((item) => item.trim());
      const row = {};
      headers.forEach((header, index) => {
        row[header] = values[index];
      });
      if (row.ticketUserIds) {
        row.ticketUserIds = row.ticketUserIds.split('|').map((item) => item.trim()).filter(Boolean);
      }
      return normalizeUser(row);
    });
  }
  const users = await loadJson(resolved);
  return users.map(normalizeUser);
}

function normalizeTargets(targets) {
  if (!Array.isArray(targets)) {
    throw new Error('目标配置必须是数组 JSON。');
  }
  return targets.map(normalizeTarget).filter((target) => {
    if (!target.programId) {
      console.warn(`[WARN] 跳过目标 ${target.name}，缺少 programId。`);
      return false;
    }
    return true;
  });
}

async function loadTargets(file) {
  const targets = await loadJson(file);
  return normalizeTargets(targets);
}

function parseTargetsText(targetsText) {
  const targets = JSON.parse(targetsText);
  return normalizeTargets(targets);
}

function joinUrl(baseUrl, endpoint) {
  return `${baseUrl.replace(/\/+$/, '')}/${endpoint.replace(/^\/+/, '')}`;
}

function normalizeBaseUrls(baseUrlConfig) {
  if (Array.isArray(baseUrlConfig)) {
    return baseUrlConfig.map((item) => String(item || '').trim()).filter(Boolean);
  }
  const text = String(baseUrlConfig || '').trim();
  return text ? text.split(',').map((item) => item.trim()).filter(Boolean) : [];
}

function createApi(config) {
  const counters = new Map();
  return {
    async post(service, endpoint, body, extraHeaders = {}) {
      const baseUrls = normalizeBaseUrls(config.serviceBaseUrls[service]);
      if (baseUrls.length === 0) throw new ApiError(`未配置 ${service} 服务地址`);
      const current = counters.get(service) || 0;
      const baseUrl = baseUrls[current % baseUrls.length];
      counters.set(service, current + 1);
      const url = joinUrl(baseUrl, endpoint);
      const retries = Number(config.http.retries || 0);
      let lastError;
      for (let attempt = 0; attempt <= retries; attempt += 1) {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), Number(config.http.timeoutMs || 15000));
        try {
          const response = await fetch(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              no_verify: 'true',
              noVerify: 'true',
              ...extraHeaders,
            },
            body: JSON.stringify(removeUndefined(body)),
            signal: controller.signal,
          });
          const text = await response.text();
          let json;
          try {
            json = text ? JSON.parse(text) : {};
          } catch {
            throw new ApiError(`接口返回不是 JSON: ${url}`, { status: response.status, text });
          }
          if (!response.ok) {
            throw new ApiError(`HTTP ${response.status}: ${url}`, { status: response.status, json });
          }
          return json;
        } catch (error) {
          lastError = error;
          if (attempt < retries) {
            await sleep(100 * (attempt + 1));
          }
        } finally {
          clearTimeout(timer);
        }
      }
      throw lastError;
    },
  };
}

function removeUndefined(value) {
  if (Array.isArray(value)) return value.map(removeUndefined);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.entries(value)
      .filter(([, entryValue]) => entryValue !== undefined)
      .map(([key, entryValue]) => [key, removeUndefined(entryValue)]),
  );
}

function apiSuccess(response) {
  return String(response?.code) === '0';
}

function apiMessage(response) {
  return response?.message || response?.msg || '接口调用失败';
}

async function prepareUsers(config, users, api, requiredTicketCount) {
  const semaphore = new Semaphore(config.prepare.concurrency);
  return Promise.all(users.map((user, index) => semaphore.run(async () => prepareOneUser(
    config,
    user,
    api,
    requiredTicketCount,
    index,
  ))));
}

async function prepareOneUser(config, rawUser, api, requiredTicketCount, index) {
  const user = { ...rawUser };
  if (!user.userId) {
    if (!user.password || (!user.email && !user.mobile)) {
      throw new Error(`第 ${index + 1} 个用户缺少 userId，且没有 email/mobile + password，无法登录。`);
    }
    const loginResponse = await api.post('user', config.endpoints.login, {
      code: user.code || '0001',
      email: user.email,
      mobile: user.mobile,
      password: user.password,
    });
    if (!apiSuccess(loginResponse)) {
      throw new Error(`用户登录失败：${apiMessage(loginResponse)}`);
    }
    user.userId = asId(loginResponse.data?.userId, 'login.userId');
    user.token = loginResponse.data?.token;
  }

  user.ticketUserIds = asIdList(user.ticketUserIds || [], 'ticketUserIds');
  if (user.ticketUserIds.length < requiredTicketCount) {
    const ticketUsers = await queryTicketUsers(config, api, user.userId);
    user.ticketUserIds = ticketUsers.map((item) => asId(item.id, 'ticketUser.id')).filter(Boolean);
  }

  while (user.ticketUserIds.length < requiredTicketCount && config.prepare.autoCreateTicketUser) {
    const seq = `${index + 1}${user.ticketUserIds.length + 1}`.padStart(6, '0').slice(-6);
    const addResponse = await api.post('user', config.endpoints.ticketUserAdd, {
      userId: user.userId,
      relName: `压测购票人${seq}`,
      idType: 1,
      idNumber: `33010019900101${seq.slice(-4)}`,
    });
    if (!apiSuccess(addResponse)) {
      throw new Error(`创建购票人失败：${apiMessage(addResponse)}`);
    }
    const ticketUsers = await queryTicketUsers(config, api, user.userId);
    user.ticketUserIds = ticketUsers.map((item) => asId(item.id, 'ticketUser.id')).filter(Boolean);
  }

  if (user.ticketUserIds.length < requiredTicketCount) {
    throw new Error(`用户 ${user.userId} 的购票人数量不足，需要 ${requiredTicketCount} 个。`);
  }
  return {
    userId: user.userId,
    token: user.token,
    ticketUserIds: user.ticketUserIds,
  };
}

async function queryTicketUsers(config, api, userId) {
  const response = await api.post('user', config.endpoints.ticketUserList, { userId });
  if (!apiSuccess(response)) {
    throw new Error(`查询购票人失败：${apiMessage(response)}`);
  }
  return Array.isArray(response.data) ? response.data : [];
}

async function expandTargets(config, targets, api) {
  const result = [];
  for (const target of targets) {
    if (target.type !== 'movie') {
      result.push(target);
      continue;
    }
    const screeningIds = target.screeningId ? [target.screeningId] : target.screeningIds;
    if (screeningIds.length) {
      screeningIds.forEach((screeningId, index) => {
        result.push({
          ...target,
          name: screeningIds.length > 1 ? `${target.name}-场次${index + 1}` : target.name,
          screeningId,
        });
      });
      continue;
    }
    const response = await api.post('program', config.endpoints.movieScreeningList, {
      programId: target.programId,
      cinemaId: target.cinemaId,
      areaId: target.areaId,
      showDayTime: target.showDayTime,
    });
    if (!apiSuccess(response) || !Array.isArray(response.data) || response.data.length === 0) {
      console.warn(`[WARN] 电影目标 ${target.name} 没有查到可用场次。`);
      continue;
    }
    response.data
      .slice()
      .sort((a, b) => String(a.showTime || '').localeCompare(String(b.showTime || '')))
      .forEach((screening, index) => {
        result.push({
          ...target,
          name: `${target.name}-${screening.cinemaName || '影院'}-${screening.showTime || index + 1}`,
          screeningId: asId(screening.id, 'screening.id'),
          cinemaId: asId(screening.cinemaId, 'screening.cinemaId'),
        });
      });
  }
  return result;
}

async function resolveTicketCategory(config, api, target, cache) {
  if (target.ticketCategoryId) return target.ticketCategoryId;
  const cacheKey = `${target.type}:${target.programId}:${target.screeningId || ''}`;
  if (cache.has(cacheKey)) return cache.get(cacheKey);

  if (target.type === 'movie') {
    const seats = await fetchAvailableSeats(config, api, target);
    const firstSeat = seats[0];
    if (!firstSeat) throw new Error(`${target.name} 没有可售座位，无法自动选择票档。`);
    const ticketCategoryId = asId(firstSeat.ticketCategoryId, 'seat.ticketCategoryId');
    cache.set(cacheKey, ticketCategoryId);
    return ticketCategoryId;
  }

  const response = await api.post('program', config.endpoints.programDetail, { id: target.programId });
  if (!apiSuccess(response)) {
    throw new Error(`查询节目详情失败：${apiMessage(response)}`);
  }
  const categories = Array.isArray(response.data?.ticketCategoryVoList)
    ? response.data.ticketCategoryVoList.slice()
    : [];
  if (!categories.length) {
    throw new Error(`${target.name} 没有可用票档。`);
  }
  categories.sort((a, b) => Number(a.price || 0) - Number(b.price || 0));
  const ticketCategoryId = asId(categories[0].id, 'ticketCategory.id');
  cache.set(cacheKey, ticketCategoryId);
  return ticketCategoryId;
}

async function fetchAvailableSeats(config, api, target) {
  const endpoint = target.type === 'movie' ? config.endpoints.movieSeatInfo : config.endpoints.programSeatInfo;
  const body = target.type === 'movie' ? { screeningId: target.screeningId } : { programId: target.programId };
  const response = await api.post('program', endpoint, body);
  if (!apiSuccess(response)) {
    throw new Error(`查询座位失败：${apiMessage(response)}`);
  }
  const seats = flattenSeats(response.data?.seatVoMap || {});
  return seats.filter((seat) => isAvailableSeat(seat));
}

function flattenSeats(seatVoMap) {
  return Object.values(seatVoMap).flatMap((seatList) => Array.isArray(seatList) ? seatList : []);
}

function isAvailableSeat(seat) {
  const sellStatus = Number(seat.sellStatus ?? seat.redisSellStatus ?? seat.dbSellStatus ?? 1);
  const sellableFlag = Number(seat.sellableFlag ?? 1);
  const aisleFlag = Number(seat.aisleFlag ?? 0);
  const repairFlag = Number(seat.repairFlag ?? 0);
  return sellStatus === 1 && sellableFlag !== 0 && aisleFlag !== 1 && repairFlag !== 1;
}

function chooseSeats(seats, target, ticketCount, userIndex, attempt) {
  let candidates = seats;
  if (target.ticketCategoryId) {
    candidates = candidates.filter((seat) => asId(seat.ticketCategoryId, 'seat.ticketCategoryId') === target.ticketCategoryId);
  }
  if (isMeaningful(target.price)) {
    candidates = candidates.filter((seat) => Number(seat.price) === Number(target.price));
  }
  candidates = candidates.slice().sort((a, b) => {
    const rowDiff = Number(a.rowCode || 0) - Number(b.rowCode || 0);
    return rowDiff || Number(a.colCode || 0) - Number(b.colCode || 0);
  });
  if (candidates.length < ticketCount) return [];
  const offset = hashNumber(`${target.name}:${userIndex}:${attempt}`) % candidates.length;
  const rotated = candidates.slice(offset).concat(candidates.slice(0, offset));
  return rotated.slice(0, ticketCount);
}

function hashNumber(text) {
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = ((hash << 5) - hash) + text.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

function buildSeatDtoList(seats) {
  return seats.map((seat) => ({
    id: asId(seat.id, 'seat.id'),
    ticketCategoryId: asId(seat.ticketCategoryId, 'seat.ticketCategoryId'),
    rowCode: Number(seat.rowCode),
    colCode: Number(seat.colCode),
    seatType: seat.seatType === undefined ? undefined : Number(seat.seatType),
    price: String(seat.price),
  }));
}

async function buildOrderPayload(config, api, target, user, userIndex, attempt, targetCache, runId) {
  const ticketCount = Number(target.ticketCount || config.defaultTicketCount || 1);
  const ticketUserIdList = user.ticketUserIds.slice(0, ticketCount);
  if (ticketUserIdList.length < ticketCount) {
    throw new Error(`用户 ${user.userId} 购票人数量不足。`);
  }
  const payload = {
    programId: target.programId,
    screeningId: target.type === 'movie' ? target.screeningId : undefined,
    userId: user.userId,
    ticketUserIdList,
    clientRequestId: `${runId}-${userIndex}-${target.name}-${attempt}`,
  };

  if (target.seatMode === 'manual') {
    const seats = await fetchAvailableSeats(config, api, target);
    const selectedSeats = chooseSeats(seats, target, ticketCount, userIndex, attempt);
    if (selectedSeats.length < ticketCount) {
      throw new ApiError('没有足够可选座位', { businessType: 'sold_out' });
    }
    payload.seatDtoList = buildSeatDtoList(selectedSeats);
  } else {
    payload.ticketCategoryId = await resolveTicketCategory(config, api, target, targetCache);
    payload.ticketCount = ticketCount;
  }
  return payload;
}

async function createOrder(config, api, payload) {
  const start = performance.now();
  const extraHeaders = String(config.endpoints.createOrder || '').includes('/fast')
    ? { 'X-Tides-Simulator-Token': config.simulatorUserToken }
    : {};
  const response = await api.post('program', config.endpoints.createOrder, payload, extraHeaders);
  const latencyMs = performance.now() - start;
  return { response, latencyMs };
}

async function pollOrderNumber(config, api, cacheOrderNumber) {
  const start = performance.now();
  if (Number(config.orderVersion) !== 4) {
    return { orderNumber: cacheOrderNumber, latencyMs: 0, polls: 0 };
  }
  const deadline = Date.now() + Number(config.poll.timeoutMs || 15000);
  let polls = 0;
  while (Date.now() < deadline) {
    polls += 1;
    const response = await api.post('order', config.endpoints.orderCache, {
      orderNumber: cacheOrderNumber,
    });
    if (apiSuccess(response) && response.data) {
      return {
        orderNumber: String(response.data),
        latencyMs: performance.now() - start,
        polls,
      };
    }
    await sleep(Number(config.poll.intervalMs || 200));
  }
  throw new ApiError('订单异步创建超时', { cacheOrderNumber });
}

async function simulatePay(config, api, orderNumber) {
  if (!config.payment.enabled) return { paid: false, latencyMs: 0 };
  if (config.payment.mode !== 'simulate') {
    throw new Error(`暂不支持的支付模式：${config.payment.mode}`);
  }
  const start = performance.now();
  const response = await api.post(
    'order',
    config.endpoints.simulatePay,
    { orderNumber },
    { 'X-Tides-Simulator-Token': config.simulatorPayToken },
  );
  const latencyMs = performance.now() - start;
  if (!apiSuccess(response)) {
    throw new ApiError(`模拟支付失败：${apiMessage(response)}`, { response });
  }
  return { paid: true, latencyMs };
}

function classifyBusinessFailure(responseOrError) {
  const code = String(responseOrError?.code || responseOrError?.detail?.response?.code || '');
  const message = String(responseOrError?.message || responseOrError?.detail?.response?.message || responseOrError?.message || '');
  if (['40004', '40011', '40031'].includes(code) || /余票|库存|售罄|占用|锁定|not sufficient/i.test(message)) {
    return { reason: 'sold_or_conflict', retrySameTarget: code === '40004' || code === '40031' };
  }
  return { reason: code ? `business_${code}` : 'error', retrySameTarget: false };
}

async function rushOneUser(context, user, userIndex) {
  const {
    config,
    api,
    targets,
    targetCache,
    paySemaphore,
    reporter,
    runId,
  } = context;
  const attemptsPerTarget = Math.max(1, Number(config.rush.attemptsPerTarget || 1));
  const targetFailures = [];
  const userStart = performance.now();
  const userTargets = selectUserTargets(targets, config, userIndex);

  for (let targetIndex = 0; targetIndex < userTargets.length; targetIndex += 1) {
    const target = userTargets[targetIndex];
    for (let attempt = 1; attempt <= attemptsPerTarget; attempt += 1) {
      try {
        const payload = await buildOrderPayload(config, api, target, user, userIndex, attempt, targetCache, runId);
        const { response, latencyMs } = await createOrder(config, api, payload);
        if (!apiSuccess(response) || !response.data) {
          const failure = classifyBusinessFailure(response);
          reporter.write('create_failed', {
            userIndex,
            userId: user.userId,
            target: target.name,
            code: response?.code,
            message: apiMessage(response),
            latencyMs: round(latencyMs),
          });
          targetFailures.push({ target: target.name, reason: failure.reason, message: apiMessage(response) });
          if (failure.retrySameTarget && attempt < attemptsPerTarget) continue;
          break;
        }

        const pollResult = await pollOrderNumber(config, api, String(response.data));
        const orderNumber = pollResult.orderNumber;
        reporter.write('order_created', {
          userIndex,
          userId: user.userId,
          target: target.name,
          orderNumber,
          latencyMs: round(latencyMs),
          asyncOrderLatencyMs: round(pollResult.latencyMs),
          asyncOrderPolls: pollResult.polls,
        });

        const payResult = await paySemaphore.run(() => simulatePay(config, api, orderNumber));
        reporter.write('order_paid', {
          userIndex,
          userId: user.userId,
          target: target.name,
          orderNumber,
          paid: payResult.paid,
          latencyMs: round(payResult.latencyMs),
        });
        return {
          ok: true,
          paid: payResult.paid,
          orderNumber,
          target: target.name,
          createLatencyMs: latencyMs,
          asyncOrderLatencyMs: pollResult.latencyMs,
          payLatencyMs: payResult.latencyMs,
          endToEndLatencyMs: performance.now() - userStart,
        };
      } catch (error) {
        const failure = classifyBusinessFailure(error);
        reporter.write('attempt_error', {
          userIndex,
          userId: user.userId,
          target: target.name,
          reason: failure.reason,
          message: error.message,
        });
        targetFailures.push({ target: target.name, reason: failure.reason, message: error.message });
        if (failure.retrySameTarget && attempt < attemptsPerTarget) continue;
        break;
      }
    }
  }
  return { ok: false, paid: false, failures: targetFailures };
}

function selectUserTargets(targets, config, userIndex) {
  const maxTargetsPerUser = Number(config.rush.maxTargetsPerUser || 0);
  const strategy = String(config.rush.targetStrategy || 'sequence').toLowerCase();
  let orderedTargets = targets;
  if (strategy === 'rotate' || strategy === 'round-robin' || strategy === 'roundrobin') {
    const startIndex = userIndex % targets.length;
    orderedTargets = [...targets.slice(startIndex), ...targets.slice(0, startIndex)];
  }
  if (maxTargetsPerUser > 0) {
    return orderedTargets.slice(0, maxTargetsPerUser);
  }
  return orderedTargets;
}

async function runRush(config, users, targets, reporter, options = {}) {
  const runId = reporter.runId;
  const api = createApi(config);
  const expandedTargets = await expandTargets(config, targets, api);
  if (!expandedTargets.length) throw new Error('没有可用抢票目标。');
  const maxTicketCount = Math.max(...expandedTargets.map((target) => Number(target.ticketCount || config.defaultTicketCount || 1)));
  const preparedUsers = await prepareUsers(config, users, api, maxTicketCount);
  const activeUsers = pickUsers(preparedUsers, Number(config.rush.users), config.reuseUsers);
  await waitForStart(config.rush.startAt);

  const stats = {
    total: activeUsers.length,
    success: 0,
    paid: 0,
    failed: 0,
    createLatencyMs: [],
    asyncOrderLatencyMs: [],
    payLatencyMs: [],
    endToEndLatencyMs: [],
    byTarget: {},
    failures: {},
  };
  const startTime = Date.now();
  const rushSemaphore = new Semaphore(config.rush.concurrency);
  const paySemaphore = new Semaphore(config.payment.concurrency);
  const targetCache = new Map();
  let finished = 0;
  const progressTimer = setInterval(() => {
    console.log(`进度 ${finished}/${activeUsers.length}，成功 ${stats.success}，支付 ${stats.paid}，失败 ${stats.failed}`);
  }, 5000);

  await Promise.all(activeUsers.map((user, userIndex) => rushSemaphore.run(async () => {
    const result = await rushOneUser({
      config,
      api,
      targets: expandedTargets,
      targetCache,
      paySemaphore,
      reporter,
      runId,
    }, user, userIndex);

    finished += 1;
    if (result.ok) {
      stats.success += 1;
      if (result.paid) stats.paid += 1;
      stats.createLatencyMs.push(result.createLatencyMs);
      stats.asyncOrderLatencyMs.push(result.asyncOrderLatencyMs);
      stats.payLatencyMs.push(result.payLatencyMs);
      stats.endToEndLatencyMs.push(result.endToEndLatencyMs);
      stats.byTarget[result.target] = (stats.byTarget[result.target] || 0) + 1;
    } else {
      stats.failed += 1;
      const reason = result.failures?.at(-1)?.reason || 'unknown';
      stats.failures[reason] = (stats.failures[reason] || 0) + 1;
    }
    if (options.onProgress) {
      options.onProgress(buildLiveSummary(stats, Date.now() - startTime, expandedTargets, config));
    }
  })));

  clearInterval(progressTimer);
  return buildSummary(stats, Date.now() - startTime, expandedTargets, config);
}

function pickUsers(users, n, reuseUsers) {
  if (!users.length) throw new Error('用户列表为空。');
  if (users.length >= n) return users.slice(0, n);
  if (!reuseUsers) {
    throw new Error(`用户数量不足：需要 ${n} 个，实际 ${users.length} 个。可以补充用户，或显式加 --reuse-users 做非真实账号复用测试。`);
  }
  return Array.from({ length: n }, (_, index) => users[index % users.length]);
}

async function waitForStart(startAt) {
  if (!startAt) return;
  const startTime = new Date(startAt).getTime();
  if (!Number.isFinite(startTime)) throw new Error(`startAt 格式不正确：${startAt}`);
  const waitMs = startTime - Date.now();
  if (waitMs > 0) {
    console.log(`等待到 ${new Date(startTime).toISOString()} 统一开抢，剩余 ${Math.ceil(waitMs / 1000)} 秒。`);
    await sleep(waitMs);
  }
}

function buildLiveSummary(stats, durationMs, targets, config) {
  return buildSummary(stats, durationMs, targets, config, false);
}

function buildSummary(stats, durationMs, targets, config = {}, final = true) {
  const completedUsers = stats.success + stats.failed;
  const durationSeconds = Math.max(durationMs / 1000, 0.001);
  const successRate = stats.total ? stats.success / stats.total : 0;
  const paidRate = stats.success ? stats.paid / stats.success : 0;
  const failureRate = stats.total ? stats.failed / stats.total : 0;
  return {
    status: final ? 'finished' : 'running',
    updatedAt: new Date().toISOString(),
    durationMs,
    totalUsers: stats.total,
    completedUsers,
    successOrders: stats.success,
    paidOrders: stats.paid,
    failedUsers: stats.failed,
    successRate: round(successRate * 100),
    paidRate: round(paidRate * 100),
    failureRate: round(failureRate * 100),
    configuredConcurrency: Number(config.rush?.concurrency || 0),
    configuredPayConcurrency: Number(config.payment?.concurrency || 0),
    throughput: {
      completedUsersPerSecond: round(completedUsers / durationSeconds),
      successfulOrdersPerSecond: round(stats.success / durationSeconds),
      paidOrdersPerSecond: round(stats.paid / durationSeconds),
    },
    latencyMs: {
      createOrder: percentileSummary(stats.createLatencyMs),
      asyncOrderCache: percentileSummary(stats.asyncOrderLatencyMs),
      simulatePay: percentileSummary(stats.payLatencyMs),
      endToEnd: percentileSummary(stats.endToEndLatencyMs),
    },
    qualityGates: {
      createSuccessRateGte95: successRate >= 0.95,
      paidSuccessRateGte99: !config.payment?.enabled || paidRate >= 0.99,
      endToEndP99Lt10s: percentile(stats.endToEndLatencyMs
        .filter((value) => Number.isFinite(value))
        .slice()
        .sort((a, b) => a - b), 0.99) < 10000,
    },
    byTarget: stats.byTarget,
    failures: stats.failures,
    targets: targets.map((target) => ({
      name: target.name,
      type: target.type,
      programId: target.programId,
      screeningId: target.screeningId,
      ticketCategoryId: target.ticketCategoryId,
      seatMode: target.seatMode,
      ticketCount: target.ticketCount,
    })),
  };
}

function percentileSummary(values) {
  const list = values.filter((value) => Number.isFinite(value)).slice().sort((a, b) => a - b);
  if (!list.length) return { count: 0 };
  return {
    count: list.length,
    min: round(list[0]),
    p50: round(percentile(list, 0.5)),
    p90: round(percentile(list, 0.9)),
    p95: round(percentile(list, 0.95)),
    p99: round(percentile(list, 0.99)),
    max: round(list[list.length - 1]),
  };
}

function percentile(sorted, p) {
  if (!sorted.length) return 0;
  const index = Math.min(sorted.length - 1, Math.ceil(sorted.length * p) - 1);
  return sorted[index];
}

function round(value) {
  return Math.round(Number(value) * 100) / 100;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function startUiServer(args, baseConfig) {
  const port = Number(args.port || 6170);
  const host = args.host || '127.0.0.1';
  const publicDir = path.join(TOOL_ROOT, 'public');
  const runs = new Map();

  const server = createServer(async (request, response) => {
    try {
      const requestUrl = new URL(request.url, `http://${request.headers.host || `${host}:${port}`}`);
      if (request.method === 'GET' && requestUrl.pathname === '/api/defaults') {
        const targetsPath = path.join(TOOL_ROOT, 'targets.local.json');
        const targetsExamplePath = path.join(TOOL_ROOT, 'targets.example.json');
        return sendJson(response, {
          configPath: path.join(TOOL_ROOT, 'config.local.json'),
          usersPath: path.join(TOOL_ROOT, 'users.generated.json'),
          targetsPath,
          targetsText: await readOptionalText(targetsPath) || await readOptionalText(targetsExamplePath),
          config: baseConfig,
        });
      }

      if (request.method === 'POST' && requestUrl.pathname === '/api/users/generate') {
        const payload = await readRequestJson(request);
        const result = await generateUsersFromUi(payload, baseConfig);
        return sendJson(response, result);
      }

      if (request.method === 'POST' && requestUrl.pathname === '/api/run') {
        const payload = await readRequestJson(request);
        const run = createRunState();
        runs.set(run.runId, run);
        sendJson(response, { runId: run.runId });
        void startUiRun(run, payload, args, baseConfig);
        return;
      }

      const eventMatch = requestUrl.pathname.match(/^\/api\/runs\/([^/]+)\/events$/);
      if (request.method === 'GET' && eventMatch) {
        const run = runs.get(eventMatch[1]);
        if (!run) return sendJson(response, { message: 'run not found' }, 404);
        attachSse(run, response);
        return;
      }

      const runMatch = requestUrl.pathname.match(/^\/api\/runs\/([^/]+)$/);
      if (request.method === 'GET' && runMatch) {
        const run = runs.get(runMatch[1]);
        if (!run) return sendJson(response, { message: 'run not found' }, 404);
        return sendJson(response, publicRunState(run));
      }

      return serveStatic(publicDir, requestUrl.pathname, response);
    } catch (error) {
      return sendJson(response, { message: error.message, stack: error.stack }, 500);
    }
  });

  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, host, () => {
      server.off('error', reject);
      resolve();
    });
  });
  console.log(`抢票压测控制台已启动：http://${host}:${port}`);
}

function createRunState() {
  return {
    runId: `${new Date().toISOString().replace(/[:.]/g, '-')}-ui`,
    status: 'starting',
    recentEvents: [],
    summary: null,
    error: null,
    sseClients: new Set(),
  };
}

async function startUiRun(run, payload, args, baseConfig) {
  try {
    run.status = 'running';
    publishRunState(run, 'state');
    const config = await buildUiRunConfig(payload, baseConfig);
    const usersPath = await resolveInputPath(payload.usersPath || args.users || config.usersFile || path.join(TOOL_ROOT, 'users.generated.json'));
    const targetsText = typeof payload.targetsJson === 'string' ? payload.targetsJson.trim() : '';
    const targetsPath = targetsText
      ? null
      : await resolveInputPath(payload.targetsPath || args.targets || config.targetsFile || path.join(TOOL_ROOT, 'targets.local.json'));
    const users = Array.isArray(payload.users) ? payload.users.map(normalizeUser) : await loadUsers(usersPath);
    const targets = targetsText ? parseTargetsText(targetsText) : await loadTargets(targetsPath);
    const reportDir = resolveReportDir(config.reportDir || 'reports');
    const reporter = new Reporter(reportDir, run.runId, (event) => {
      run.recentEvents.unshift(event);
      run.recentEvents = run.recentEvents.slice(0, 80);
      publishRunState(run, 'event', event);
    });
    await reporter.open();
    reporter.write('run_started', {
      users: config.rush.users,
      rushConcurrency: config.rush.concurrency,
      paymentEnabled: config.payment.enabled,
      paymentConcurrency: config.payment.concurrency,
      usersPath,
      targetsPath: targetsPath || 'ui-targets-json',
    });
    const summary = await runRush(config, users, targets, reporter, {
      onProgress(summaryData) {
        run.summary = summaryData;
        publishRunState(run, 'progress', summaryData);
      },
    });
    run.status = 'finished';
    run.summary = summary;
    await reporter.close(summary);
    publishRunState(run, 'finished', summary);
  } catch (error) {
    run.status = 'failed';
    run.error = { message: error.message, stack: error.stack };
    publishRunState(run, 'failed', run.error);
  }
}

async function buildUiRunConfig(payload, baseConfig) {
  let config = deepMerge({}, baseConfig);
  if (payload.configPath) {
    const configPath = await resolveInputPath(payload.configPath);
    config = deepMerge(DEFAULT_CONFIG, await loadJson(configPath));
  }
  if (payload.config && typeof payload.config === 'object') {
    config = deepMerge(config, payload.config);
  }
  if (payload.n) config.rush.users = Number(payload.n);
  if (payload.rushConcurrency) config.rush.concurrency = Number(payload.rushConcurrency);
  if (payload.attemptsPerTarget) config.rush.attemptsPerTarget = Number(payload.attemptsPerTarget);
  if (payload.defaultTicketCount) config.defaultTicketCount = Number(payload.defaultTicketCount);
  if (payload.payConcurrency) config.payment.concurrency = Number(payload.payConcurrency);
  if (payload.pollIntervalMs) config.poll.intervalMs = Number(payload.pollIntervalMs);
  if (payload.pollTimeoutMs) config.poll.timeoutMs = Number(payload.pollTimeoutMs);
  if (payload.httpTimeoutMs) config.http.timeoutMs = Number(payload.httpTimeoutMs);
  if (payload.httpRetries !== undefined) config.http.retries = Number(payload.httpRetries);
  if (payload.startAt) config.rush.startAt = payload.startAt;
  if (payload.simulatorPayToken) config.simulatorPayToken = payload.simulatorPayToken;
  if (payload.simulatorUserToken) config.simulatorUserToken = payload.simulatorUserToken;
  if (payload.reuseUsers !== undefined) config.reuseUsers = Boolean(payload.reuseUsers);
  if (payload.paymentEnabled !== undefined) config.payment.enabled = Boolean(payload.paymentEnabled);
  return config;
}

async function generateUsersFromUi(payload, baseConfig) {
  const config = await buildUiRunConfig(payload, baseConfig);
  const generator = deepMerge(config.userGenerator || {}, payload.userGenerator || {});
  const outputPath = await resolveOutputPath(generator.outputPath || 'users.generated.json');
  const api = createApi(config);
  const response = await api.post(
    'user',
    config.endpoints.simulatorUserPrepare,
    {
      count: Number(generator.count || 100),
      startIndex: Number(generator.startIndex || 1),
      mobilePrefix: generator.mobilePrefix || '19099',
      emailDomain: generator.emailDomain || 'sandbox.local',
      password: generator.password || '111111',
      ticketUserCount: Number(generator.ticketUserCount || 1),
      relNamePrefix: generator.relNamePrefix || '压测购票人',
      overwritePassword: Boolean(generator.overwritePassword),
    },
    {
      'X-Tides-Simulator-Token': config.simulatorUserToken || config.simulatorPayToken,
    },
  );
  if (!apiSuccess(response)) {
    throw new Error(`生成压测用户失败：${apiMessage(response)}`);
  }
  const users = Array.isArray(response.data?.userList) ? response.data.userList.map(normalizeUser) : [];
  if (!users.length) {
    throw new Error('用户服务没有返回可用用户。');
  }
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, `${JSON.stringify(users, null, 2)}\n`, 'utf8');
  return {
    outputPath,
    summary: {
      requestedCount: response.data?.requestedCount ?? users.length,
      createdCount: response.data?.createdCount ?? 0,
      reusedCount: response.data?.reusedCount ?? 0,
      ticketUserCreatedCount: response.data?.ticketUserCreatedCount ?? 0,
      ticketUserCount: response.data?.ticketUserCount ?? generator.ticketUserCount,
    },
    preview: users.slice(0, 5),
  };
}

async function resolveInputPath(filePath) {
  if (path.isAbsolute(filePath)) return filePath;
  const cwdPath = path.resolve(process.cwd(), filePath);
  if (await fileExists(cwdPath)) return cwdPath;
  return path.resolve(TOOL_ROOT, filePath);
}

async function resolveOutputPath(filePath) {
  if (path.isAbsolute(filePath)) return filePath;
  return path.resolve(TOOL_ROOT, filePath);
}

async function readOptionalText(filePath) {
  try {
    return await fs.readFile(filePath, 'utf8');
  } catch {
    return '';
  }
}

async function fileExists(filePath) {
  try {
    await fs.access(filePath);
    return true;
  } catch {
    return false;
  }
}

function publishRunState(run, eventName, payload = null) {
  const data = JSON.stringify({
    ...publicRunState(run),
    payload,
  });
  for (const client of run.sseClients) {
    client.write(`event: ${eventName}\n`);
    client.write(`data: ${data}\n\n`);
  }
}

function publicRunState(run) {
  return {
    runId: run.runId,
    status: run.status,
    summary: run.summary,
    error: run.error,
    recentEvents: run.recentEvents,
  };
}

function attachSse(run, response) {
  response.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
    'Access-Control-Allow-Origin': '*',
  });
  run.sseClients.add(response);
  response.write(`event: state\n`);
  response.write(`data: ${JSON.stringify(publicRunState(run))}\n\n`);
  response.on('close', () => {
    run.sseClients.delete(response);
  });
}

async function readRequestJson(request) {
  const chunks = [];
  for await (const chunk of request) {
    chunks.push(chunk);
  }
  const text = Buffer.concat(chunks).toString('utf8');
  return text ? JSON.parse(text) : {};
}

function sendJson(response, data, statusCode = 200) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
  });
  response.end(JSON.stringify(data));
}

async function serveStatic(publicDir, pathname, response) {
  const normalizedPath = pathname === '/' ? '/index.html' : pathname;
  const filePath = path.resolve(publicDir, `.${decodeURIComponent(normalizedPath)}`);
  if (!filePath.startsWith(publicDir)) {
    response.writeHead(403);
    response.end('Forbidden');
    return;
  }
  try {
    const content = await fs.readFile(filePath);
    response.writeHead(200, { 'Content-Type': contentType(filePath) });
    response.end(content);
  } catch {
    response.writeHead(404);
    response.end('Not Found');
  }
}

function contentType(filePath) {
  if (filePath.endsWith('.html')) return 'text/html; charset=utf-8';
  if (filePath.endsWith('.css')) return 'text/css; charset=utf-8';
  if (filePath.endsWith('.js')) return 'text/javascript; charset=utf-8';
  if (filePath.endsWith('.json')) return 'application/json; charset=utf-8';
  if (filePath.endsWith('.svg')) return 'image/svg+xml';
  return 'application/octet-stream';
}

function resolveReportDir(reportDir) {
  return path.isAbsolute(reportDir) ? reportDir : path.resolve(TOOL_ROOT, reportDir);
}

async function main() {
  const args = parseArgs(process.argv);
  const command = args._[0] || 'run';
  const config = await loadConfig(args);
  if (command === 'ui') {
    await startUiServer(args, config);
    return;
  }
  const usersFile = await resolveInputPath(args.users || config.usersFile || 'users.local.json');
  const users = await loadUsers(usersFile);
  const targets = command === 'prepare-users'
    ? []
    : await loadTargets(await resolveInputPath(args.targets || config.targetsFile || 'targets.local.json'));
  const runId = `${new Date().toISOString().replace(/[:.]/g, '-')}-${command}`;
  const reportDir = resolveReportDir(config.reportDir);

  if (command === 'prepare-users') {
    const api = createApi(config);
    const prepared = await prepareUsers(config, users, api, Number(args['ticket-count'] || config.defaultTicketCount || 1));
    const output = path.resolve(args.output || 'users.prepared.json');
    await fs.writeFile(output, `${JSON.stringify(prepared, null, 2)}\n`, 'utf8');
    console.log(`已写入预处理用户：${output}`);
    return;
  }

  if (command !== 'run') {
    throw new Error(`未知命令：${command}`);
  }

  const reporter = new Reporter(reportDir, runId);
  await reporter.open();
  reporter.write('run_started', {
    users: config.rush.users,
    rushConcurrency: config.rush.concurrency,
    paymentEnabled: config.payment.enabled,
    paymentConcurrency: config.payment.concurrency,
  });
  try {
    const summary = await runRush(config, users, targets, reporter);
    await reporter.close(summary);
    console.log(JSON.stringify(summary, null, 2));
    console.log(`报告已生成：${reporter.eventsFile}`);
    console.log(`汇总已生成：${reporter.summaryFile}`);
  } catch (error) {
    reporter.write('run_failed', { message: error.message, stack: error.stack });
    await reporter.close({ failed: true, message: error.message, stack: error.stack });
    throw error;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
