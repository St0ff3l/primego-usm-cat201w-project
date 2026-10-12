<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import ReceiptUploader from '../components/ReceiptUploader.vue'

const receipt = ref(null)
const amount = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')

async function checkAccess() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (user.role !== 'CUSTOMER') {
      window.location.replace(user.role === 'ADMIN' ? '/admin/wallet/admin_approval.jsp' : '/public/wallet/wallet.jsp')
      return
    }
    document.title = 'Top Up Wallet - Manual QR'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to read the current session.'
  } finally {
    loading.value = false
  }
}

async function submitTopUp(event) {
  error.value = ''
  if (!receipt.value) {
    error.value = 'Please upload your payment receipt.'
    return
  }
  busy.value = true
  try {
    const body = new FormData(event.currentTarget)
    body.set('receipt', receipt.value)
    const response = await fetch(legacyBase + '/TopUpServlet', {
      method: 'POST',
      headers: { Accept: 'application/json' },
      credentials: 'include',
      body,
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to submit your top-up request.')
    sessionStorage.setItem('walletMessage', result.data.message || 'Top-up request submitted! Waiting for approval.')
    window.location.replace('/public/wallet/wallet.jsp')
  } catch (submitError) {
    error.value = submitError.message || 'Unable to submit your top-up request.'
  } finally {
    busy.value = false
  }
}

onMounted(checkAccess)
</script>

<template>
  <div class="topup-page">
    <div class="background-blob blob-red"></div><div class="background-blob blob-yellow"></div><div class="background-blob blob-orange"></div>
    <main class="glass-panel">
      <div class="top-bar">
        <a href="/public/wallet/wallet.jsp" class="back-btn" title="Back to Wallet" aria-label="Back to wallet"><i class="ri-arrow-left-line"></i></a>
        <h1 class="page-title">Manual Top Up</h1>
      </div>
      <div v-if="loading" class="page-state" aria-live="polite">Loading...</div>
      <template v-else>
        <div v-if="error" class="error-notice" role="alert">{{ error }}</div>
        <div class="topup-grid">
          <section class="qr-section" aria-labelledby="qr-title">
            <h2 id="qr-title" class="qr-title">Scan to Pay</h2>
            <p class="qr-desc">Use Touch'n Go / DuitNow</p>
            <div class="qr-img-placeholder"><img :src="legacyPageUrl('/assets/images/QR%20code.jpg')" class="qr-img-real" alt="DuitNow QR code" /></div>
            <div class="bank-info">
              <div class="bank-row"><span>Bank:</span><strong>Maybank</strong></div>
              <div class="bank-row"><span>Acc:</span><strong>5123 4567 8901</strong></div>
              <div class="bank-row"><span>Name:</span><strong>PrimeGo Sdn Bhd</strong></div>
            </div>
          </section>
          <section class="form-section">
            <form @submit.prevent="submitTopUp">
              <div class="input-group">
                <label for="amount">1. Enter Amount (RM)</label>
                <input id="amount" v-model="amount" type="number" name="amount" placeholder="e.g. 50.00" min="1" step="0.01" required />
              </div>
              <div class="input-group">
                <label>2. Upload Receipt</label>
                <ReceiptUploader v-model="receipt" name="receipt" />
              </div>
              <button class="btn-submit" type="submit" :disabled="busy">{{ busy ? 'Submitting...' : 'Submit for Approval' }}</button>
              <p class="submit-note"><i class="ri-time-line"></i> Your request will be processed within 24 hours.</p>
            </form>
          </section>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.topup-page { position: relative; min-height: 100vh; overflow-x: hidden; padding: 1px 16px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #333; font-family: Poppins,sans-serif; }
.background-blob { position: fixed; z-index: 0; border-radius: 50%; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; background: white; }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; background: white; transform: rotate(30deg); }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.glass-panel { position: relative; z-index: 1; max-width: 900px; margin: 60px auto; padding: 40px; border: 1px solid rgba(255,255,255,.6); border-radius: 24px; background: rgba(255,255,255,.75); box-shadow: 0 10px 40px rgba(0,0,0,.08); backdrop-filter: blur(20px); }
.top-bar { display: flex; align-items: center; margin-bottom: 30px; }
.back-btn { display: grid; width: 40px; height: 40px; flex: 0 0 40px; place-items: center; border-radius: 50%; background: rgba(255,255,255,.8); color: #555; font-size: 1.2rem; text-decoration: none; transition: .2s; }
.back-btn:hover { background: white; box-shadow: 0 4px 10px rgba(0,0,0,.1); transform: scale(1.1); }
.page-title { margin: 0 0 0 20px; color: #2d3436; font-size: 1.5rem; font-weight: 700; }
.page-state { padding: 45px; color: #666; text-align: center; }
.error-notice { margin-bottom: 20px; padding: 12px 16px; border: 1px solid #f5c6cb; border-radius: 12px; background: #f8d7da; color: #721c24; }
.topup-grid { display: grid; grid-template-columns: 1fr 1.2fr; gap: 40px; }
.qr-section { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px; border-radius: 20px; background: white; box-shadow: 0 8px 20px rgba(0,0,0,.05); text-align: center; }
.qr-title { margin: 0 0 5px; color: #2d3436; font-size: 1.1rem; font-weight: 700; }
.qr-desc { margin: 0 0 20px; color: #888; font-size: .85rem; }
.qr-img-placeholder { display: flex; width: 200px; height: 200px; align-items: center; justify-content: center; margin-bottom: 15px; }
.qr-img-real { width: 100%; height: 100%; border-radius: 8px; object-fit: contain; }
.bank-info { width: 100%; padding: 10px; border-radius: 10px; background: #f8f9fa; color: #555; font-size: .85rem; text-align: left; }
.bank-row { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 5px; }
.bank-row:last-child { margin-bottom: 0; }
.form-section { display: flex; flex-direction: column; justify-content: center; }
.input-group { margin-bottom: 20px; }
.input-group label { display: block; margin-bottom: 8px; color: #444; font-size: .9rem; font-weight: 600; }
.input-group input { width: 100%; padding: 12px 16px; border: 1px solid rgba(0,0,0,.1); border-radius: 12px; outline: none; background: rgba(255,255,255,.6); font-size: 1rem; transition: .3s; }
.input-group input:focus { border-color: #0984e3; background: white; }
.btn-submit { width: 100%; padding: 14px; border: 0; border-radius: 50px; background: linear-gradient(135deg,#0984e3,#74b9ff); box-shadow: 0 8px 20px rgba(9,132,227,.3); color: white; cursor: pointer; font-size: 1rem; font-weight: 700; transition: .3s; }
.btn-submit:hover:not(:disabled) { box-shadow: 0 12px 25px rgba(9,132,227,.4); transform: translateY(-2px); }
.btn-submit:disabled { cursor: wait; opacity: .65; }
.submit-note { margin: 15px 0 0; color: #888; font-size: .8rem; text-align: center; }
@media(max-width:768px) { .glass-panel { margin: 20px auto; padding: 24px 18px; } .topup-grid { grid-template-columns: 1fr; gap: 22px; } .qr-section { padding: 22px; } }
</style>
