<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import MerchantBackground from '../components/MerchantBackground.vue'

const username = ref('')
const profile = ref(null)
const loading = ref(true)
const error = ref('')

async function loadProfile() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (user.role !== 'MERCHANT') {
      window.location.replace('/profile')
      return
    }
    username.value = user.username || ''

    const response = await fetch(`${legacyBase}/api/merchant/profile`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load your merchant profile.')
    profile.value = result.data.profile
    document.title = 'Merchant Profile - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your merchant profile.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="merchant-profile-page">
    <MerchantBackground />
    <div v-if="loading" class="glass-panel page-state" aria-live="polite">Loading profile...</div>
    <div v-else-if="error" class="glass-panel page-state" role="alert">{{ error }}</div>
    <div v-else class="glass-panel">
      <h1>Merchant Profile</h1>
      <div class="profile-info">
        <p><strong>Username:</strong> {{ username }}</p>
        <template v-if="profile">
          <p><strong>Store Name:</strong> {{ profile.storeName }}</p>
          <p><strong>Business License:</strong> {{ profile.businessLicense }}</p>
          <p><strong>Contact Info:</strong> {{ profile.contactInfo }}</p>
        </template>
        <p v-else>No additional profile details found.</p>
      </div>
      <a :href="legacyPageUrl('/logout')" class="btn-back">Logout</a>
    </div>
  </div>
</template>

<style scoped>
.merchant-profile-page { position: relative; min-height: 100vh; overflow-x: hidden; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #333; }
.merchant-profile-page,.merchant-profile-page * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.glass-panel { position: relative; z-index: 1; max-width: 800px; margin: 100px auto; padding: 40px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); box-shadow: 0 8px 32px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
h1 { margin-bottom: 20px; color: #e6b800; }
.profile-info p { margin-bottom: 15px; font-size: 1.1rem; }
.btn-back { display: inline-block; margin-top: 20px; padding: 10px 20px; border-radius: 20px; background: #e6b800; color: white; text-decoration: none; }
.page-state { margin: 100px auto; color: #636e72; text-align: center; }
</style>
