<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'

const username = ref('')
const password = ref('')
const contactInfo = ref('')
const error = ref('')
const submitting = ref(false)
const checkingSession = ref(true)

onMounted(async () => {
  try {
    const session = await getCurrentUser()
    if (!session.authenticated || session.role !== 'ADMIN') {
      window.location.replace(legacyPageUrl('/login'))
      return
    }
    document.title = 'Create Merchant - PrimeGo Admin'
  } catch {
    error.value = 'Unable to verify the administrator session.'
  } finally {
    checkingSession.value = false
  }
})

async function createMerchant() {
  submitting.value = true
  error.value = ''
  try {
    const body = new URLSearchParams({
      username: username.value,
      password: password.value,
      contactInfo: contactInfo.value,
    })
    const response = await fetch(`${legacyBase}/admin/merchant/create`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body,
    })
    const result = await response.json().catch(() => null)
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to create merchant account.')
    window.location.assign('/admin/dashboard')
  } catch (createError) {
    error.value = createError.message || 'Unable to create merchant account.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-merchant-create-page">
    <div class="background-blob blob-red" aria-hidden="true"></div>
    <main class="container">
      <h1>Create Merchant Account</h1>

      <p v-if="checkingSession" class="status-message" role="status">Checking administrator session...</p>
      <p v-else-if="error" class="error-msg" role="alert">{{ error }}</p>

      <form @submit.prevent="createMerchant">
        <div class="form-group">
          <label for="merchant-username">Username</label>
          <input id="merchant-username" v-model="username" name="username" type="text" autocomplete="off" required :disabled="checkingSession || submitting">
        </div>

        <div class="form-group">
          <label for="merchant-password">Password</label>
          <input id="merchant-password" v-model="password" name="password" type="password" autocomplete="new-password" required :disabled="checkingSession || submitting">
        </div>

        <div class="form-group">
          <label for="merchant-contact">Contact Info</label>
          <textarea id="merchant-contact" v-model="contactInfo" name="contactInfo" rows="3" placeholder="Phone, Email, Address..." :disabled="checkingSession || submitting"></textarea>
        </div>

        <button class="btn-submit" type="submit" :disabled="checkingSession || submitting">
          {{ submitting ? 'Creating...' : 'Create Merchant' }}
        </button>
        <a class="btn-cancel" href="/admin/dashboard">Cancel &amp; Back to Dashboard</a>
      </form>
    </main>
  </div>
</template>

<style scoped>
.admin-merchant-create-page,.admin-merchant-create-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-merchant-create-page { position: relative; z-index: 1; display: flex; min-height: 100vh; align-items: center; justify-content: center; padding: 24px 0; color: #333; }
.background-blob { position: fixed; z-index: -1; top: -200px; left: -200px; width: 700px; height: 700px; border-radius: 50%; background: linear-gradient(145deg,#ff5e55,#d92e25); filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.container { position: relative; z-index: 1; width: 500px; max-width: 90%; padding: 40px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.85); box-shadow: 0 10px 40px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
h1 { margin: 0 0 30px; color: #d63031; font-size: 1.8rem; text-align: center; }
.form-group { margin-bottom: 20px; }
label { display: block; margin-bottom: 8px; color: #555; font-weight: 500; }
input,textarea { width: 100%; padding: 12px 15px; border: 1px solid rgba(0,0,0,.1); border-radius: 10px; outline: none; background: rgba(255,255,255,.9); font-size: .95rem; transition: .3s; }
input:focus,textarea:focus { border-color: #d63031; box-shadow: 0 0 0 3px rgba(214,48,49,.1); }
input:disabled,textarea:disabled { opacity: .65; }
textarea { resize: vertical; }
.btn-submit { width: 100%; margin-top: 10px; padding: 12px; border: 0; border-radius: 10px; background: #d63031; color: white; cursor: pointer; font-size: 1rem; font-weight: 600; transition: .3s; }
.btn-submit:hover:not(:disabled) { transform: translateY(-2px); background: #c0392b; box-shadow: 0 5px 15px rgba(214,48,49,.3); }
.btn-submit:disabled { cursor: wait; opacity: .7; }
.btn-cancel { display: block; margin-top: 15px; color: #666; font-size: .9rem; text-align: center; text-decoration: none; }
.btn-cancel:hover { text-decoration: underline; }
.error-msg { margin-bottom: 20px; padding: 10px; border: 1px solid #ffcdd2; border-radius: 8px; background: #ffebee; color: #c62828; font-size: .9rem; text-align: center; overflow-wrap: anywhere; }
.status-message { margin-bottom: 20px; color: #666; text-align: center; }
@media(max-width:540px) { .container { padding: 28px 22px; } h1 { font-size: 1.55rem; } }
</style>
