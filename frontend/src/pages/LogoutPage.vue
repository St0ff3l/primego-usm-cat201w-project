<script setup>
import { onMounted, ref } from 'vue'
import { legacyBase } from '../api'

const error = ref('')
const busy = ref(false)

async function logout() {
  busy.value = true
  error.value = ''
  try {
    const response = await fetch(`${legacyBase}/logout`, { credentials: 'include' })
    if (!response.ok) throw new Error('Unable to end this session.')
    window.location.replace('/public/login.jsp')
  } catch (logoutError) {
    error.value = logoutError.message || 'Unable to log out. Please try again.'
    busy.value = false
  }
}

onMounted(logout)
</script>

<template>
  <main class="logout-page" aria-live="polite">
    <p v-if="busy">Signing out...</p>
    <p v-else-if="error" role="alert">{{ error }}</p>
    <button v-if="!busy && error" type="button" @click="logout">Try again</button>
  </main>
</template>

<style scoped>
.logout-page { display: grid; min-height: 100vh; place-content: center; gap: 16px; color: #333; font-family: Poppins, sans-serif; text-align: center; }
.logout-page button { justify-self: center; padding: 10px 18px; border: 0; border-radius: 8px; background: #ff9500; color: #fff; cursor: pointer; font: inherit; }
</style>
