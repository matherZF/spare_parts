import { ref, onMounted, onBeforeUnmount } from 'vue'

const isMobile = ref(false)
let listeners = 0
let resizeTimer = null

function checkViewport() {
  isMobile.value = window.innerWidth < 768
}

function onResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkViewport, 100)
}

export function useMobile() {
  onMounted(() => {
    if (listeners === 0) {
      checkViewport()
      window.addEventListener('resize', onResize)
    }
    listeners++
  })

  onBeforeUnmount(() => {
    listeners--
    if (listeners <= 0) {
      window.removeEventListener('resize', onResize)
      if (resizeTimer) clearTimeout(resizeTimer)
    }
  })

  return { isMobile }
}
