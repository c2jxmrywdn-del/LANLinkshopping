// 轻量 i18n：locale ref + t('a.b') 取值，取不到回退中文再回退 key
import { ref } from 'vue'

export const locale = ref('zh-CN')

const zh = {
  acct: {
    title: '账号中心',
    nav: { profile: '个人信息', settings: '系统设置' },
    card: {
      role: '角色', phone: '手机', email: '邮箱', unbound: '未绑定',
      quick: '快捷入口', orders: '我的订单', cart: '购物车', merchant: '商户入驻'
    },
    loadFail: '数据加载失败',
    loadFailDesc: '请检查网络后重试',
    retry: '重试',
    discardTitle: '放弃未保存的修改？',
    discardContent: '当前编辑内容尚未保存，切换后修改将丢失。',
    leaveConfirm: '当前页面有未保存的修改，确定离开吗？',
    ok: '确定', cancelText: '取消',
    profile: {
      title: '个人信息',
      edit: '编辑', save: '保存', cancel: '取消',
      changeAvatar: '更换头像',
      realName: '姓名', nickname: '昵称', gender: '性别', birthday: '出生日期',
      bio: '个人简介', phone: '手机号', email: '邮箱',
      male: '男', female: '女', secret: '保密',
      notSet: '未设置',
      bioPlaceholder: '介绍一下自己吧（最多 500 字）',
      bioHelp: '剩余可输入 {n} 字',
      saveSuccess: '个人信息已保存',
      draftTitle: '恢复草稿',
      draftContent: '检测到上次未保存的修改，是否恢复？',
      restore: '恢复', discardDraft: '放弃',
      ruleRealNameRequired: '请输入姓名',
      ruleNicknameRequired: '请输入昵称',
      ruleTooLong: '长度不能超过 32 字',
      change: '更换',
      changePhone: '更换手机号',
      changeEmail: '更换邮箱',
      newPhone: '新手机号',
      newEmail: '新邮箱',
      sendCode: '获取验证码',
      resendIn: '{s}s 后重发',
      code: '验证码',
      codePlaceholder: '6 位验证码',
      devCode: '演示验证码：{code}',
      submit: '提交',
      bindSuccess: '绑定信息已更新',
      invalidPhone: '手机号格式不正确',
      invalidEmail: '邮箱格式不正确',
      codeRequired: '请输入 6 位验证码'
    },
    avatar: {
      title: '编辑头像',
      dropHint: '点击或拖拽图片到此处上传',
      typeHint: '支持 JPG/PNG/WebP，≤5MB',
      invalidType: '仅支持 JPG/PNG/WebP 图片',
      invalidSize: '图片大小不能超过 5MB',
      zoom: '缩放',
      rotate: '旋转90°',
      reselect: '重新选择',
      upload: '上传',
      uploadFail: '上传失败，请重试',
      retry: '重试',
      done: '头像已更新'
    },
    settings: {
      security: '账户安全',
      changePwd: '修改密码',
      oldPwd: '原密码', newPwd: '新密码', confirmPwd: '确认新密码',
      ruleOldRequired: '请输入原密码',
      ruleNewRequired: '请输入新密码',
      ruleNewPattern: '8-20 位，需包含大小写字母、数字和特殊字符',
      ruleNewSameAsOld: '新密码不能与原密码相同',
      ruleConfirmRequired: '请再次输入新密码',
      ruleConfirmMismatch: '两次输入的密码不一致',
      strength: '密码强度',
      pwdConfirmTitle: '确认修改密码？',
      pwdConfirmContent: '修改密码后，下次登录需使用新密码。',
      pwdSuccess: '密码修改成功',
      submit: '提交',
      twofa: '两步验证（TOTP）',
      twofaOn: '已启用',
      twofaOff: '未启用',
      twofaDesc: '启用后登录需额外输入动态验证码，提升账号安全性。',
      firstTime: '首次验证时间',
      enable2fa: '启用两步验证',
      disable2fa: '关闭两步验证',
      twofaSetupTitle: '设置两步验证',
      twofaStart: '开始设置',
      twofaScan: '使用验证器 App（如 Google Authenticator）扫描二维码',
      twofaSecret: '无法扫码？手动输入密钥：',
      copy: '复制', copied: '已复制',
      twofaCodeLabel: '动态验证码',
      twofaVerifyEnable: '验证并启用',
      twofaEnabled: '两步验证已启用',
      twofaDisableTitle: '关闭两步验证',
      twofaDisableTip: '关闭后账号安全性将降低，请输入当前动态验证码确认。',
      twofaConfirmDisable: '确认关闭',
      twofaDisabled: '两步验证已关闭',
      codeRequired: '请输入 6 位验证码',
      notify: '通知偏好',
      notifyEmail: '邮件通知',
      notifyPush: '站内推送',
      notifySms: '短信提醒',
      notifyGroup: '分类通知',
      notifyOrder: '订单动态',
      notifyPromotion: '优惠活动',
      notifySystem: '系统公告',
      notifyAll: '全部开启 / 全部关闭',
      saved: '设置已保存',
      saveFail: '保存失败，已恢复原设置',
      appearance: '界面个性化',
      theme: '主题模式',
      themeLight: '亮色', themeDark: '暗色', themeSystem: '跟随系统',
      language: '语言',
      fontSize: '字体大小',
      fontStandard: '标准', fontLarge: '放大 120%', fontSmall: '缩小 80%',
      privacy: '隐私权限',
      recommend: '个性化推荐',
      recommendTip: '开启后将根据你的浏览与购买偏好推荐商品（对应隐私政策第 3 条）。',
      ads: '广告推送',
      adsTip: '开启后允许平台向你推送营销广告信息（对应隐私政策第 5 条）。',
      thirdAuth: '第三方授权管理',
      thirdEmpty: '暂无第三方应用授权',
      revoke: '撤回授权',
      revokeTitle: '撤回授权？',
      revokeContent: '撤回后，该应用将无法再访问你的账号信息。',
      revoked: '授权已撤回',
      scopes: '授权范围',
      authTime: '授权时间',
      reset: '恢复默认设置',
      resetTitle: '恢复默认设置？',
      resetContent: '仅重置系统设置（通知、外观、隐私），不影响个人信息。',
      resetDone: '已恢复默认设置'
    }
  }
}

const en = {
  acct: {
    title: 'Account Center',
    nav: { profile: 'Profile', settings: 'Settings' },
    card: {
      role: 'Role', phone: 'Phone', email: 'Email', unbound: 'Not bound',
      quick: 'Quick Links', orders: 'My Orders', cart: 'Cart', merchant: 'Become a Merchant'
    },
    loadFail: 'Failed to load',
    loadFailDesc: 'Check your network and retry',
    retry: 'Retry',
    discardTitle: 'Discard unsaved changes?',
    discardContent: 'Your edits have not been saved and will be lost after switching.',
    leaveConfirm: 'You have unsaved changes. Leave anyway?',
    ok: 'OK', cancelText: 'Cancel',
    profile: {
      title: 'Profile',
      edit: 'Edit', save: 'Save', cancel: 'Cancel',
      changeAvatar: 'Change Avatar',
      realName: 'Name', nickname: 'Nickname', gender: 'Gender', birthday: 'Birthday',
      bio: 'Bio', phone: 'Phone', email: 'Email',
      male: 'Male', female: 'Female', secret: 'Secret',
      notSet: 'Not set',
      bioPlaceholder: 'Introduce yourself (up to 500 chars)',
      bioHelp: '{n} characters remaining',
      saveSuccess: 'Profile saved',
      draftTitle: 'Restore Draft',
      draftContent: 'Unsaved changes from last time detected. Restore them?',
      restore: 'Restore', discardDraft: 'Discard',
      ruleRealNameRequired: 'Please enter your name',
      ruleNicknameRequired: 'Please enter a nickname',
      ruleTooLong: 'Must be within 32 characters',
      change: 'Change',
      changePhone: 'Change Phone',
      changeEmail: 'Change Email',
      newPhone: 'New Phone',
      newEmail: 'New Email',
      sendCode: 'Send Code',
      resendIn: 'Resend in {s}s',
      code: 'Code',
      codePlaceholder: '6-digit code',
      devCode: 'Demo code: {code}',
      submit: 'Submit',
      bindSuccess: 'Binding updated',
      invalidPhone: 'Invalid phone number',
      invalidEmail: 'Invalid email address',
      codeRequired: 'Please enter the 6-digit code'
    },
    avatar: {
      title: 'Edit Avatar',
      dropHint: 'Click or drag an image here',
      typeHint: 'JPG/PNG/WebP supported, ≤5MB',
      invalidType: 'Only JPG/PNG/WebP images are allowed',
      invalidSize: 'Image must be within 5MB',
      zoom: 'Zoom',
      rotate: 'Rotate 90°',
      reselect: 'Reselect',
      upload: 'Upload',
      uploadFail: 'Upload failed, please retry',
      retry: 'Retry',
      done: 'Avatar updated'
    },
    settings: {
      security: 'Account Security',
      changePwd: 'Change Password',
      oldPwd: 'Current Password', newPwd: 'New Password', confirmPwd: 'Confirm Password',
      ruleOldRequired: 'Please enter current password',
      ruleNewRequired: 'Please enter new password',
      ruleNewPattern: '8-20 chars incl. upper/lowercase letters, digits and symbols',
      ruleNewSameAsOld: 'New password must differ from the current one',
      ruleConfirmRequired: 'Please confirm new password',
      ruleConfirmMismatch: 'Passwords do not match',
      strength: 'Strength',
      pwdConfirmTitle: 'Change password?',
      pwdConfirmContent: 'You will need the new password at next login.',
      pwdSuccess: 'Password changed',
      submit: 'Submit',
      twofa: 'Two-Factor Auth (TOTP)',
      twofaOn: 'Enabled',
      twofaOff: 'Disabled',
      twofaDesc: 'Requires an extra dynamic code at login for better security.',
      firstTime: 'First verified at',
      enable2fa: 'Enable 2FA',
      disable2fa: 'Disable 2FA',
      twofaSetupTitle: 'Set up 2FA',
      twofaStart: 'Start Setup',
      twofaScan: 'Scan the QR code with an authenticator app',
      twofaSecret: 'Cannot scan? Enter the key manually:',
      copy: 'Copy', copied: 'Copied',
      twofaCodeLabel: 'Dynamic Code',
      twofaVerifyEnable: 'Verify & Enable',
      twofaEnabled: '2FA enabled',
      twofaDisableTitle: 'Disable 2FA',
      twofaDisableTip: 'Security will be reduced. Enter the current dynamic code to confirm.',
      twofaConfirmDisable: 'Confirm Disable',
      twofaDisabled: '2FA disabled',
      codeRequired: 'Please enter the 6-digit code',
      notify: 'Notifications',
      notifyEmail: 'Email',
      notifyPush: 'In-app Push',
      notifySms: 'SMS',
      notifyGroup: 'Categories',
      notifyOrder: 'Order Updates',
      notifyPromotion: 'Promotions',
      notifySystem: 'System Announcements',
      notifyAll: 'All On / All Off',
      saved: 'Settings saved',
      saveFail: 'Save failed, reverted',
      appearance: 'Appearance',
      theme: 'Theme',
      themeLight: 'Light', themeDark: 'Dark', themeSystem: 'System',
      language: 'Language',
      fontSize: 'Font Size',
      fontStandard: 'Standard', fontLarge: 'Large 120%', fontSmall: 'Small 80%',
      privacy: 'Privacy',
      recommend: 'Personalized Recommendations',
      recommendTip: 'Recommend products based on browsing and purchase history (Privacy Policy §3).',
      ads: 'Ad Push',
      adsTip: 'Allow marketing messages from the platform (Privacy Policy §5).',
      thirdAuth: 'Third-party Authorizations',
      thirdEmpty: 'No third-party authorizations',
      revoke: 'Revoke',
      revokeTitle: 'Revoke authorization?',
      revokeContent: 'The app will no longer access your account info.',
      revoked: 'Authorization revoked',
      scopes: 'Scopes',
      authTime: 'Authorized at',
      reset: 'Reset to Defaults',
      resetTitle: 'Reset settings?',
      resetContent: 'Only system settings (notifications, appearance, privacy) are reset; your profile is not affected.',
      resetDone: 'Settings reset to defaults'
    }
  }
}

const dicts = { 'zh-CN': zh, 'en-US': en }

function pick(dict, path) {
  let cur = dict
  for (const k of path.split('.')) {
    if (cur == null || typeof cur !== 'object') return undefined
    cur = cur[k]
  }
  return cur
}

/**
 * 取文案，支持 {a.b} 路径与 {name} 插值
 * t('acct.profile.bioHelp', { n: 12 })
 */
export function t(key, params) {
  let s = pick(dicts[locale.value], key)
  if (s === undefined) s = pick(zh, key) // 回退中文
  if (s === undefined) return key // 再回退 key 本身
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      s = s.replaceAll(`{${k}}`, String(v))
    }
  }
  return s
}

export function setLocale(l) {
  locale.value = dicts[l] ? l : 'zh-CN'
}
