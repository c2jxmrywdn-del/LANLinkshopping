<template>
  <main class="wrap">
    <a-card class="box">
      <template #title><span class="ll-wordmark ll-wordmark-on-light"><span class="ll-lan">LAN</span><span class="ll-link">Link</span><span class="ll-shopping">shopping</span></span></template>
      <a-tabs v-model:activeKey="tab" centered>
        <!-- ===== 登录 ===== -->
        <a-tab-pane key="login" tab="登录">
          <!-- 第一步：手机号 + 密码 -->
          <a-form v-if="!twofa" :model="form" layout="vertical" @finish="submit">
            <a-form-item label="手机号" :rules="[{ required: true, message: '请输入手机号' }]">
              <a-input v-model:value="form.phone" :placeholder="demoMode ? '演示账号手机号' : '请输入手机号'" size="large" />
            </a-form-item>
            <a-form-item label="密码" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model:value="form.password" :placeholder="demoMode ? '本地演示密码' : '请输入密码'" size="large" />
            </a-form-item>
            <a-button type="primary" html-type="submit" block size="large" :loading="loading">登录</a-button>
          </a-form>

          <!-- 第二步：两步验证动态码 -->
          <a-form v-else :model="twofa" layout="vertical" @finish="submit2fa">
            <a-alert type="info" show-icon class="twofa-tip">
              <template #message>该账号已开启两步验证，请输入验证器 App 上的动态验证码（绑嶻�q�^