<template>
  <section class="author-card" :class="[`author-card--${variant}`, { 'author-card--socials': showSocials }]" aria-label="项目作者信息">
    <div class="author-card__identity">
      <div class="author-card__mark" aria-hidden="true">JO</div>
      <div class="author-card__copy">
        <span class="author-card__eyebrow">PROJECT AUTHOR · LANLINKSHOPPING</span>
        <strong>{{ AUTHOR.name }}</strong>
        <small>{{ AUTHOR.project }} · {{ AUTHOR.role }}</small>
      </div>
      <span v-if="variant === 'detailed'" class="author-card__badge">ORIGINAL PROJECT</span>
    </div>

    <div class="author-card__meta">
      <span>© {{ year }} {{ AUTHOR.name }}</span>
      <span v-if="variant === 'detailed'">版权所有 · 未经授权请勿擅自复制、冒用或商业化</span>
    </div>

    <div v-if="showSocials" class="author-card__link-groups">
      <section class="author-card__link-group" aria-label="社交平台">
        <div class="author-card__group-heading">
          <strong>社交平台</strong>
          <small>FOLLOW · CONNECT</small>
        </div>
        <div class="author-card__socials">
      <a
        v-for="item in AUTHOR.socials"
        :key="item.name + item.url"
        :href="item.url"
        target="_blank"
        rel="noopener noreferrer"
        class="author-card__social"
      >
        <span class="author-card__social-icon">{{ item.icon }}</span>
        <span>
          <b>{{ item.name }}</b>
          <small>{{ item.handle }}</small>
        </span>
        <span class="author-card__arrow">↗</span>
      </a>
        </div>
      </section>

      <section class="author-card__link-group author-card__developer" aria-label="开发者交流区">
        <div class="author-card__group-heading">
          <strong>开发者交流区</strong>
          <small>DEVELOPER COMMUNITY</small>
        </div>
        <p class="author-card__group-description">用于项目讨论、开发协作、问题反馈与社区交流。</p>
        <div class="author-card__socials">
          <a
            v-for="item in AUTHOR.developerChannels"
            :key="item.name + item.url"
            :href="item.url"
            target="_blank"
            rel="noopener noreferrer"
            class="author-card__social"
          >
            <span class="author-card__social-icon">{{ item.icon }}</span>
            <span>
              <b>{{ item.name }}</b>
              <small>{{ item.handle }}</small>
            </span>
            <span class="author-card__arrow">↗</span>
          </a>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'compact' },
  showSocials: { type: Boolean, default: false }
})

const AUTHOR = {
  name: 'Jason Ouyang',
  project: 'LANLinkshopping',
  role: 'B2B 聚合型电商平台毕业设计',
  emails: ['3514485358@qq.com', 'andouyangjason001@gmail.com'],
  socials: [
    { name: 'X / Twitter', handle: '@Orion_Yves_Jude', url: 'https://x.com/Orion_Yves_Jude', icon: '𝕏' },
    { name: 'Facebook', handle: 'Jason Ouyang', url: 'https://www.facebook.com/profile.php?id=61590596057471', icon: 'f' },
    { name: 'Instagram', handle: '@yr54976', url: 'https://www.instagram.com/yr54976?stkn=MTE2a3AwcDZlZ3o5aA==', icon: '◎' },
    { name: 'Threads', handle: '@yr54976', url: 'https://www.threads.com/@yr54976', icon: '@' },
    { name: 'Discord', handle: 'LANLinkshopping 交流群', url: 'https://discord.gg/M3JpQGtbz', icon: '◈' },
  ],
  developerChannels: [
    { name: 'GitHub Discussions', handle: 'LANLinkshopping · 项目讨论区', url: 'https://github.com/c2jxmrywdn-del/LANLinkshopping/discussions/1', icon: '◇' },
    { name: 'Discord', handle: 'LANLinkshopping · 开发者社区', url: 'https://discord.gg/M3JpQGtbz', icon: '◈' }
  ]
}
const year = computed(() => new Date().getFullYear())
</script>

<style scoped>
.author-card {
  min-width: 0;
  border: 1px solid var(--ll-author-line, #D9D3C7);
  background: linear-gradient(120deg, rgba(255,255,255,.92), rgba(245,240,232,.82));
  color: var(--ll-ink, #0F172A);
  box-shadow: 0 12px 34px rgba(15,23,42,.055);
}
.author-card--compact {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  box-shadow: none;
  background: rgba(255,255,255,.42);
}
.author-card--detailed {
  grid-column: 1 / -1;
  padding: 18px 20px;
  border-radius: 18px;
}
.author-card__identity { min-width: 0; display: flex; align-items: center; gap: 13px; }
.author-card__mark {
  flex: 0 0 auto;
  width: 46px; height: 46px; border-radius: 13px;
  display: grid; place-items: center;
  background: var(--ll-brand-base, #13233A);
  color: #fff; font-weight: 850; letter-spacing: .04em;
}
.author-card--compact .author-card__mark { width: 38px; height: 38px; border-radius: 11px; font-size: 12px; }
.author-card__copy { min-width: 0; display: grid; gap: 3px; }
.author-card__eyebrow {
  color: var(--ll-brand-gold, #C8A45C);
  font-size: 8px; font-weight: 850; letter-spacing: .13em;
}
.author-card__copy strong { color: var(--ll-ink, #0F172A); font-size: 16px; line-height: 1.2; }
.author-card--compact .author-card__copy strong { font-size: 13px; }
.author-card__copy small { color: #7B8492; font-size: 10px; line-height: 1.45; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.author-card__badge {
  margin-left: auto; flex: 0 0 auto;
  padding: 6px 9px; border-radius: 999px;
  border: 1px solid rgba(155,45,32,.2);
  background: rgba(155,45,32,.05);
  color: var(--ll-brand-red, #9B2D20);
  font-size: 9px; font-weight: 850; letter-spacing: .08em;
}
.author-card__meta {
  display: flex; justify-content: space-between; gap: 14px;
  margin-top: 10px; padding-top: 9px;
  border-top: 1px solid rgba(217,211,199,.72);
  color: #7B8492; font-size: 10px; line-height: 1.55;
}
.author-card--compact .author-card__meta { margin: 0 0 0 auto; padding: 0; border-top: 0; text-align: right; white-space: nowrap; }
.author-card__link-groups { display: grid; gap: 18px; margin-top: 18px; }
.author-card__link-group { min-width: 0; }
.author-card__group-heading { display: flex; align-items: baseline; gap: 10px; margin-bottom: 8px; }
.author-card__group-heading strong { color: var(--ll-ink, #0F172A); font-size: 12px; }
.author-card__group-heading small { color: #9A8B70; font-size: 8px; font-weight: 800; letter-spacing: .12em; }
.author-card__group-description { margin: -2px 0 10px; color: #7B8492; font-size: 10px; line-height: 1.6; }
.author-card__socials { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 9px; }
.author-card__developer { padding: 13px; border: 1px solid rgba(200,164,92,.28); border-radius: 14px; background: rgba(245,240,232,.58); }
.author-card__developer .author-card__socials { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.author-card__social {
  min-width: 0; display: flex; align-items: center; gap: 9px;
  padding: 10px 11px; border: 1px solid #E6E0D6; border-radius: 12px;
  background: rgba(255,255,255,.72); color: inherit; text-decoration: none; transition: .2s ease;
}
.author-card__social:hover { transform: translateY(-1px); border-color: rgba(200,164,92,.65); box-shadow: 0 7px 18px rgba(15,23,42,.06); }
.author-card__social-icon {
  width: 29px; height: 29px; flex: 0 0 auto; display: grid; place-items: center;
  border-radius: 9px; background: #F5F0E8; color: var(--ll-brand-base, #13233A); font-weight: 800;
}
.author-card__social > span:nth-child(2) { min-width: 0; display: grid; gap: 1px; }
.author-card__social b { font-size: 11px; color: var(--ll-ink, #0F172A); }
.author-card__social small { font-size: 9px; color: #7B8492; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.author-card__arrow { margin-left: auto; color: var(--ll-brand-gold, #C8A45C); }
@media (max-width: 767px) {
  .author-card--compact { align-items: flex-start; flex-wrap: wrap; }
  .author-card--compact .author-card__meta { width: 100%; margin-left: 51px; text-align: left; }
  .author-card--detailed { grid-column: auto; }
  .author-card__badge { margin-left: 0; }
  .author-card__socials, .author-card__developer .author-card__socials { grid-template-columns: 1fr; }
  .author-card__meta { flex-direction: column; gap: 2px; }
}
</style>
