# YoumuChan

> **Velocity 代理端 AI 聊天机器人插件** — 以「妖梦酱」的身份自主参与 Minecraft 服务器聊天。

**版本**: 2.0 · **平台**: Velocity 3.3+ · **Java**: 21+ · **作者**: Allivilsey

---

## 概述

YoumuChan 是一个运行在 Velocity 代理层的 AI 聊天插件。它会实时监听全服玩家的聊天、加入/退出等游戏内事件，通过**双阶段 AI 推理链路**（边界分析模型 + 主对话模型）自主生成符合角色设定的回复，并广播到所有子服。

插件的核心特色是**自适应调度**：玩家越活跃，AI 回复频率越高；无人在线或冷场时自动休眠，无需人工干预。

---

## 架构概览

```
               ┌─ InGameInfoListener ─┐
 玩家事件 ───→ │  HeatControllerListener │ ──→ InGameInfoCollector (事件缓存)
               │  FocusControllerListener│      HeatController     (热度引擎)
               └──────────────────────┘      FocusController    (专注追踪)
                                                      │
                          ┌───────────────────────────┘
                          ▼
                  GhostInThePlugin (总调度器)
                    │ 每秒心跳, 根据热度决定调度间隔
                    ▼
            MentalStateController
              SLEEP ←→ DREAM 状态机
                    │
                    ▼ (仅 DREAM 时触发)
              KaianPassageway (推理管线)
                    │
           ┌────────┴────────┐
     阶段一: Border 模型      阶段二: Youmu 模型
     (信息过滤/情感分析)       (角色对话生成)
                    │
                    ▼
              MessageSender → 广播至全部子服
```

### 关键组件说明

| 组件 | 职责 |
|---|---|
| **HeatController** | 维护热度值（指数衰减），玩家行为注入燃料，热度驱动调度节奏 |
| **FocusController** | 追踪各玩家活跃度分数（指数衰减），决定 AI 当前关注的目标玩家，带滞回切换和锁定机制 |
| **MentalStateController** | SLEEP/DREAM 状态机，根据在线人数与热度自动切换，支持手动锁定 |
| **GhostInThePlugin** | 总调度器：1 秒心跳轮询，按热度动态计算推理间隔，串行执行 AI 链路 |
| **KaianPassageway** | 两阶段推理管线：先用轻量模型做边界分析，再用主模型生成角色对话 |
| **InGameInfoCollector** | 按时间窗口和容量上限缓存游戏内事件（聊天、进退服等） |
| **DebugInfo** | 以 BossBar 形式向订阅者玩家实时展示热度、燃料、目标玩家等内部状态 |

---

## 配置文件

配置文件位于 `plugins/youmuchan/config.yml`，首次启动自动生成。

### 基础 API 设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `api_key` | `your-api-key` | 模型 API Key |
| `api_url` | `https://dashscope.aliyuncs.com/...` | OpenAI 兼容格式的 API 地址（默认使用阿里云百炼）|
| `debug_mode` | `false` | 开启后在控制台打印完整 API 请求/响应 JSON |

### AI 模型设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `border_model` | `qwen3.5-flash` | 边界分析模型（轻量级，做信息过滤和情感分析） |
| `border_temperature` | `0.0` | 边界模型温度（0.0 = 最确定性输出） |
| `youmu_model` | `qwen3.5-plus` | 主对话模型（生成最终角色回复） |
| `youmu_temperature` | `0.7` | 主模型温度（越高越活泼多样） |

### 调度与缓存设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `time_window_ms` | `1800000`（30 分钟） | AI 构建上下文时追溯的聊天记录时间窗口 |
| `base_interval_ms` | `20000`（20 秒） | 基础调度间隔，实际间隔 = 基础值 / 热度倍率 |
| `cache_duration_ms` | `600000`（10 分钟） | 游戏事件在内存中的最大保留时间 |
| `cache_max_size` | `30` | 游戏事件缓存的最大条数 |
| `half_life_seconds` | `60` | 热度衰减半衰期（秒），越小降温越快 |

### 游戏内显示

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `youmu_name` | `妖梦酱` | AI 在游戏内发言时显示的名称 |

---

## 命令参考

所有命令以 `/youmu` 为前缀。

| 命令 | 权限节点 | 说明 |
|---|---|---|
| `/youmu reload` | `youmuchan.reload` | 重载配置文件并重启所有组件 |
| `/youmu start <时间>` | `youmuchan.start` | 强制切换至 DREAM 状态（必须指定持续时间） |
| `/youmu stop [时间]` | `youmuchan.stop` | 切换至 SLEEP 状态（不指定时间则永久休眠） |
| `/youmu debug` | `youmuchan.debug` | 切换 Debug 模式开关 |
| `/youmu debug info` | `youmuchan.debug` | 切换 BossBar 实时状态面板（仅玩家可用） |
| `/youmu clear` | `youmuchan.clear` | 清除聊天记录缓存 |
| `/youmu setmodel border <name>` | `youmuchan.setmodel` | 热更新边界分析模型 |
| `/youmu setmodel youmu <name>` | `youmuchan.setmodel` | 热更新主对话模型 |
| `/youmu setkey <api_key>` | `youmuchan.setkey` | 热更新 API Key |
| `/youmu seturl <api_url>` | `youmuchan.seturl` | 热更新 API URL |

---

## 常见运维操作

### 首次部署

```
1. 部署 jar → 启动 Velocity → 填写 api_key → /youmu reload
2. 插件默认为 SLEEP 状态，有玩家在线且热度达标后自动进入 DREAM
```

### 临时关停 AI 发言

```
/youmu stop            ← 永久休眠，直到手动唤醒
/youmu stop 30m        ← 休眠 30 分钟后自动恢复
```

### 强制唤醒 AI

```
/youmu start 1h        ← 强制 DREAM 状态 1 小时
```

### 切换模型（无需重启）

```
/youmu setmodel youmu qwen-turbo
/youmu setmodel border qwen3.5-flash
```

### 排查问题

```
/youmu debug           ← 开启控制台详细日志
/youmu debug info      ← 在游戏内 BossBar 查看实时状态
```

BossBar 面板显示字段：`Heat` (热度) · `Fuel` (燃料值) · `Cache` (缓存条数) · `TgtPlayer` (关注目标) · `TgtScore` (目标分数) · `TgtTime` (锁定剩余) · `NextPulse` (下次推理倒计时)

---