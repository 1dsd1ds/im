# IM — 轻量级即时通讯系统

清爽、现代、高效的即时通讯系统，聚焦于"聊天"这一核心行为，提供实时消息收发、会话管理、好友体系和在线状态感知。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 后端 | Spring Boot 3.3 + Spring Security + JPA |
| 实时通信 | Netty WebSocket |
| 数据库 | MySQL + Redis |
| 认证 | JWT (jjwt) |
| API 文档 | SpringDoc OpenAPI (Swagger) |

## 项目结构

```
IM/
├── im-frontend/          # Vue 3 前端项目
│   └── src/
│       ├── api/           # API 请求模块 (auth, contact, group, message, user)
│       ├── components/    # 可复用组件 (ChatInput, ChatWindow, MessageBubble 等)
│       ├── router/        # Vue Router 路由配置
│       ├── stores/        # Pinia 状态管理 (auth, chat)
│       ├── utils/         # 工具函数 (axios 封装, WebSocket 客户端)
│       └── views/         # 页面视图 (auth, chat, contact, group, profile)
├── im-backend/           # Spring Boot 后端项目
│   └── src/main/java/cn/edu/zjut/im/
│       ├── config/        # 配置类 (Security, Netty, Redis, JWT)
│       ├── controller/    # REST 控制器 (Auth, Contact, Group, Message, User, File)
│       ├── entity/        # JPA 实体 (User, Contact, Group, GroupMember, Message)
│       ├── netty/         # WebSocket 服务端 (握手认证、编解码、消息路由)
│       ├── repository/    # 数据访问层
│       └── security/      # Spring Security 认证逻辑
├── DESIGN.md             # 设计系统文档
├── PRODUCT.md            # 产品定义文档
└── uploads/              # 文件上传目录
```

## 功能模块

- **用户认证** — 注册 / 登录 / JWT 令牌管理
- **好友体系** — 添加好友 / 好友列表 / 在线状态
- **一对一聊天** — 实时消息收发 / 消息状态 (已发送 / 已读)
- **群组聊天** — 创建群组 / 群聊消息
- **文件传输** — 图片 / 文件上传与预览
- **个人设置** — 个人资料编辑

## 本地开发

### 环境要求

- **JDK 21**
- **Maven** (或使用项目自带的 `mvnw`)
- **Node.js 18+**
- **MySQL 8.0+**
- **Redis 7.0+**

### 后端启动

```bash
cd im-backend

# 确保 MySQL 和 Redis 已运行，并修改 application.yml 中的连接信息

# 编译并启动
./mvnw spring-boot:run       # Linux / macOS
mvnw.cmd spring-boot:run     # Windows
```

后端运行在 `http://localhost:8080`，Swagger 文档位于 `http://localhost:8080/swagger-ui.html`。

### 前端启动

```bash
cd im-frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，API 请求通过 Vite proxy 转发到后端 8080 端口。

## 设计原则

1. **信息优先** — 消息内容是界面主角，chrome 退居其次
2. **状态可见** — 在线、离线、发送中、已读等状态即时更新
3. **操作零摩擦** — 高频操作无需思考即可完成
4. **安静不冰冷** — 克制但不等于无性格

详见 [DESIGN.md](./DESIGN.md) 和 [PRODUCT.md](./PRODUCT.md)。
