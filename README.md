# YoumuChan

> **Velocity 代理端 AI 聊天机器人插件** - 以「妖梦酱」的身份自主参与 Minecraft 服务器聊天，并可配合 Paper 数据探针采集更多子服事件。

**版本**: 2.1 · **平台**: Velocity 3.3+ / Paper 1.20.4+ · **Java**: 21+ · **作者**: Allivilsey

---

## 概述

YoumuChan 是一个运行在 Velocity 代理层的 AI 聊天插件。它会实时监听全服玩家的聊天、加入/退出等游戏内事件，通过**双阶段 AI 推理链路**（边界分析模型 + 主对话模型）自主生成符合角色设定的回复，并广播到所有子服。

当前仓库是一个 **Maven 多模块项目**：

| 模块 | 作用 |
|---|---|
| `youmuchan-velocity` | 主插件，负责 AI 调度、上下文构建、模型调用、消息广播与命令控制 |
| `youmuchan-paper` | Paper 侧数据探针，通过 TCP 将子服事件、热度和关注信息上报给 Velocity |

插件的核心特色是**自适应调度**：玩家越活跃，AI 回复频率越高；无人在线或冷场时自动休眠，无需人工干预。

感谢 Claude、Gemini、ChatGPT 的协助，没他们我造不出这么高的屎山。

---

## 架构概览

```text
  Paper 子服事件
        │
        ▼
 youmuchan-paper (数据探针)
        │  TCP 上报 info / fuel / focus
        ▼
 Hanrei (Velocity TCP 接收器)
        │
        ├─ InGameInfoCollector (事件缓存)
        ├─ HeatController     (热度引擎)
        └─ FocusController    (专注追踪)
                      │
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
| **Hanrei** | Velocity 侧 TCP 接收器，负责接收来自 Paper 探针的消息并分发处理 |
| **DebugInfo** | 以 BossBar 形式向订阅者玩家实时展示热度、燃料、目标玩家等内部状态 |

### 可选依赖

| 依赖 | 用途 |
|---|---|
| **LuckPerms** | 控制玩家是否可见 AI 发言，并支持 `/youmu mute` |
| **LiteBans** | 将惩罚事件桥接进上下文，供 AI 感知服务器管理动态 |

---

## 构建与产物

### 构建命令

```bash
mvn clean package
```

构建完成后，产物默认位于：

| 模块 | 产物路径 |
|---|---|
| Velocity 主插件 | `youmuchan-velocity/target/youmuchan-velocity-2.1.jar` |
| Paper 数据探针 | `youmuchan-paper/target/youmuchan-paper-2.1.jar` |

### 推荐部署方式

```text
Velocity 代理服:
  plugins/ -> 放入 youmuchan-velocity-2.1.jar

每个需要上报事件的 Paper 子服:
  plugins/ -> 放入 youmuchan-paper-2.1.jar
```

如果你只想先体验基础能力，也可以先仅部署 Velocity 端；但要获取更完整的子服事件、热度与焦点信息，建议同时部署 Paper 探针。

---

## 配置文件

首次启动后会自动生成配置文件。

| 模块 | 配置路径 |
|---|---|
| Velocity 主插件 | `plugins/youmuchan/config.yml` |
| Paper 数据探针 | `plugins/YoumuChan/config.yml` |

### Velocity 基础 API 设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `api_key` | `your-api-key` | 模型 API Key |
| `api_url` | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | OpenAI 兼容格式的 API 地址（默认使用阿里云百炼） |
| `debug_mode` | `false` | 开启后在控制台打印完整 API 请求/响应 JSON |

### Velocity AI 模型设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `border_model` | `qwen3.5-flash` | 边界分析模型（轻量级，做信息过滤和情感分析） |
| `border_temperature` | `0.0` | 边界模型温度（0.0 = 最确定性输出） |
| `youmu_model` | `qwen3.5-plus` | 主对话模型（生成最终角色回复） |
| `youmu_temperature` | `0.7` | 主模型温度（越高越活泼多样） |

### Velocity 调度与缓存设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `time_window_ms` | `1800000`（30 分钟） | AI 构建上下文时追溯的聊天记录时间窗口 |
| `base_interval_ms` | `20000`（20 秒） | 基础调度间隔，实际间隔 = 基础值 / 热度倍率 |
| `cache_duration_ms` | `600000`（10 分钟） | 游戏事件在内存中的最大保留时间 |
| `cache_max_size` | `30` | 游戏事件缓存的最大条数 |
| `half_life_seconds` | `120` | 热度衰减半衰期（秒），越小降温越快 |

### Velocity Paper 通讯设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `tcp_listen_host` | `127.0.0.1` | TCP 监听地址，跨机器部署时可改为可访问地址 |
| `tcp_listen_port` | `55500` | TCP 监听端口，需与 Paper 端保持一致 |

### Velocity 游戏内显示

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `youmu_name` | `妖梦酱` | AI 在游戏内发言时显示的名称 |

### Paper 数据探针设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `velocity_tcp_host` | `127.0.0.1` | Velocity TCP 接收地址 |
| `velocity_tcp_port` | `55500` | Velocity TCP 监听端口 |
| `velocity_tcp_timeout_ms` | `3000` | TCP 连接超时时间（毫秒） |
| `is_default_server` | `false` | 是否为默认服务器 |
| `server_name` | `main` | 当前子服名称 |

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
| `/youmu mute` | 无（玩家可用） | 开关自己的 AI 发言显示状态（需 LuckPerms 已就绪） |
| `/youmu mute <玩家名或UUID>` | `youmu.muteplayer` | 开关指定玩家的 AI 发言显示状态 |
| `/youmu setmodel border <name>` | `youmuchan.setmodel` | 热更新边界分析模型 |
| `/youmu setmodel youmu <name>` | `youmuchan.setmodel` | 热更新主对话模型 |
| `/youmu setkey <api_key>` | `youmuchan.setkey` | 热更新 API Key |
| `/youmu seturl <api_url>` | `youmuchan.seturl` | 热更新 API URL |

说明：`youmu.visible` 是 LuckPerms 上用于控制“是否可见 AI 发言”的数据节点，不是命令权限节点。

---

## 常见运维操作

### 首次部署

```text
1. 构建项目并部署 Velocity / Paper 对应 jar
2. 启动 Velocity 与子服，自动生成配置
3. 在 Velocity 配置中填写 api_key
4. 如使用 Paper 探针，确认两端 TCP 地址与端口一致
5. 执行 /youmu reload
6. 插件默认处于 SLEEP 状态；有玩家在线且热度达标后会自动进入 DREAM
```

### 临时关停 AI 发言

```text
/youmu stop            <- 永久休眠，直到手动唤醒
/youmu stop 30m        <- 休眠 30 分钟后自动恢复
```

### 强制唤醒 AI

```text
/youmu start 1h        <- 强制 DREAM 状态 1 小时
```

### 切换模型（无需重启）

```text
/youmu setmodel youmu qwen-turbo
/youmu setmodel border qwen3.5-flash
```

### 对单个玩家隐藏 AI 发言

```text
/youmu mute
/youmu mute PlayerName
```

第一条用于玩家自己切换可见性；第二条用于管理员代为切换指定玩家的可见性，需要 LuckPerms 支持。

### 排查问题

```text
/youmu debug           <- 开启控制台详细日志
/youmu debug info      <- 在游戏内 BossBar 查看实时状态
```

BossBar 面板显示字段：`Heat`（热度）· `Fuel`（燃料值）· `Cache`（缓存条数）· `TgtPlayer`（关注目标）· `TgtScore`（目标分数）· `TgtTime`（锁定剩余）· `NextPulse`（下次推理倒计时）

### 常见排查点

| 现象 | 建议检查项 |
|---|---|
| AI 完全不说话 | `api_key` / `api_url` 是否正确，是否仍处于 SLEEP 状态 |
| Velocity 能启动但收不到子服事件 | `tcp_listen_host` / `tcp_listen_port` 与 Paper 端配置是否一致 |
| `/youmu mute` 无法使用 | 是否已安装 LuckPerms，相关权限节点是否已分配 |
| 惩罚事件没有进入上下文 | 是否已安装 LiteBans，桥接日志是否正常输出 |

---
