<template>
  <transition name="ss-leave">
    <div class="ss-overlay" v-show="visible">
      <div class="ss-bg" aria-hidden="true">
        <span class="ss-gradient-orb ss-orb-brick"></span>
        <span class="ss-gradient-orb ss-orb-gold"></span>
        <span class="ss-gradient-orb ss-orb-cyan"></span>
        <span class="ss-gradient-noise"></span>
      </div>

      <div class="ss-content">
        <svg class="ss-mark" viewBox="0 0 200 130" width="200" height="130" aria-hidden="true">
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
          <circle class="ss-ring ss-ringA" pathLength="1" cx="70" cy="65" r="24"/>
          <circle class="ss-ring ss-ringB" pathLength="1" cx="98" cy="65" r="24"/>
          <circle class="ss-core" cx="84" cy="65" r="6.5"/>
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

        <div class="ss-wordmark">LAN<b>Link</b>shopping</div>
        <div class="ss-tagline">聚合 · 连接 · 一体多元解决方案</div>
        <div class="ss-progress" aria-hidden="true"><span class="ss-progress-bar"></span></div>
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
  try { sessionStorage.setItem('ll-startup-seen', '1') } catch (_) {}
  setTimeout(() => emit('done'), 650)
}

onMounted(() => {
  timer = setTimeout(finish, 2100)
})

onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.ss-overlay {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; isolation: isolate;
}
.ss-leave-leave-active { transition: opacity .6s ease, transform .6s ease; }
.ss-leave-leave-to { opacity: 0; transform: scale(1.035); }

.ss-bg {
  position: absolute; inset: 0; overflow: hidden;
  background:
    radial-gradient(90% 80% at 50% 45%, rgba(30,110,184,.12), transparent 64%),
    linear-gradient(135deg, #13233A 0%, #101B2D 52%, #0B1220 100%);
  animation: ssBgIn .65s ease both;
}
.ss-bg::after {
  content: ""; position: absolute; inset: -20%;
  background: conic-gradient(from 120deg at 50% 50%,
    rgba(155,45,32,.07),
    rgba(200,164,92,.09),
    rgba(30,110,184,.10),
    rgba(155,45,32,.07));
  filter: blur(50px);
  animation: ssColorShift 7s ease-in-out infinite alternate;
}
.ss-gradient-orb {
  position: absolute; width: 520px; height: 520px; border-radius: 50%;
  filter: blur(46px); opacity: .16; mix-blend-mode: screen;
  will-change: transform, opacity;
}
.ss-orb-brick {
  left: -150px; top: 4%;
  background: radial-gradient(circle, rgba(155,45,32,.95) 0%, rgba(155,45,32,.45) 42%, transparent 72%);
  animation: ssOrbBrick 8s ease-in-out infinite alternate;
}
.ss-orb-gold {
  right: -120px; top: 18%;
  background: radial-gradient(circle, rgba(200,164,92,.9) 0%, rgba(200,164,92,.35) 45%, transparent 72%);
  animation: ssOrbGold 9s ease-in-out -2s infinite alternate;
}
.ss-orb-cyan {
  left: 32%; bottom: -260px;
  background: radial-gradient(circle, rgba(30,110,184,.95) 0%, rgba(30,110,184,.4) 45%, transparent 72%);
  animation: ssOrbCyan 8.5s ease-in-out -1s infinite alternate;
}
.ss-gradient-noise {
  position: absolute; inset: 0; opacity: .045;
  background-image: radial-gradient(rgba(251,249,241,.55) .6px, transparent .6px);
  background-size: 4px 4px;
  animation: ssNoiseDrift 6s linear infinite;
}

.ss-content { position: relative; z-index: 2; text-align: center; }
.ss-mark { display: block; margin: 0 auto; overflow: visible; }
.ss-ring {
  fill: none; stroke-width: 12; stroke-linecap: round;
  stroke-dasharray: 1; stroke-dashoffset: 1;
  transform-box: fill-box; transform-origin: center;
}
.ss-ringA { stroke: #9B2D20; animation: ssDraw 1s .35s ease forwards; }
.ss-ringB { stroke: #1E6EB8; animation: ssDraw 1s .55s ease forwards; }
.ss-core {
  fill: #C8A45C; transform-box: fill-box; transform-origin: center; opacity: 0;
  animation: ssPop .5s 1.15s ease forwards, ssGlow 2s 1.7s ease-in-out infinite;
}
.ss-dot { transform-box: fill-box; transform-origin: center; opacity: 0; }
.ss-cv { fill: #1E6EB8; animation: ssSlideL .6s ease forwards; }
.ss-sp { fill: #C8A45C; animation: ssPopR .5s ease forwards; }
.ss-flow {
  fill: none; stroke: #1E6EB8; stroke-width: 4; stroke-linecap: round;
  stroke-dasharray: 1; stroke-dashoffset: 1; opacity: .9; animation: ssDraw .5s ease forwards;
}
.ss-flowR { stroke: #C8A45C; }

.ss-wordmark {
  margin-top: 18px; font-size: 30px; font-weight: 800; letter-spacing: -.5px;
  color: #FBF9F1; opacity: 0; animation: ssFadeUp .7s 1.55s ease forwards;
}
.ss-wordmark b { color: #C8A45C; }
.ss-tagline {
  margin-top: 8px; font-size: 14px; letter-spacing: 4px; color: #AEB7C4;
  opacity: 0; animation: ssFadeUp .7s 1.72s ease forwards;
}
.ss-progress {
  margin: 26px auto 0; width: 220px; height: 3px; border-radius: 3px;
  background: rgba(251,249,241,.12); overflow: hidden; opacity: 0;
  animation: ssFadeIn .4s 1s forwards;
}
.ss-progress-bar {
  display: block; height: 100%; width: 0;
  background: linear-gradient(90deg, #9B2D20 0%, #C8A45C 46%, #1E6EB8 100%);
  animation: ssSweep 1.95s .15s cubic-bezier(.22,.8,.32,1) forwards;
}
.ss-skip {
  position: absolute; top: 22px; right: 26px; z-index: 3;
  background: rgba(19,35,58,.22); border: 1px solid rgba(251,249,241,.34);
  color: #E8E3D9; font-size: 13px; padding: 6px 14px; border-radius: 20px;
  cursor: pointer; opacity: 0; animation: ssFadeIn .5s .8s forwards;
  transition: background .2s ease, border-color .2s ease;
}
.ss-skip:hover { background: rgba(251,249,241,.08); border-color: rgba(251,249,241,.6); }

@keyframes ssBgIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes ssColorShift { from { transform: translate3d(-4%, -2%, 0) rotate(0deg) scale(1); } to { transform: translate3d(4%, 3%, 0) rotate(18deg) scale(1.08); } }
@keyframes ssOrbBrick { from { transform: translate3d(0,0,0) scale(.88); opacity:.10; } to { transform: translate3d(190px,120px,0) scale(1.16); opacity:.18; } }
@keyframes ssOrbGold { from { transform: translate3d(0,0,0) scale(1.04); opacity:.09; } to { transform: translate3d(-150px,95px,0) scale(.86); opacity:.18; } }
@keyframes ssOrbCyan { from { transform: translate3d(-40px,0,0) scale(.86); opacity:.08; } to { transform: translate3d(110px,-150px,0) scale(1.15); opacity:.16; } }
@keyframes ssNoiseDrift { from { transform: translate3d(0,0,0); } to { transform: translate3d(12px,8px,0); } }
@keyframes ssDraw { to { stroke-dashoffset: 0; } }
@keyframes ssPop { 0% { transform: scale(0); opacity: 0; } 60% { transform: scale(1.35); opacity: 1; } 100% { transform: scale(1); opacity: 1; } }
@keyframes ssSlideL { from { transform: translateX(-16px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
@keyframes ssPopR { 0% { transform: scale(0); opacity: 0; } 70% { transform: scale(1.25); opacity: 1; } 100% { transform: scale(1); opacity: 1; } }
@keyframes ssFadeUp { from { transform: translateY(16px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes ssFadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes ssSweep { from { width: 0; } to { width: 100%; } }
@keyframes ssGlow {
  0%,100% { filter: drop-shadow(0 0 4px rgba(200,164,92,.42)); }
  50% { filter: drop-shadow(0 0 14px rgba(200,164,92,.82)); }
}

@media (prefers-reduced-motion: reduce) {
  .ss-leave-leave-active { transition: opacity .15s linear; }
  .ss-leave-leave-to { transform: none; }
  .ss-bg::after, .ss-gradient-orb, .ss-gradient-noise,
  .ss-ringA, .ss-ringB, .ss-core, .ss-dot, .ss-flow,
  .ss-wordmark, .ss-tagline, .ss-progress, .ss-skip { animation: none !important; }
  .ss-ring { stroke-dashoffset: 0; }
  .ss-core, .ss-dot, .ss-wordmark, .ss-tagline, .ss-progress, .ss-skip { opacity: 1; }
  .ss-progress-bar { width: 100%; }
}
</style>
