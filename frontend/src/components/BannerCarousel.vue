<template>
  <div class="banner-wrap">
    <Swiper
      :modules="modules"
      :slides-per-view="1"
      :loop="true"
      :speed="700"
      :autoplay="{ delay: 3500, disableOnInteraction: false, pauseOnMouseEnter: true }"
      :pagination="{ clickable: true, dynamicBullets: true }"
      :navigation="true"
      :keyboard="{ enabled: true }"
      :grab-cursor="true"
      class="banner-swiper"
    >
      <SwiperSlide v-for="s in slides" :key="s.key">
        <div class="slide" @click="go(s)">
          <img :src="s.img" :alt="s.title" loading="lazy" decoding="async" />
          <div class="mask"></div>
          <div class="content">
            <h2>{{ s.title }}</h2>
            <p>{{ s.sub }}</p>
            <span class="cta">{{ s.cta }} →</span>
          </div>
        </div>
      </SwiperSlide>
    </Swiper>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Autoplay, Pagination, Navigation, Keyboard } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/pagination'
import 'swiper/css/navigation'

const router = useRouter()
const modules = [Autoplay, Pagination, Navigation, Keyboard]

const slides = [
  { key: 'c', img: '/banners/01-construction.jpg', tag: '建筑行业', title: '源头钢材 · 水泥 · 管材直供', sub: '资质厂商入驻，规格与检测报告可查', cta: '进入建筑专区', indId: 1 },
  { key: 't', img: '/banners/02-textile.jpg', tag: '纺织行业', title: '面料 · 纱线 · 辅料一站采购', sub: '按克重、色牢度严选源头货盘', cta: '进入纺织专区', indId: 2 },
  { key: 'p', img: '/banners/03-petrochemical.jpg', tag: '石化行业', title: '化工原料 · 油品合规供应', sub: '危化经营许可核验，运输条件透明', cta: '进入石化专区', indId: 3 },
  { key: 'e', img: '/banners/04-electronics.jpg', tag: '电子行业', title: '元器件 · 连接器 · 显示面板', sub: '3C / RoHS 认证，批次可追溯', cta: '进入电子专区', indId: 4 },
  { key: 'g', img: '/banners/05-gift.jpg', tag: '礼品定制', title: '企业定制 · 打样到批量交付', sub: '发布需求，源头厂商报价打样', cta: '了解礼品定制' },
  { key: 'f', img: '/banners/06-supplychain.jpg', tag: '供应链金融', title: '账期 · 尾款 · 缓解回款压力', sub: '凭信用与订单申请账期结算', cta: '了解账期服务' },
]

function go(s) {
  if (s.indId) router.push({ name: 'mall', query: { indId: s.indId } })
  else router.push({ name: 'mall' })
}
</script>

<style scoped>
.banner-wrap { margin-bottom: 24px; border-radius: 14px; overflow: hidden; box-shadow: 0 6px 24px rgba(15,23,42,.12); }
.banner-swiper { width: 100%; }
.slide { position: relative; height: 320px; cursor: pointer; }
.slide img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mask { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(15,23,42,.78) 0%, rgba(15,23,42,.35) 45%, rgba(15,23,42,0) 75%); }
.content { position: absolute; left: 0; bottom: 0; top: 0; display: flex; flex-direction: column; justify-content: center; padding: 0 48px; color: #fff; max-width: 62%; }
.content h2 { font-size: 30px; margin: 0 0 8px; font-weight: 800; text-shadow: 0 2px 10px rgba(0,0,0,.35); }
.content p { font-size: 15px; margin: 0 0 14px; opacity: .92; }
.cta { font-size: 14px; font-weight: 600; color: #ffb020; }
</style>

<style>
/* Swiper 全局控件主题化（品牌色） */
/* 圆点：默认隐藏，悬停轮播时才显示 */
.banner-swiper .swiper-pagination { opacity: 0; transition: opacity .25s ease; }
.banner-swiper:hover .swiper-pagination { opacity: 1; }
.banner-swiper .swiper-pagination-bullet { background: #cbd5e1; opacity: .8; }
.banner-swiper .swiper-pagination-bullet-active { background: #0ea5e9; opacity: 1; }

/* 箭头：无底衬，默认隐藏，悬停浮现，半透明，两侧垂直居中 */
.banner-swiper .swiper-button-next,
.banner-swiper .swiper-button-prev {
  width: auto; height: auto; margin-top: 0; border-radius: 0;
  top: 50%;
  background: none; box-shadow: none; backdrop-filter: none;
  color: rgba(255, 255, 255, .65);
  opacity: 0; pointer-events: none;
  transition: opacity .25s ease, transform .25s ease, color .2s ease;
}
.banner-swiper .swiper-button-prev { left: 16px; transform: translateY(-50%) translateX(-12px); }
.banner-swiper .swiper-button-next { right: 16px; transform: translateY(-50%) translateX(12px); }
.banner-swiper:hover .swiper-button-next,
.banner-swiper:hover .swiper-button-prev { opacity: 1; pointer-events: auto; }
.banner-swiper:hover .swiper-button-prev { transform: translateY(-50%) translateX(0); }
.banner-swiper:hover .swiper-button-next { transform: translateY(-50%) translateX(0); }
.banner-swiper .swiper-button-next:hover,
.banner-swiper .swiper-button-prev:hover { color: rgba(255, 255, 255, .95); }
.banner-swiper .swiper-button-next:after,
.banner-swiper .swiper-button-prev:after { font-size: 44px; font-weight: 700; }
</style>
