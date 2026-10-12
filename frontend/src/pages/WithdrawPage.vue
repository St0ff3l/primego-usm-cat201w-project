<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import ReceiptUploader from '../components/ReceiptUploader.vue'

const role = ref('CUSTOMER')
const balance = ref(0)
const receiptImage = ref(null)
const amount = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')

async function loadWallet() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!['CUSTOMER', 'MERCHANT'].includes(user.role)) {
      window.location.replace(legacyPageUrl(window.location.pathname + window.location.search))
      return
    }
    role.value = user.role
    const response = await fetch(legacyBase + '/api/wallet', {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load your wallet balance.')
    balance.value = Number(result.data.balance || 0)
    document.title = role.value === 'MERCHANT' ? 'Withdraw Revenue - PrimeGo' : 'Withdraw Funds - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your wallet balance.'
  } finally {
    loading.value = false
  }
}

async function submitWithdrawal(event) {
  error.value = ''
  const value = Number(amount.value)
  if (!Number.isFinite(value) || value <= 0) {
    error.value = 'Please enter a valid amount greater than 0.'
    return
  }
  if (value > balance.value) {
    error.value = 'Withdrawal amount exceeds your available balance.'
    return
  }
  if (!receiptImage.value) {
    error.value = 'Please upload your QR Code.'
    return
  }

  busy.value = true
  try {
    const body = new FormData(event.currentTarget)
    body.set('receiptImage', receiptImage.value)
    const response = await fetch(legacyBase + '/WithdrawServlet', {
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
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to submit your withdrawal request.')
    sessionStorage.setItem('walletMessage', result.data.message || 'Withdrawal request submitted! QR Code uploaded.')
    window.location.replace('/public/wallet/wallet.jsp')
  } catch (submitError) {
    error.value = submitError.message || 'Unable to submit your withdrawal request.'
  } finally {
    busy.value = false
  }
}

onMounted(loadWallet)
</script>

<template>
  <div class="withdraw-page">
    <div class="background-blob blob-red"></div><div class="background-blob blob-yellow"></div><div class="background-blob blob-orange"></div>
    <main class="glass-panel">
      <div class="back-row"><a href="/public/wallet/wallet.jsp" class="back-btn" title="Back to Wallet" aria-label="Back to wallet"><i class="ri-arrow-left-line"></i><span class="back-text">Back</span></a></div>
      <h1>{{ role === 'MERCHANT' ? 'Withdraw Revenue' : 'Withdraw Funds' }}</h1>
      <div v-if="loading" class="page-state" aria-live="polite">Loading wallet...</div>
      <template v-else>
        <div v-if="error" class="error-notice" role="alert"><i class="ri-error-warning-line"></i> {{ error }}</div>
        <div class="balance-info">
          <div class="balance-label">Available Balance</div>
          <div class="balance-amount">RM {{ balance.toFixed(2) }}</div>
        </div>
        <form @submit.prevent="submitWithdrawal">
          <div class="form-group">
            <label class="form-label" for="withdraw-amount">Amount (RM)</label>
            <input id="withdraw-amount" v-model="amount" type="number" name="amount" class="form-control" min="0.01" step="0.01" placeholder="e.g. 50.00" required />
          </div>
          <div class="form-group">
            <label class="form-label">Upload QR Code (DuitNow / TnG)</label>
            <ReceiptUploader v-model="receiptImage" name="receiptImage" />
          </div>
          <button class="btn-submit" type="submit" :disabled="busy">{{ busy ? 'Submitting...' : 'Confirm Withdraw' }}</button>
        </form>
      </template>
    </main>
  </div>
</template>

<style scoped>
.withdraw-page { position: relative; display: flex; min-height: 100vh; align-items: center; justify-content: center; overflow: hidden; padding: 24px 16px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #333; font-family: Poppins,sans-serif; }
.background-blob { position: fixed; z-index: 0; border-radius: 50%; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; background: white; }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; background: white; transform: rotate(30deg); }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.glass-panel { position: relative; z-index: 1; width: 100%; max-width: 500px; padding: 42px; border: 1px solid rgba(255,255,255,.6); border-radius: 30px; background: rgba(255,255,255,.8); box-shadow: 0 20px 60px rgba(0,0,0,.08); backdrop-filter: blur(20px); }
.back-row { margin-bottom: 20px; }
.back-btn { display: inline-flex; width: 40px; height: 40px; align-items: center; justify-content: center; border: 1px solid rgba(0,0,0,.05); border-radius: 50%; background: rgba(255,255,255,.5); color: #666; font-size: 1.1rem; text-decoration: none; transition: .3s; }
.back-btn:hover,.back-btn:focus-visible { width: 110px; border-radius: 20px; background: rgba(0,0,0,.05); color: #2d3436; }
.back-text { max-width: 0; margin-left: 0; overflow: hidden; opacity: 0; white-space: nowrap; font-size: .9rem; font-weight: 600; transition: .3s; }
.back-btn:hover .back-text,.back-btn:focus-visible .back-text { max-width: 100px; margin-left: 8px; opacity: 1; }
h1 { margin: 0 0 26px; color: #2d3436; font-size: 1.8rem; font-weight: 700; text-align: center; }
.page-state { padding: 30px; color: #666; text-align: center; }
.error-notice { display: flex; align-items: center; justify-content: center; gap: 8px; margin-bottom: 24px; padding: 15px; border: 1px solid #f5c6cb; border-radius: 15px; background: #f8d7da; color: #721c24; font-size: .95rem; text-align: center; }
.balance-info { margin-bottom: 30px; padding: 25px; border: 1px solid rgba(255,255,255,.8); border-radius: 20px; background: linear-gradient(135deg,#fff,#f8f9fa); box-shadow: 0 10px 30px rgba(0,0,0,.03); text-align: center; }
.balance-label { margin-bottom: 8px; color: #666; font-size: .85rem; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; }
.balance-amount { color: #2d3436; font-size: 2.2rem; font-weight: 800; }
.form-group { margin-bottom: 25px; }
.form-label { display: block; margin-bottom: 10px; color: #444; font-size: .95rem; font-weight: 600; }
.form-control { width: 100%; padding: 15px 20px; border: 2px solid transparent; border-radius: 15px; outline: none; background: rgba(255,255,255,.9); box-shadow: 0 4px 15px rgba(0,0,0,.02); font-size: 1rem; transition: .3s; }
.form-control:focus { border-color: #6c5ce7; background: white; box-shadow: 0 0 0 4px rgba(108,92,231,.1); }
.btn-submit { width: 100%; margin-top: 10px; padding: 16px; border: 0; border-radius: 50px; background: linear-gradient(45deg,#3498db,#2980b9); box-shadow: 0 10px 20px rgba(52,152,219,.3); color: white; cursor: pointer; font-size: 1.1rem; font-weight: 600; transition: .3s; }
.btn-submit:hover:not(:disabled) { box-shadow: 0 15px 25px rgba(52,152,219,.4); transform: translateY(-2px); }
.btn-submit:disabled { cursor: wait; opacity: .65; }
@media(max-width:520px) { .glass-panel { padding: 26px 18px; border-radius: 22px; } .balance-info { padding: 18px; } .balance-amount { font-size: 1.9rem; } }
</style>
