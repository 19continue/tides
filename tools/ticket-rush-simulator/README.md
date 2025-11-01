# 潮声抢票压测模拟器

这个工具用于自有演示环境压测抢票链路。它不会访问支付宝沙箱；抢票成功后会调用订单服务的 `/order/simulate/pay`，复用真实支付成功后的订单、电子票和库存推进逻辑，把订单推进为已支付。

## 能测什么

- 几百到 3000-5000 个用户同时请求抢票。
- 按目标列表顺序抢票：第一个节目或场次失败、售罄、座位冲突后，自动尝试下一个。
- 演出和电影都支持。
- 支持自动分配座位，也支持按实时座位图选座。
- 输出事件明细和汇总报告，包括成功数、支付数、失败原因、P50/P90/P99。

## 前置条件

- Node.js 18 或以上。
- 后端至少启动 user、program、order 服务，以及它们依赖的 Redis、Kafka、MySQL、Nacos 等。
- order 服务已包含演示压测支付接口，并且请求头 `X-Tides-Simulator-Token` 与配置 `ticket-rush-simulator.pay-token` 一致。
- user 服务已包含演示压测用户准备接口，并且请求头 `X-Tides-Simulator-Token` 与配置 `ticket-rush-simulator.user-token` 一致。
- 建议先在后台保存一次演示数据快照，压测后再恢复。

## 配置文件

复制示例文件后修改：

```powershell
Copy-Item tools\ticket-rush-simulator\config.example.json tools\ticket-rush-simulator\config.local.json
Copy-Item tools\ticket-rush-simulator\users.example.json tools\ticket-rush-simulator\users.local.json
Copy-Item tools\ticket-rush-simulator\targets.example.json tools\ticket-rush-simulator\targets.local.json
```

所有 Java Long 类型 ID 建议写成字符串，避免 JavaScript 大整数精度丢失：

```json
{
  "userId": "100000000000000001",
  "ticketUserIds": ["100000000000000101"]
}
```

如果走网关，把 `serviceBaseUrls` 改成：

```json
{
  "user": "http://127.0.0.1:6085/tides/user",
  "program": "http://127.0.0.1:6085/tides/program",
  "order": "http://127.0.0.1:6085/tides/order"
}
```

默认是直连微服务：

```json
{
  "user": "http://127.0.0.1:6082",
  "program": "http://127.0.0.1:6086",
  "order": "http://127.0.0.1:8081"
}
```

## 用户文件

UI 里可以直接点“生成并创建用户”。它会调用 user 服务的 `/user/simulator/prepare`，自动创建真实用户、手机号、邮箱、实名认证信息和购票人，并写入 `tools/ticket-rush-simulator/users.generated.json`。

生成接口只用于演示压测环境，普通注册仍然走验证码和频控。部署到服务器后请修改：

```yaml
ticket-rush-simulator:
  user-token: "改成自己的强 token"
  pay-token: "改成自己的强 token"
```

也可以手动准备真实 `userId` 和 `ticketUserIds`，这样开抢前不用登录，也不会把登录接口混进抢票压测：

```json
[
  {
    "userId": "100000000000000001",
    "ticketUserIds": ["100000000000000101"]
  }
]
```

如果只填邮箱或手机号，工具会在开抢前登录并查询购票人：

```json
[
  {
    "email": "demo-user@example.com",
    "password": "123456"
  }
]
```

用户数不足时，默认会中止。确实只想做非真实账号复用测试时，加 `--reuse-users`。

## 目标文件

演出自动分配座位：

```json
{
  "name": "周杰伦演唱会",
  "type": "program",
  "programId": "26001",
  "ticketCategoryId": "70001",
  "ticketCount": 1,
  "seatMode": "auto"
}
```

电影真实选座：

```json
{
  "name": "电影黄金场",
  "type": "movie",
  "programId": "26000",
  "screeningIds": ["880001", "880002"],
  "ticketCount": 1,
  "seatMode": "manual"
}
```

电影也可以不填 `screeningIds`，工具会按 `programId` 查询当前可售场次并按时间排序。

## 运行

启动本地 UI：

```powershell
node tools\ticket-rush-simulator\src\index.mjs ui --port 6170
```

然后打开：

```text
http://127.0.0.1:6170
```

UI 支持直接配置：

- user、program、order 服务地址。
- 支付推进 token、用户生成 token。
- 用户数量、手机号前缀、邮箱域名、购票人数量和输出文件。
- 抢票用户数、抢票并发、支付并发、轮询和 HTTP 超时。
- 目标节目/电影 JSON。填写后优先使用页面内容；留空才读取目标文件。

小规模冒烟：

```powershell
node tools\ticket-rush-simulator\src\index.mjs run --config tools\ticket-rush-simulator\config.local.json --users tools\ticket-rush-simulator\users.local.json --targets tools\ticket-rush-simulator\targets.local.json --n 20 --concurrency 20 --pay-concurrency 10
```

演示压测：

```powershell
node tools\ticket-rush-simulator\src\index.mjs run --config tools\ticket-rush-simulator\config.local.json --users tools\ticket-rush-simulator\users.local.json --targets tools\ticket-rush-simulator\targets.local.json --n 3000 --concurrency 500 --pay-concurrency 100
```

定时统一开抢：

```powershell
node tools\ticket-rush-simulator\src\index.mjs run --config tools\ticket-rush-simulator\config.local.json --users tools\ticket-rush-simulator\users.local.json --targets tools\ticket-rush-simulator\targets.local.json --n 3000 --concurrency 500 --start-at "2026-05-26T14:00:00+08:00"
```

只测抢票，不推进支付：

```powershell
node tools\ticket-rush-simulator\src\index.mjs run --config tools\ticket-rush-simulator\config.local.json --users tools\ticket-rush-simulator\users.local.json --targets tools\ticket-rush-simulator\targets.local.json --n 1000 --concurrency 300 --no-pay
```

报告会输出到 `tools/ticket-rush-simulator/reports/`。

## 指标

UI 和汇总报告会展示这些指标：

- 抢票成功率：成功创建订单的用户数 / 本次压测用户数。
- 支付推进率：模拟支付成功订单数 / 已创建订单数。
- 吞吐：完成用户数/s、成功订单/s、支付订单/s。
- 抢票接口延迟：`/program/order/create/v4` 的 P50、P90、P95、P99。
- 异步订单耗时：v4 返回临时订单号后，轮询 `/order/get/cache` 拿到真实订单号的耗时。
- 模拟支付耗时：`/order/simulate/pay` 推进订单、电子票、库存状态的耗时。
- 端到端耗时：单个用户从开始尝试到支付推进完成的总耗时。
- 目标命中：每个节目或电影场次最终成交订单数。
- 失败原因：售罄/座位冲突、业务错误、接口异常等错误分布。

默认质量门槛用于演示判断：

- 抢票成功率 >= 95%
- 支付推进率 >= 99%
- 端到端 P99 < 10s

## 调参建议

- 先用 `20 -> 100 -> 500 -> 1000 -> 3000` 逐步加压，观察 Redis、Kafka、MySQL、order/program 服务日志。
- `--concurrency` 是同时抢票的 HTTP 并发，不一定等于用户总数。
- `--pay-concurrency` 是模拟支付成功的并发，太高会集中打订单状态推进和库存落库链路。
- 电影 `seatMode=manual` 会频繁查座位图，压力更接近真实选座；演出高并发主测建议用 `seatMode=auto`。

## 安全说明

`/order/simulate/pay` 只能用于自有演示或压测环境。部署到公网时请修改 `ticket-rush-simulator.pay-token`，演示结束后关闭该接口或移除相关配置。
