<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase } from '../api'

const role = ref('CUSTOMER')
const balance = ref('0.00')
const transactions = ref([])
const loading = ref(true)
const error = ref('')
const message = ref('')
const isMerchant = computed(() => role.value === 'MERCHANT')

function formatAmount(amount) {
  return Number(amount || 0).toFixed(2)
}

function transactionLabel(type) {
  return ({ TOPUP: 'Top Up', WITHDRAW: 'Withdraw', PURCHASE: 'Payment (Shopping)', SALES: 'Sales Revenue' })[type] || type
}

function isIncoming(type) {
  return ['TOPUP', 'SALES', 'REFUND_IN'].includes(type)
}

async function loadWallet() {
  const savedMessage = sessionStorage.getItem('walletMessage')
  if (savedMessage) {
    message.value = savedMessage
    sessionStorage.removeItem('walletMessage')
  }

  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!['CUSTOMER', 'MERCHANT'].includes(user.role)) {
      window.location.replace(user.role === 'ADMIN' ? '/admin/wallet/admin_approval.jsp' : '/public/login.jsp')
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
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load your wallet.')
    balance.value = result.data.balance
    transactions.value = result.data.transactions || []
    document.title = 'My Wallet - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your wallet.'
  } finally {
    loading.value = false
  }
}

onMounted(loadWallet)
</script>

<template>
  <div :class="['wallet-page', { 'merchant-theme': isMerchant }]">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>

    <main class="glass-panel">
      <div class="back-row">
        <a :href="isMerchant ? '/merchant/merchant_dashboard.jsp' : '/profile'" class="back-btn" :title="isMerchant ? 'Back to Dashboard' : 'Back to Profile'" aria-label="Go back">
          <i class="ri-arrow-left-line"></i><span class="back-text">{{ isMerchant ? 'Dashboard' : 'Back Profile' }}</span>
        </a>
      </div>

      <div v-if="message" class="wallet-message" role="status"><i class="ri-checkbox-circle-line"></i><span>{{ message }}</span></div>
      <div v-if="error" class="wallet-error" role="alert">{{ error }}</div>
      <div v-if="loading" class="wallet-state" aria-live="polite">Loading wallet...</div>
      <template v-else-if="!error">
        <header class="header-row">
          <div>
            <div class="balance-label">{{ isMerchant ? 'Shop Total Revenue' : 'Personal Balance' }}</div>
            <div class="balance-val">RM {{ formatAmount(balance) }}</div>
          </div>
          <span :class="['role-badge', isMerchant ? 'badge-merch' : 'badge-cust']">{{ role }}</span>
        </header>

        <div class="btn-group">
          <a v-if="!isMerchant" href="/public/wallet/topup.jsp" class="btn btn-primary"><i class="ri-add-circle-line"></i> Top Up Wallet</a>
          <a href="/public/wallet/withdraw.jsp" :class="['btn', isMerchant ? 'btn-purple' : 'btn-secondary']"><i class="ri-arrow-down-circle-line"></i>{{ isMerchant ? 'Withdraw Revenue' : 'Withdraw Funds' }}</a>
        </div>

        <section class="transactions" aria-labelledby="txn-title">
          <div class="txn-header-row"><h1 id="txn-title" class="txn-title">Transaction History</h1></div>
          <div v-if="!transactions.length" class="txn-item empty-state"><p>No transaction history found.</p></div>
          <div v-else class="txn-list">
            <article v-for="transaction in transactions" :key="transaction.id" class="txn-item">
              <div class="txn-copy">
                <div class="txn-left-main">
                  {{ transactionLabel(transaction.transactionType) }}
                  <span :class="['status-badge', 'status-' + (transaction.status || '').toLowerCase()]">{{ transaction.status }}</span>
                </div>
                <div class="txn-left-sub">{{ transaction.createdAt || '' }}</div>
                <div v-if="transaction.remarks" class="txn-remarks"><i class="ri-message-2-line"></i> Admin: “{{ transaction.remarks }}”</div>
              </div>
              <div :class="['txn-amount', isIncoming(transaction.transactionType) ? 'txn-amount-plus' : 'txn-amount-minus']">
                {{ isIncoming(transaction.transactionType) ? '+' : '-' }} RM {{ formatAmount(transaction.amount) }}
              </div>
            </article>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.wallet-page { position: relative; min-height: 100vh; overflow: hidden; padding: 1px 20px 50px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #333; font-family: Poppins,sans-serif; }
.background-blob { position: fixed; z-index: 0; border-radius: 50%; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; background: white; }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; background: white; transform: rotate(30deg); }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.merchant-theme .blob-yellow { background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.merchant-theme .blob-orange { background: white; box-shadow: none; }
.glass-panel { position: relative; z-index: 1; max-width: 1000px; margin: 60px auto; padding: 50px; border: 1px solid rgba(255,255,255,.6); border-radius: 30px; background: rgba(255,255,255,.75); box-shadow: 0 20px 60px rgba(0,0,0,.08); backdrop-filter: blur(20px); }
.back-row { margin-bottom: 30px; }
.back-btn { display: inline-flex; width: 40px; height: 40px; align-items: center; justify-content: center; border: 1px solid rgba(0,0,0,.05); border-radius: 50%; background: rgba(255,255,255,.5); color: #666; font-size: 1.1rem; text-decoration: none; transition: .3s; }
.back-btn:hover { width: 130px; border-radius: 20px; background: rgba(0,0,0,.05); color: #2d3436; }
.back-text { max-width: 0; margin-left: 0; overflow: hidden; opacity: 0; white-space: nowrap; font-size: .9rem; font-weight: 600; transition: .3s; }
.back-btn:hover .back-text,.back-btn:focus-visible .back-text { max-width: 100px; margin-left: 8px; opacity: 1; }
.wallet-message,.wallet-error { display: flex; align-items: center; gap: 10px; margin-bottom: 30px; padding: 15px 20px; border: 1px solid #c3e6cb; border-radius: 15px; background: #d4edda; color: #155724; }
.wallet-error { display: block; border-color: #f5c6cb; background: #f8d7da; color: #721c24; }
.wallet-state { padding: 45px; color: #666; text-align: center; }
.header-row { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 40px; }
.balance-label { margin-bottom: 5px; color: #666; font-size: .9rem; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }
.balance-val { color: #2d3436; font-size: 4rem; font-weight: 800; line-height: 1; }
.role-badge { padding: 8px 20px; border-radius: 999px; box-shadow: 0 8px 18px rgba(0,0,0,.18); color: #fff; font-size: .85rem; font-weight: 700; letter-spacing: .8px; text-transform: uppercase; }
.badge-cust { background: linear-gradient(145deg,#ffad33,#e68a00); }
.badge-merch { background: linear-gradient(135deg,#ffdb4d,#e6b800); }
.btn-group { display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 38px; }
.btn { display: inline-flex; min-height: 48px; align-items: center; justify-content: center; gap: 10px; padding: 14px 32px; border: 0; border-radius: 50px; font-size: 1rem; font-weight: 600; text-decoration: none; transition: .3s; }
.btn-primary { background: linear-gradient(45deg,#2ecc71,#27ae60); box-shadow: 0 10px 20px rgba(39,174,96,.35); color: #fff; }
.btn-secondary { border: 2px solid #e74c3c; background: #fff; color: #e74c3c; }
.btn-purple { background: linear-gradient(45deg,#6c5ce7,#a29bfe); box-shadow: 0 8px 20px rgba(108,92,231,.3); color: #fff; }
.btn:hover { transform: translateY(-2px); }
.txn-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.txn-title { margin: 0; color: #2d3436; font-size: 1.6rem; font-weight: 700; }
.txn-list { display: flex; flex-direction: column; gap: 18px; }
.txn-item { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 22px 30px; border-radius: 24px; background: linear-gradient(90deg,#fff,#fefaf3); box-shadow: 0 10px 30px rgba(0,0,0,.03); }
.empty-state { justify-content: center; padding: 40px; color: #888; }
.empty-state p { margin: 0; }
.txn-copy { min-width: 0; }
.txn-left-main { margin-bottom: 4px; color: #2d3436; font-size: 1rem; font-weight: 600; }
.status-badge { display: inline-block; margin-left: 5px; padding: 2px 8px; border-radius: 10px; font-size: .8rem; }
.status-approved { background: #d4edda; color: #155724; }
.status-pending { background: #fff3cd; color: #856404; }
.status-rejected { background: #f8d7da; color: #721c24; }
.txn-left-sub { color: #95a5a6; font-size: .85rem; }
.txn-remarks { margin-top: 6px; color: #666; font-size: .85rem; font-style: italic; overflow-wrap: anywhere; }
.txn-amount { flex: 0 0 auto; color: #2d3436; font-size: 1rem; font-weight: 700; }
.txn-amount-minus { color: #e74c3c; }
.txn-amount-plus { color: #27ae60; }
@media(max-width:700px) { .wallet-page { padding: 1px 12px 30px; } .glass-panel { margin: 20px auto; padding: 24px 18px; border-radius: 20px; } .header-row { align-items: flex-start; flex-direction: column; gap: 16px; margin-bottom: 28px; } .balance-val { font-size: 2.6rem; } .btn-group { gap: 10px; } .btn { flex: 1 1 100%; padding: 12px 18px; } .txn-title { font-size: 1.3rem; } .txn-item { align-items: flex-start; flex-direction: column; gap: 12px; padding: 18px; border-radius: 16px; } .txn-amount { align-self: flex-end; } }
</style>
