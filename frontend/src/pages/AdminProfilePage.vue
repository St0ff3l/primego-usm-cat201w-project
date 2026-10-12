<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase } from '../api'
import AdminBackground from '../components/AdminBackground.vue'

const username = ref('')
const profile = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const session = await getCurrentUser()
    if (!session.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (session.role !== 'ADMIN') {
      window.location.replace(session.role === 'MERCHANT' ? '/merchant/user/merchant_profile.jsp' : '/profile')
      return
    }
    const response = await fetch(`${legacyBase}/profile`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load admin profile.')
    username.value = result.data.username || ''
    profile.value = result.data.profile
    document.title = 'Admin Profile - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load admin profile.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="admin-profile-page">
    <AdminBackground />
    <main class="glass-panel">
      <h1>Admin Profile</h1>
      <div v-if="loading" class="profile-info muted" role="status">Loading profile...</div>
      <div v-else-if="error" class="profile-info error" role="alert">{{ error }}</div>
      <div v-else class="profile-info">
        <p><strong>Username:</strong> {{ username }}</p>
        <template v-if="profile">
          <p><strong>Department:</strong> {{ profile.department }}</p>
          <p><strong>Access Level:</strong> {{ profile.level }}</p>
        </template>
        <p v-else>No additional profile details found.</p>
      </div>
      <a class="btn-back" href="/">Back to Home</a>
      <a class="btn-back logout" href="/logout">Logout</a>
    </main>
  </div>
</template>

<style scoped>
.admin-profile-page,.admin-profile-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-profile-page { position: relative; z-index: 1; min-height: 100vh; overflow-x: hidden; padding: 1px 16px; color: #333; }
.glass-panel { position: relative; z-index: 1; width: 100%; max-width: 800px; margin: 100px auto; padding: 40px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); box-shadow: 0 8px 32px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
h1 { margin: 0 0 20px; color: #d63031; }
.profile-info p { margin: 0 0 15px; font-size: 1.1rem; overflow-wrap: anywhere; }
.btn-back { display: inline-block; margin: 20px 10px 0 0; padding: 10px 20px; border-radius: 20px; background: #333; color: white; text-decoration: none; }
.logout { background: #d63031; }
.muted { color: #666; }
.error { color: #c62828; overflow-wrap: anywhere; }
@media(max-width:540px) { .glass-panel { margin: 48px auto; padding: 28px 22px; } }
</style>
