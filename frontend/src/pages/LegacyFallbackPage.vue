<script setup>
import { onMounted } from 'vue'
import { legacyPageUrl } from '../api'

onMounted(() => {
  window.location.replace(legacyPageUrl(window.location.pathname + window.location.search + window.location.hash))
})
</script>

<template>
  <main class="loading-page" aria-live="polite">Loading...</main>
</template>

<style scoped>
.loading-page { min-height: 100vh; display: grid; place-items: center; color: #636e72; font-family: 'Poppins', sans-serif; }
</style>
