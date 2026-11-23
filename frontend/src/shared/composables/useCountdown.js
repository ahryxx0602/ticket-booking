import { ref, computed, onUnmounted, getCurrentInstance } from 'vue'

export function useCountdown(onExpiredCallback) {
  const remainingSeconds = ref(0)
  let timerId = null

  const formattedTime = computed(() => {
    const mins = Math.floor(remainingSeconds.value / 60)
    const secs = remainingSeconds.value % 60
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  })

  const isRunning = computed(() => remainingSeconds.value > 0)

  function start(expiresAtIso) {
    stop()
    const targetMs = new Date(expiresAtIso).getTime()
    const update = () => {
      const diff = Math.max(0, Math.ceil((targetMs - Date.now()) / 1000))
      remainingSeconds.value = diff
      if (diff <= 0) {
        stop()
        if (typeof onExpiredCallback === 'function') {
          onExpiredCallback()
        }
      }
    }
    update()
    if (remainingSeconds.value > 0) {
      timerId = setInterval(update, 1000)
    }
  }

  function stop() {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
    remainingSeconds.value = 0
  }

  if (getCurrentInstance()) {
    onUnmounted(stop)
  }

  return { remainingSeconds, formattedTime, isRunning, start, stop }
}
