<template>
  <router-view :key="route.fullPath"/>
  <button
      v-show="showBackTop"
      class="back-to-top"
      type="button"
      aria-label="回到顶部"
      @click="backToTop"
  >
    <el-icon><ArrowUpBold /></el-icon>
  </button>
</template>
<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {useRoute} from 'vue-router'
const route = useRoute();
const showBackTop = ref(false)

function updateBackTopVisible() {
  showBackTop.value = window.scrollY > 520
}

function backToTop() {
  window.scrollTo({top: 0, behavior: 'smooth'})
}

onMounted(() => {
  updateBackTopVisible()
  window.addEventListener('scroll', updateBackTopVisible, {passive: true})
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateBackTopVisible)
})
</script>

<style scoped lang="scss">
.back-to-top {
  position: fixed;
  right: clamp(18px, 3vw, 38px);
  bottom: clamp(24px, 5vw, 54px);
  z-index: 120;
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(17, 16, 14, .08);
  border-radius: 50%;
  color: var(--brand-dark);
  background: rgba(255, 253, 248, .92);
  box-shadow: 0 16px 38px rgba(40, 32, 18, .18);
  backdrop-filter: blur(14px);
  cursor: pointer;
  transition: transform .18s ease, background .18s ease, box-shadow .18s ease;

  &:hover {
    transform: translateY(-2px);
    background: var(--brand-primary);
    box-shadow: 0 18px 42px rgba(40, 32, 18, .22);
  }
}
</style>
