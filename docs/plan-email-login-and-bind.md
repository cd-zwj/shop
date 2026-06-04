# 开发计划：用户端邮箱登录 + 邮箱绑定

**分支**: `codex-dual-wallet-v1`
**日期**: 2026-06-02
**范围**: 用户端邮箱登录、用户端邮箱绑定。不做第三方 OAuth、不做短信。

---

## 现状确认

### 后端接口（已存在，无需改动）

| 接口 | Controller | DTO | 说明 |
|------|-----------|-----|------|
| `POST /v1/app/auth/email/send-login-code` | V1AppAuthController:86 | `PlatformEmailSendCodeDTO` (email, captchaKey, captchaCode) | 发送邮箱登录验证码，带 RateLimit |
| `POST /v1/app/auth/login/email` | V1AppAuthController:113 | `PlatformEmailLoginDTO` (email, emailCode, captchaKey, captchaCode) | 邮箱验证码登录，返回 token |
| `POST /v1/app/users/email/send-bind-code` | V1AppUserController:51 | `PlatformBindEmailSendCodeDTO` (email) | 发送绑定验证码，@SaCheckLogin |
| `POST /v1/app/users/email/bind` | V1AppUserController:68 | `PlatformBindEmailDTO` (email, emailCode) | 绑定邮箱，返回 PlatformUser，@SaCheckLogin |
| `GET /v1/app/users/me` | V1AppUserController:34 | 无 | 获取当前用户，PlatformUser 含 email 字段 |

### 前端现状

| 文件 | 现状 |
|------|------|
| `auth.ts` | 已有 `PlatformEmailSendCodeDTO`（找回密码用），缺 `PlatformEmailLoginDTO`、`PlatformBindEmailSendCodeDTO`、`PlatformBindEmailDTO` |
| `appAuth.ts` | 已有 `sendRecoverEmailCode`，缺 `sendLoginEmailCode`、`loginByEmail` |
| `appUser.ts` | 只有 `getCurrentUser()`，缺 `sendBindEmailCode`、`bindEmail` |
| `AuthContext.tsx` | `UserLoginMethod = 'password' \| 'sms' \| 'third-party'`，缺 `'email'`；`loginUser` 不支持邮箱登录 |
| `Login.tsx` | 用户端有密码/短信 tab，缺邮箱登录 tab 和表单 |
| `Profile.tsx` | "账号安全" path 为 `'#'`，`cursor-not-allowed opacity-50`，不可操作 |

---

## 一、用户端邮箱登录

### 1.1 补类型 `auth.ts`

新增一个类型：

```typescript
export interface PlatformEmailLoginDTO {
  email: string;
  emailCode: string;
  captchaKey: string;
  captchaCode: string;
}
```

`PlatformEmailSendCodeDTO` 已存在，直接复用，无需新增。

### 1.2 补 API `appAuth.ts`

```typescript
sendLoginEmailCode(payload: PlatformEmailSendCodeDTO) {
  return request<void>({
    url: '/v1/app/auth/email/send-login-code',
    method: 'post',
    data: payload,
    authRole: false,
  });
},

loginByEmail(payload: PlatformEmailLoginDTO) {
  return request<string>({
    url: '/v1/app/auth/login/email',
    method: 'post',
    data: payload,
    authRole: false,
  });
},
```

### 1.3 改 AuthContext.tsx

**UserLoginMethod 加 `'email'`**：

```typescript
type UserLoginMethod = 'password' | 'sms' | 'third-party' | 'email';
```

**loginUser 分支加 email**：

```typescript
async function loginUser(method: UserLoginMethod, payload: PlatformLoginDTO | PlatformEmailLoginDTO) {
  await activateRole('user');

  let token: string;
  if (method === 'password') {
    token = await appAuthService.loginByPassword(payload as PlatformLoginDTO);
  } else if (method === 'email') {
    token = await appAuthService.loginByEmail(payload as PlatformEmailLoginDTO);
  } else if (method === 'sms') {
    token = await appAuthService.loginBySms(payload as PlatformLoginDTO);
  } else {
    token = await appAuthService.loginByThirdParty(payload as PlatformLoginDTO);
  }

  setToken('user', token);
  const profile = await appUserService.getCurrentUser();
  setPlatformUserProfile(profile);
  setCurrentUser(profile);
  return profile;
}
```

**接口签名调整**：`loginUser` 的 payload 类型改为 `PlatformLoginDTO | PlatformEmailLoginDTO`，或用联合类型。在 `AuthContextValue` 接口处同步更新。

### 1.4 改 Login.tsx

**登录方式 tab 扩展**：

用户端目前有 `密码登录 | 短信登录`。改为 `密码登录 | 邮箱登录 | 短信登录`（短信 tab 保留入口但不接功能，点击后提示"暂未开放"或直接 disabled）。

更简方案：用户端只展示 `密码登录 | 邮箱登录` 两个 tab，短信 tab 暂时隐藏（等短信能力接入后再加）。管理端和商户端不受影响。

**邮箱登录表单**：

当 `loginMethod === 'email'` 时，表单区改为：
- 邮箱输入框（替代用户名输入框）
- 图形验证码（复用现有组件）
- "发送邮箱验证码" 按钮（调 `sendLoginEmailCode`，成功后显示提示，失败刷新验证码）
- 邮箱验证码输入框（替代密码输入框）
- 确认登录按钮（调 `loginUser('email', { email, emailCode, captchaKey, captchaCode })`）

**状态管理**：

新增 `email`、`emailCode`、`isSendingEmailCode` 三个 state。

`handleLogin` 里根据 `loginMethod` 构造不同的 payload：
- `password`：`{ username, password, captchaKey, captchaCode }`（不变）
- `email`：`{ email, emailCode, captchaKey, captchaCode }`

**错误处理**：

- 发送验证码失败：刷新图形验证码，显示错误
- 登录失败：刷新图形验证码，显示错误
- 与密码登录保持一致的 UX

---

## 二、用户端邮箱绑定

### 2.1 补类型 `auth.ts`

```typescript
export interface PlatformBindEmailSendCodeDTO {
  email: string;
}

export interface PlatformBindEmailDTO {
  email: string;
  emailCode: string;
}
```

### 2.2 补 API `appUser.ts`

```typescript
sendBindEmailCode(payload: PlatformBindEmailSendCodeDTO) {
  return request<void>({
    url: '/v1/app/users/email/send-bind-code',
    method: 'post',
    data: payload,
    authRole: 'user',
  });
},

bindEmail(payload: PlatformBindEmailDTO) {
  return request<PlatformUser>({
    url: '/v1/app/users/email/bind',
    method: 'post',
    data: payload,
    authRole: 'user',
  });
},
```

### 2.3 改 Profile.tsx

**"账号安全"入口激活**：

将 `path: '#'` 改为实际路由（如 `/profile/security`），或直接在 Profile 页内嵌入邮箱绑定面板（更轻量，推荐）。

**推荐方案：Profile 页内嵌邮箱绑定**：

在 Profile 的"账户设置"区域，点击"账号安全"展开一个内嵌面板（或 modal），包含：

- **已绑定状态**：显示当前邮箱，灰显，不可修改（本轮不做换绑）
- **未绑定状态**：
  - 邮箱输入框
  - "发送绑定验证码" 按钮（调 `sendBindEmailCode`，注意此接口需要登录态，不带图形验证码——后端 DTO 只要 email）
  - 邮箱验证码输入框
  - "确认绑定" 按钮（调 `bindEmail`）
  - 成功后调 `refreshCurrentUser()` 更新页面展示

**注意**：绑定验证码接口（`sendBindEmailCode`）的后端 DTO 只需要 `email`，不需要图形验证码。这与登录/找回密码的验证码不同。绑定接口有 `@SaCheckLogin` 保护，依赖登录态而非图形验证码防刷。

---

## 三、前后端 DTO 对照表

| 前端新增类型 | 后端 DTO | 字段 |
|-------------|---------|------|
| `PlatformEmailLoginDTO` | `PlatformEmailLoginDTO.java` | email, emailCode, captchaKey, captchaCode |
| `PlatformBindEmailSendCodeDTO` | `PlatformBindEmailSendCodeDTO.java` | email |
| `PlatformBindEmailDTO` | `PlatformBindEmailDTO.java` | email, emailCode |

`PlatformEmailSendCodeDTO` 已存在，前端复用。

---

## 四、文件改动清单

| 文件 | 改动内容 |
|------|---------|
| `salessystem/src/types/auth.ts` | 新增 `PlatformEmailLoginDTO`、`PlatformBindEmailSendCodeDTO`、`PlatformBindEmailDTO` |
| `salessystem/src/services/modules/appAuth.ts` | 新增 `sendLoginEmailCode`、`loginByEmail` |
| `salessystem/src/services/modules/appUser.ts` | 新增 `sendBindEmailCode`、`bindEmail` |
| `salessystem/src/context/AuthContext.tsx` | `UserLoginMethod` 加 `'email'`，`loginUser` 加 email 分支，签名调 |
| `salessystem/src/pages/Login.tsx` | 用户端 tab 加"邮箱登录"，新增邮箱登录表单和状态 |
| `salessystem/src/pages/Profile.tsx` | "账号安全"激活，嵌入邮箱绑定面板 |

---

## 五、安全与校验

- 前端校验邮箱格式（正则）、验证码非空、邮箱验证码非空
- 不在 console.log 输出验证码、邮箱验证码等敏感信息
- 发送验证码失败 / 登录失败后刷新图形验证码（邮箱登录场景）
- 绑定验证码不依赖图形验证码（后端设计如此，靠 @SaCheckLogin + RateLimit 保护）
- 成功绑定后清空输入框中的邮箱和验证码
- 后端 DTO 做最终校验（@NotBlank、@Email）

---

## 六、验收标准

### 邮箱登录
1. 登录页用户端可切换"密码登录"和"邮箱登录"
2. 邮箱登录：输入邮箱 + 图形验证码 → 发送邮箱验证码 → 输入邮箱验证码 → 登录成功跳首页
3. 发送验证码失败显示错误提示，图形验证码刷新
4. 登录失败显示错误提示，图形验证码刷新
5. 密码登录仍正常可用
6. `npm run lint` 通过，`npm run build` 通过

### 邮箱绑定
1. 个人中心"账号安全"可点击进入绑定面板
2. 未绑定邮箱：输入邮箱 → 发送验证码 → 输入验证码 → 绑定成功 → 页面显示新邮箱
3. 已绑定邮箱：显示当前邮箱，不可修改
4. 绑定成功后 `refreshCurrentUser()` 更新页面
5. `npm run lint` 通过，`npm run build` 通过
