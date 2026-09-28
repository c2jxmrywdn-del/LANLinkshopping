<template>
  <transition name="ss-leave">
    <div class="ss-overlay" v-show="visible">
      <!-- 背景光晕 -->
      <div class="ss-bg"></div>
      <div class="ss-glow"></div>

      <div class="ss-content">
        <!-- 动态 Logo 图形 -->
        <svg class="ss-mark" viewBox="0 0 200 130" width="200" height="130" aria-hidden="true">
          <!-- 左：汇聚点 -->
          <g class="ss-dots-l">
            <circle class="ss-dot ss-cv" style="animation-delay:.55s" cx="30" cy="42" r="5"/>
            <circle class="ss-dot ss-cv" style="animation-delay:.68s" cx="24" cy="65" r="6"/>
            <circle class="ss-dot ss-cv" style="animation-delay:.81s" cx="30" cy="88" r="5"/>
          </g>
          <g class="ss-flows">
            <path class="ss-flow" style="animation-delay:.5s" d="M30 42 C40 46 40 57 46 60"/>
            <path class="ss-flow" style="animation-delay:.63s" d="M24 65 C36 65 40 65 46 65"/>
            <path class="ss-flow" style="animation-delay:.76s" d="M30 88 C40 84 40 73 46 70"/>
          </g>

          <!-- 核心双环（描线生长） -->
          <circle class="ss-ring ss-ringA" pathLength="1" cx="70" cy="65" r="24"/>
          <circle class="ss-ring ss-ringB" pathLength="1" cx="98" cy="65" r="24"/>
          <circle class="ss-core" cx="84" cy="65" r="6.5"/>

          <!-- 右：迸发火花 -->
          <g class="ss-flows">
            <path class="ss-flow ss-flowR" style="animation-delay:1.3s" d="M122 59 C128 55 132 53 138 49"/>
            <path class="ss-flow ss-flowR" style="animation-delay:1.43s" d="M124 65 C132 65 138 65 144 65"/>
            <path class="ss-flow ss-flowR" style="animation-delay:1.56s" d="M122 71 C128 75 132 77 138 81"/>
          </g>
          <g class="ss-dots-r">
            <circle class="ss-dot ss-sp" style="animation-delay:1.35s" cx="140" cy="49" r="5"/>
            <circle class="ss-dot ss-sp" style="animation-delay:1.48s" cx="146" cy="65" r="6"/>
            <circle class="ss-dot ss-sp" style="animation-delay:1.61s" cx="140" cy="81" r="5"/>
          </g>
        </svg>

        <!-- 字标 -->
        <div class="ss-wordmark">LAN<b>Link</b>shopping</div>
        <div class="ss-tagline">聚合 · 连接 · 一体多元解决方案</div>

        <!-- 进度条 -->
        <div class="ss-progress"><span class="ss-progress-bar"></span></div>
      </div>

      <button class="ss-skip" @click="finish">跳过 ›</button>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
const emit = defineEmits(['done'])
const visible = ref(true)
let timer = null
function finish() {
  if (!visible.value) return
  visible.value = false
  setTimeout(() => emit('done'), 650)
}
onMounted(() => { timer = setTimeout(finish, 5000) })
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.ss-overlay {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}
.ss-leave-leave-active { transition: opacity .6s ease, transform .6s ease; }
.ss-leave-leave-to { opacity: 0; transform: scale(1.06); }

.ss-bg {
  position: absolute; inset: 0;
  background: radial-gradient(120% 120% at 50% 40%, #16294a 0%, #0b1220 70%);
  animation: ssFadeIn .5s ease both;
}
.ss-glow {
  position: absolute; width: 360px; height: 360px; border-radius: 50%;
  background: radial-gradient(circle, rgba(34,211,238,.22) 0%, transparent 65%);
  filter: blur(6px); animation: ssFadeIn 1s .3s ease both;
}
.ss-content { position: relative; text-align: center; }

.ss-mark { display: block; margin: 0 auto; overflow: visible; }

/* 双环描线生长 */
.ss-ring { fill: none; stroke-width: 12; stroke-linecap: round;
  stroke-dasharray: 1; stroke-dashoffset: 1;
  transform-box: fill-box; transform-origin: center; }
.ss-ringA { stroke: #4f46e5; animation: ssDraw 1s .35s ease forwards; }
.ss-ringB { stroke: #0ea5e9; animation: ssDraw 1s .55s ease forwards; }
.ss-core { fill: #22d3ee; transform-box: fill-box; transform-origin: center;
  opacity: 0; animation: ssPop .5s 1.15s ease forwards, ssGlow 2s 1.7s ease-in-out infinite; }

/* 点与流 */
.ss-dot { transform-box: fill-box; transform-origin: center; opacity: 0; }
.ss-cv { fill: #6366f1; animation: ssSlideL .6s ease forwards; }
.ss-sp { fill: #f59e0b; animation: ssPopR .5s ease forwards; }
.ss-flow { fill: none; stroke: #0ea5e9; stroke-width: 4; stroke-linecap: round;
  stroke-dasharray: 1; stroke-dashoffset: 1; opacity: .9; animation: ssDraw .5s ease forwards; }
.ss-flowR { stroke: #f59e0b; }

/* 字标 */
.ss-wordmark { margin-top: 18px; font-size: 30px; font-weight: 800; letter-spacing: -.5px;
  color: #fff; opacity: 0; animation: ssFadeUp .7s 1.8s ease forwards; }
.ss-wordmark b { color: #ffb020; }
.ss-tagline { margin-top: 8px; font-size: 14px; letter-spacing: 4px; color: #8aa0c0;
  opacity: 0; animation: ssFadeUp .7s 2.4s ease forwards; }

/* 进度条 */
.ss-progress { margin: 26px auto 0; width: 220px; height: 3px; border-radius: 3px;
  background: rgba(255,255,255,.12); overflow: hidden; opacity: 0; animation: ssFadeIn .4s 1.2s forwards; }
.ss-progress-bar { display: block; height: 100%; width: 0;
  background: linear-gradient(90deg, #4f46e5, #0ea5e9, #22d3ee); animation: ssSweep 3.6s .8s ease forwards; }

.ss-skip { position: absolute; top: 22px; right: 26px; background: transparent; border: 1px solid rgba(255,255,255,.25);
  color: #cbd5e1; font-size: 13px; padding: 6px 14px; border-radius: 20px; cursor: pointer;
  opacity: 0; animation: ssFadeIn .5s 1s forwards; transition: background .2s; }
.ss-skip:hover { background: rgba(255,255,255,.1); }

@keyframes ssFadeIn { from { opacity: 0 } to { opacity: 1 } }
@keyframes ssDraw { to { stroke-dashoffset: 0 } }
@keyframes ssPop { 0% { transform: scale(0); opacity: 0 } 60% { transform: scale(1.35); opacity: 1 } 100% { transform: scale(1); opacity: 1 } }
@keyframes ssSlideL { from { transform: translateX(-16px); opacity: 0 } to { transform: translateX(0); opacity: 1 } }
@keyframes ssPopR { 0% { transform: scale(0); opacity: 0 } 70% { transform: scale(1.25); opacity: 1 } 100% { transform: scale(1); opacity: 1 } }
@keyframes ssFadeUp { from { transform: translateY(16px); opacity: 0 } to { transform: translateY(0); opacity: 1 } }
@keyframes ssSweep { from { width: 0 } to { width: 100% } }
@keyframes ssGlow { 0%,100% { filter: drop-shadow(0 0 4px rgba(34,211,238,.5)) } 50% { filter: drop-shadow(0 0 14px rgba(34,211,238,.95)) } }
</style>
