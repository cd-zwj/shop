# Code Review：用户端邮箱登录 + 邮箱绑定

**分支**: `codex-dual-wallet-v1`
**审查日期**: 2026-06-03
**审查范围**: `auth.ts`、`appAuth.ts`、`appUser.ts`、`AuthContext.tsx`、`Login.tsx`、`Profile.tsx`

---

## 总体评价

整体实现干净利落：类型与后端 DTO 完全一致，AuthContext 的扩展方式正确（联合类型 + as 断言），Login.tsx 的角色/tab 切换逻辑合理，Profile.tsx 的邮箱绑定面板 UI 结构清晰。发现几个需要修复的问题。

---

## HIGH（应该修复）

### 1. Profile.tsx 绑定邮箱缺少邮箱格式校验

**文件**: `salessystem/src/pages/Profile.tsx` 第 60–65 行

`handleSendBindEmailCode` 只检查邮箱非空，没有校验格式。后端有 `@Email` 注解会拒绝，但用户会收到一条后端风格的错误信息，体验不好。

```typescript
// 当前（第 60–65 行）
const normalizedEmail = bindEmail.trim();
if (!normalizedEmail) {
  setBindEmailSuccess('');
  setBindEmailError('请输入要绑定的邮箱');
  return;
}

// 建议：加正则校验
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const normalizedEmail = bindEmail.trim().toLowerCase();
if (!normalizedEmail || !emailPattern.test(normalizedEmail)) {
  setBindEmailSuccess('');
  setBindEmailError('请输入正确的邮箱地址');
  return;
}
```

`handleBindEmail`（第 82–88 行）同理，也应加格式校验。

---

## MEDIUM（建议修复）

### 2. Login.tsx `useEffect` 多了 `loginMethod` 依赖

**文件**: `salessystem/src/pages/Login.tsx` 第 50–62 行

```typescript
useEffect(() => {
  if (selectedRole === 'admin') {
    setLoginMethod('password');
  }
  if (selectedRole === 'user' && loginMethod === 'sms') {
    setLoginMethod('password');
  }
  if (selectedRole === 'merchant' && loginMethod === 'email') {
    setLoginMethod('password');
  }
  setError('');
  setSuccess('');
}, [loginMethod, selectedRole]);  // ← loginMethod in deps
```

这个 effect 的设计意图是"切换角色时修正非法 tab"，但 `loginMethod` 在依赖数组里导致**每次切换 tab 也触发**，执行 `setError(''); setSuccess('')`——正好把切换 tab 前的错误提示清掉了。

这在当前代码里不会出 bug（因为切换 tab 时本来也没设 error），但依赖语义不对。建议移除 `loginMethod`：

```typescript
useEffect(() => {
  if (selectedRole === 'admin') {
    setLoginMethod('password');
  }
  if (selectedRole === 'user' && loginMethod === 'sms') {
    setLoginMethod('password');
  }
  if (selectedRole === 'merchant' && loginMethod === 'email') {
    setLoginMethod('password');
  }
  setError('');
  setSuccess('');
}, [selectedRole]);  // ← 只在角色切换时触发
```

注意：移除后需要把 `loginMethod` 作为闭包变量读取，这在 React 里是安全的（effect 在当前渲染周期执行）。

### 3. Login.tsx `handleThirdPartyLogin` payload 结构与邮箱模式不兼容

**文件**: `salessystem/src/pages/Login.tsx` 第 150–184 行

`handleThirdPartyLogin` 始终构造 `{ username, password }` payload 并调 `loginUser('third-party', payload)`。在邮箱登录模式下，`username` 实际存的是邮箱、`password` 存的是验证码，这个 payload 结构语义上不对。

当前不会出运行时问题（因为第三方登录按钮和邮箱登录不会同时激活），但属于潜在隐患。建议在入口加 guard：

```typescript
async function handleThirdPartyLogin() {
  if (loginMethod === 'email') {
    return;  // 邮箱模式下第三方登录不可用
  }
  // ...现有逻辑
}
```

---

## LOW（可选改进）

### 4. Login.tsx 切换 tab 时旧输入未清空

从"密码登录"切到"邮箱登录"时，`username` 里残留的旧用户名仍在输入框里（placeholder 变了但值没清）。对用户来说，邮箱和用户名是不同的东西，切 tab 后清空 `username` 和 `password` 更符合预期。

### 5. Profile.tsx 绑定验证码输入框缺少回车提交

`bindEmailCode` 输入框没有 `onKeyDown` 处理回车，用户输完验证码后必须点按钮。

---

## 正确的部分

- [x] `PlatformEmailLoginDTO` 字段与后端 `PlatformEmailLoginDTO.java` 完全一致
- [x] `PlatformBindEmailSendCodeDTO` / `PlatformBindEmailDTO` 与后端 DTO 完全一致
- [x] `appAuth.ts` 两个新方法 URL、method、authRole 均正确
- [x] `appUser.ts` 两个新方法 URL、method、authRole（`'user'`）均正确，`bindEmail` 返回 `PlatformUser`
- [x] `AuthContext.tsx` 的 `UserLoginMethod` 加了 `'email'`，签名改为联合类型，分支逻辑正确
- [x] Login.tsx 角色守卫正确：admin 强制密码，user 不展示 sms，merchant 不展示 email
- [x] Login.tsx 邮箱登录 payload 正确映射：`email: username`，`emailCode: password`
- [x] `handleSendLoginEmailCode` 发送成功后 `setPassword('')` + 刷新验证码，失败也刷新验证码
- [x] Profile.tsx 绑定成功后调 `refreshCurrentUser()` 更新本地状态
- [x] Profile.tsx 已绑定和未绑定两种状态 UI 分离清晰，已绑定时只读展示
- [x] "账号安全"从 disabled 项改为独立 section，不再混在 menuGroups 里
- [x] 错误/成功提示互斥处理一致（先 setSuccess('') 再 setError，反之亦然）
- [x] 无 console.log 输出敏感信息

---

## 前后端 DTO 对照

| 前端类型 | 后端 DTO | 字段 | 状态 |
|---------|---------|------|------|
| `PlatformEmailLoginDTO` | `PlatformEmailLoginDTO.java` | email, emailCode, captchaKey, captchaCode | ✓ |
| `PlatformBindEmailSendCodeDTO` | `PlatformBindEmailSendCodeDTO.java` | email | ✓ |
| `PlatformBindEmailDTO` | `PlatformBindEmailDTO.java` | email, emailCode | ✓ |

---

## 修复优先级

1. **HIGH-1**: Profile.tsx 绑定邮箱加正则校验
2. **MEDIUM-2**: Login.tsx useEffect 移除 `loginMethod` 依赖
3. **MEDIUM-3**: `handleThirdPartyLogin` 加邮箱模式 guard
4. 其余按需处理
