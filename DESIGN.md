---
name: IM 即时通讯
description: 清爽、现代、高效的即时通讯系统，界面退后让对话成为主角。
colors:
  sky-ink:
    value: oklch(52% 0.12 265)
    role: primary
  surface:
    value: oklch(100% 0 0)
    role: neutral-bg
  page:
    value: oklch(97% 0.002 265)
    role: neutral-bg-alt
  chat-area:
    value: oklch(95% 0.003 265)
    role: neutral-bg-subtle
  border:
    value: oklch(90% 0.005 265)
    role: neutral-border
  border-light:
    value: oklch(94% 0.003 265)
    role: neutral-border-subtle
  text-primary:
    value: oklch(18% 0.01 265)
    role: neutral-text
  text-secondary:
    value: oklch(48% 0.01 265)
    role: neutral-text-secondary
  text-tertiary:
    value: oklch(65% 0.008 265)
    role: neutral-text-tertiary
  bubble-self:
    value: oklch(92% 0.005 265)
    role: surface-accent
  online:
    value: oklch(62% 0.16 145)
    role: semantic-success
  error:
    value: oklch(55% 0.18 20)
    role: semantic-error
typography:
  title:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 1.4
  body:
    fontFamily: "'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.6
  caption:
    fontFamily: "'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0.02em
rounded:
  sm: 6px
  md: 10px
  lg: 16px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 20px
  2xl: 24px
  3xl: 32px
components:
  button-primary:
    backgroundColor: "{colors.sky-ink}"
    textColor: oklch(100% 0 0)
    rounded: "{rounded.sm}"
    padding: 10px 20px
  button-primary-hover:
    backgroundColor: oklch(44% 0.12 265)
  input-default:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.sm}"
    padding: 8px 12px
  bubble-self:
    backgroundColor: "{colors.bubble-self}"
    rounded: "{rounded.md}"
  bubble-other:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.md}"
---

# Design System: IM 即时通讯

## 1. Overview

**Creative North Star: "The Open Window"**

这个设计系统追求通透与呼吸感。像推开一扇窗——界面元素退到背景中，让消息内容自然流动。没有厚重的容器，没有喧宾夺主的装饰，每一像素都为信息的清晰传递服务。

它服务于 PRODUCT.md 定义的"清爽、现代、高效"人格。克制但不冰冷：中性的底色上有一抹克制的 accent，微妙的灰度层次传递温度。色彩策略为 Restrained——主色调覆盖面积不超过任何屏幕的 10%，它的稀缺性即是它的力量。

这个系统明确拒绝：渐变色大背景、微信绿气泡、玻璃态模糊效果、侧边条纹边框、以及任何"看起来很 AI"的装饰性设计。静态区域保持平面，仅悬浮元素使用极轻阴影。

**Key Characteristics:**
- 通透的层级：通过底色微差区分功能区，不依赖边框和阴影
- 信息即主角：消息气泡用中性色，仅靠位置（左/右）区分己/彼
- 克制的 accent：单一冷调蓝靛，只出现在链接、选中态、已读确认
- 微妙不等于无趣：浅蓝底调的中性色让"白"不再空洞

## 2. Colors

调色板围绕一个冷调蓝靛 accent 构建，所有中性色都向同一色相微偏，使整个界面在冷暖上保持一致。

### Primary
- **Sky Ink** (oklch(52% 0.12 265), ~#5b6eb8): 唯一的 accent 色。用于链接、选中态背景、已读状态标记、主按钮、焦点环。覆盖面积极度克制——任一屏幕不超过 10%。

### Neutral
- **Surface** (oklch(100% 0 0), ~#ffffff): 面板、卡片、他人气泡的纯白背景。
- **Page** (oklch(97% 0.002 265), ~#f7f7f9): 页面底色。几乎白色但带着一丝蓝，避免刺眼的纯白。
- **Chat Area** (oklch(95% 0.003 265), ~#f0f1f4): 聊天区域背景，与白色面板形成微妙分层。
- **Bubble Self** (oklch(92% 0.005 265), ~#e8e9f0): 自己发送的气泡背景，浅灰蓝调。仅靠位置区分己/彼，不引入第二个 accent。
- **Border** (oklch(90% 0.005 265), ~#e2e3e9): 分割线、输入框边框。
- **Border Light** (oklch(94% 0.003 265), ~#eeeef2): 更轻的分割线，用于列表项之间的次级分割。
- **Text Primary** (oklch(18% 0.01 265), ~#1c1d24): 正文、标题。接近黑但向蓝微偏。
- **Text Secondary** (oklch(48% 0.01 265), ~#6b6d7a): 辅助文字、时间戳、提示。
- **Text Tertiary** (oklch(65% 0.008 265), ~#9698a3): placeholder、禁用态文字。

### Semantic
- **Online** (oklch(62% 0.16 145), ~#52b788): 在线状态点、在线文字。柔和的绿，不刺眼。
- **Error** (oklch(55% 0.18 20), ~#e0556a): 发送失败、错误提示。柔和的暖红。

### Named Rules
**The 10% Rule.** Sky Ink 在任何屏幕上的覆盖面积不超过 10%。它出现在链接、选中态、焦点环、已读标记——永远作为点缀，不作为底色。

**The No-Gradient Rule.** 禁止使用 CSS 渐变作为背景。登录页的紫色渐变 (`#667eea → #764ba2`) 替换为纯 Page 底色。色彩由 accent 的点状出现提供，不由大面积渐变提供。

## 3. Typography

**Display Font:** system-ui, -apple-system, 'Segoe UI', sans-serif（标题）
**Body Font:** 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif（正文、标签）
**Mono Font:** ui-monospace, 'Cascadia Code', Consolas, monospace（代码/技术信息）

**Character:** 中文字体优先使用 PingFang SC（macOS）和 Microsoft YaHei（Windows），确保跨平台清晰度。西文回退到 Helvetica Neue / system-ui。字体栈不引入 Web Font，零额外加载。

### Hierarchy
- **Title** (600, 18px, 1.4): 聊天标题、面板标题。仅在需要层级区分时使用。
- **Body** (400, 14px, 1.6): 消息正文、列表项。最大行宽 65ch。
- **Caption** (400, 12px, 1.5): 时间戳、状态文字、辅助说明。
- **Label** (500, 11px, 1.4, 0.02em letter-spacing): 消息状态标签（已读/已发送/发送中）。

### Named Rules
**The System Font Rule.** 不引入 Web Font。PingFang SC / Microsoft YaHei 已是各自平台最优中文阅读字体。零额外加载保持界面轻快。

## 4. Elevation

此系统以"平面分层"为基础：不同功能区通过底色微差（Surface / Page / Chat Area）区分层级，不依赖阴影。阴影仅在悬浮元素（下拉菜单、弹窗、tooltip）出现，且极度克制——模糊半径大、透明度低，目的是暗示浮层而非制造戏剧性。

### Shadow Vocabulary
- **Panel Float** (`0 4px 16px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)`): 下拉菜单、弹窗、悬浮卡片。用于与底层内容分离的元素。
- **Resting State** (`none`): 所有静态表面——面板、卡片、输入框、气泡。不携带阴影。

### Named Rules
**The Flat-By-Default Rule.** 静态元素不使用阴影。仅当元素需要"浮起"（z-index 提升、用户交互触发）时才引入 Panel Float 阴影。

## 5. Components

### Buttons
- **Shape:** 6px 圆角（{rounded.sm}），与输入框保持一致。
- **Primary:** Sky Ink 背景，白色文字，padding 10px 20px。Hover 时加深至 oklch(44% 0.12 265)。无阴影。
- **Focus:** 2px Sky Ink 外环（`outline: 2px solid {colors.sky-ink}; outline-offset: 2px`），无 box-shadow 方案。
- **Default/Secondary:** 白色背景，Border 描边，Text Primary 文字。Hover 时背景变为 Page。
- **Ghost:** 透明背景，Text Secondary 文字。Hover 时背景变为 Page。

### Message Bubbles
- **Shape:** 10px 圆角（{rounded.md}）。
- **Self:** Bubble Self 底色，右对齐。无额外边框。
- **Other:** Surface 底色，左对齐。无额外边框。
- **Status label:** 11px Label 字体，位于自己气泡下方右对齐。已读状态使用 Sky Ink 色。

### Conversation Items
- **Default:** 透明背景，左侧 Surface 面板内。hover 时背景转为 Page。
- **Active:** Sky Ink 10% 透明度背景（~oklch(52% 0.12 265 / 0.1)），左侧 3px Sky Ink 指示条（border-radius: 0 3px 3px 0）。
- **Internal padding:** 12px 16px（{spacing.md} {spacing.lg}）。
- **Avatar:** 44px 圆形，右侧 12px gap。

### Inputs / Fields
- **Style:** 6px 圆角（{rounded.sm}），1px Border 描边，Surface 背景。
- **Focus:** Border 转为 Sky Ink，无发光扩散。简洁的状态转换。
- **Placeholder:** Text Tertiary 色。
- **Textarea:** 同 input，resize: none。

### Status Indicators
- **Online Dot:** 8px 圆形，Online 色填充。配合文字标签使用。
- **Offline Dot:** 8px 圆形，Border 色填充。
- **Connection Dot:** 10px 圆形，Online 色（已连接）或 Error 色（未连接）。

### Navigation
- **Top bar:** 左侧面板顶部，Surface 背景，底部 Border Light 分割线。用户头像 + 昵称 + 下拉箭头为一组；连接状态点 + 未读徽标 + 添加按钮为另一组。

## 6. Do's and Don'ts

### Do:
- **Do** 使用底色微差（Surface / Page / Chat Area）区分功能区域，不依赖边框。
- **Do** 将 Sky Ink 严格用于 ≤10% 的界面面积——链接、选中态、焦点环、已读标记。
- **Do** 自己和他人的消息气泡使用中性色，仅靠左右位置区分。
- **Do** 状态信息同时使用颜色和文字标签（如"在线"+"绿点"），不只依赖颜色。
- **Do** 保持消息气泡圆角为 10px，正文 14px/1.6 行高。

### Don't:
- **Don't** 使用 CSS 渐变作为背景。登录页的紫色渐变必须移除。
- **Don't** 使用微信绿 (#95ec69) 或任何高饱和色作为气泡背景。
- **Don't** 对静态表面使用阴影。面板和卡片 resting 状态保持平面。
- **Don't** 使用 `border-left` > 1px 作为彩色装饰条（选中态使用 3px 半圆角指示条，不走此禁令）。
- **Don't** 引入第二个 accent 色。Sky Ink 是唯一的品牌色。语义色（Online 绿、Error 红）不计入 accent——它们功能明确且使用量极小。
- **Don't** 使用 emoji 作为界面装饰，不使用渐变文字（`background-clip: text`）。
