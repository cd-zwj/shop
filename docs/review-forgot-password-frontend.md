# 忘记密码前端实现 Code Review

**分支**: `codex-dual-wallet-v1`
**审查日期**: 2026-06-02
**审查范围**: `auth.ts` 类型、`appAuth.ts` API 封装、`ForgotPassword.tsx` 页面、`App.tsx` 路由、`Login.tsx` 入口

---

## 总体评价

整体实现质量不错：类型与后端 DTO/VO 完全一致，3 步流程清晰，API 封装规范，路由和 UI 入口都已正确接入。下面按严重程度列出需要关注的问题。

---

## HIGH（应该修复）

### 1. Login.tsx：重置成功后登录出错，成功和错误提示同时显示

**文件**: `salessystem/src/pages/Login.tsx` 约第 113 行

`handleLogin` 的 `catch` 块只清了 `error`，没有清 `success`。当用户从找回密码页带着 `resetPasswordSuccess` 回来，`success` 被设置；如果随后登录失败，`error` 也被设置，两个提示框同时出现。

```typescript
// 当前
} catch (err) {
  setError(err instanceof ApiError ? err.message : '登录失败，请稍后重试');
  void loadCaptcha();

// 应该加上
  setSuccess('');  // ← 清除之前的重置成功提示
}
```

### 2. ForgotPassword.tsx：密码 `.trim()` 可能截断合法密码

**文件**: `salessystem/src/pages/ForgotPassword.tsx` 约第 148 行

`newPassword.trim()` 作为请求参数发送。如果用户有意在密码首尾加空格（合法密码字符），trim 后实际设置的密码与用户输入不一致，且用户无法察觉。

```typescript
// 当前
newPassword: newPassword.trim(),

// 建议：保持原样发送，仅做空值校验
newPassword: newPassword,
```

### 3. 需确认 `ApiError` 类是否支持 `instanceof` 检查

**文件**: `salessystem/src/pages/ForgotPassword.tsx` 第 7 行

`err instanceof ApiError` 依赖 `ApiError` 是一个真正的 `class`（而非 TypeScript `interface` 或 `type`）。如果 `ApiError` 是在 `request` 拦截器中手动 `throw new Error(...)` 而非 `throw new ApiError(...)`，这个判断永远为 `false`，所有错误都会走 fallback 消息。

**行动项**: 确认 `../types/api` 中 `ApiError` 的导出方式，确保拦截器里抛出的是 `new ApiError(...)` 实例。

---

## MEDIUM（建议修复）

### 4. 没有密码长度/强度最低校验

**文件**: `salessystem/src/pages/ForgotPassword.tsx` 约第 134 行

只校验了"非空"和"两次一致"，没有最小长度检查。用户可以设一个长度为 1 的密码。

```typescript
// 建议在 handleResetPassword 中加
if (newPassword.trim().length < 6) {
  setError('密码长度不能少于6位');
  return;
}
```

### 5. step 切换时 error 没有自动清除

**文件**: `salessystem/src/pages/ForgotPassword.tsx` `handleRecoverAccount` 方法

`handleRecoverAccount` 失败后 `error` 被设置，随后重试成功时 try 块里只设了 `success`，没有 `setError('')`，可能导致上一次的错误提示残留。

```typescript
// handleRecoverAccount 约第 109 行
try {
  const account = await appAuthService.recoverAccount({...});
  setRecoveredUsername(account.username);
  setSuccess('账号校验通过，请设置新密码');
+ setError('');  // ← 清除之前可能残留的错误
  setStep('reset');
}
```

同理 `handleResetPassword` 也建议在 try 块里加 `setError('')`。

### 6. 没有"返回上一步"的能力

用户在 step 2（输入邮箱验证码）时，如果发现邮箱填错了，只能点"返回登录"重新开始。可以在 step 2/3 加一个"重新填写邮箱"链接，将 step 重置为 `'email'`，同时清空 `emailCode` 和 `email`。

---

## LOW（可选改进）

### 7. 图形验证码输入框没有回车提交

**文件**: `salessystem/src/pages/ForgotPassword.tsx` 约第 246 行

图形验证码输入框没有 `onKeyDown` 处理回车键。用户输完验证码后习惯按回车，但当前必须点按钮。Login.tsx 也有同样问题（预存问题）。

### 8. `captchaCode` 自动转大写但后端可能区分大小写

**文件**: `salessystem/src/pages/ForgotPassword.tsx` 第 248 行

```typescript
setCaptchaCode(event.target.value.toUpperCase())
```

如果后端验证码是小写的，前端发大写过去会校验失败。需要确认后端 `AuthCaptchaService.validateCaptcha` 是否忽略大小写。（Login.tsx 也有同样写法，属于预存问题。）

---

## 正确的部分

- [x] 4 个 TypeScript 类型与后端 Java DTO/VO 字段完全一致
- [x] 3 个 API 方法的 URL、HTTP method、payload shape 与后端 controller 一一对应
- [x] `/forgot-password` 路由已注册，且 TopNav、BottomNav、AppContent 的 `isAuthPage` 判断都已包含
- [x] Login.tsx 的 `resetPasswordSuccess` state 读取和清除逻辑正确
- [x] 成功重置后清空了敏感字段（`emailCode`、`newPassword`、`confirmPassword`）
- [x] 图形验证码在成功和失败后都会刷新
- [x] 没有在 console.log 输出任何敏感信息

---

## 前后端类型对照

| 前端类型 | 后端 DTO | 字段一致 |
|---------|---------|---------|
| `PlatformEmailSendCodeDTO` | `PlatformEmailSendCodeDTO.java` | email, captchaKey, captchaCode |
| `PlatformRecoverAccountDTO` | `PlatformRecoverAccountDTO.java` | email, emailCode |
| `PlatformResetPasswordDTO` | `PlatformResetPasswordDTO.java` | email, emailCode, newPassword |
| `RecoveredPlatformAccountVO` | `RecoveredPlatformAccountVO.java` | username |

---

## 优先级修复建议

1. **HIGH-1**: Login.tsx 成功/错误提示冲突 — 加一行 `setSuccess('')`
2. **HIGH-2**: trim 密码问题 — 移除 `newPassword.trim()`
3. **HIGH-3**: 确认 `ApiError` instanceof 行为
4. **MEDIUM-4**: 补密码长度校验
5. **MEDIUM-5**: step 切换时清除残留 error
6. 其余按优先级逐步处理
