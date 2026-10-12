<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import AdminBackground from '../components/AdminBackground.vue'

const params = new URLSearchParams(window.location.search)
const currentView = ref(params.get('view') === 'history' ? 'history' : 'pending')
const currentType = ref(params.get('type') === 'WITHDRAW' ? 'WITHDRAW' : 'TOPUP')
const transactions = ref([])
const history = ref([])
const loading = ref(true)
const error = ref('')
const notification = ref('')
const notificationType = ref('success')
const actionModalOpen = ref(false)
const imageModalOpen = ref(false)
const selectedTransaction = ref(null)
const selectedAction = ref('approve')
const remarks = ref('')
const submitting = ref(false)
const previewUrl = ref('')
const previewAlt = ref('')

const heading = () => currentView.value === 'history' ? 'Audit History' : 'Pending Requests'

function approvalUrl(view, type = currentType.value) {
  const query = new URLSearchParams({ view })
  if (view !== 'history') query.set('type', type)
  return `/admin/wallet/admin_approval.jsp?${query}`
}

function imageUrl(path) {
  return path ? legacyPageUrl(`/${path.replace(/^\/+/, '')}`) : ''
}

async function loadApprovals() {
  const query = new URLSearchParams({ view: currentView.value, type: currentType.value })
  const response = await fetch(`${legacyBase}/admin/api/wallet-approvals?${query}`, {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })
  const result = await response.json().catch(() => null)
  if (response.status === 401) {
    window.location.replace('/public/login.jsp')
    return
  }
  if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load wallet approvals.')
  transactions.value = result.data.transactions || []
  history.value = result.data.history || []
  if (result.data.message) {
    notification.value = result.data.message
    notificationType.value = 'success'
  }
  if (result.data.error) error.value = result.data.error
}

async function loadPage() {
  try {
    const session = await getCurrentUser()
    if (!session.authenticated || session.role !== 'ADMIN') {
      window.location.replace('/public/login.jsp')
      return
    }
    document.title = 'Admin Approval - PrimeGo'
    await loadApprovals()
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load wallet approvals.'
  } finally {
    loading.value = false
  }
}

function openActionModal(transaction, action) {
  selectedTransaction.value = transaction
  selectedAction.value = action
  remarks.value = ''
  actionModalOpen.value = true
}

function closeActionModal() {
  actionModalOpen.value = false
}

function showImage(path, type) {
  previewUrl.value = imageUrl(path)
  previewAlt.value = type === 'WITHDRAW' ? 'Withdrawal QR code' : 'Payment receipt'
  imageModalOpen.value = true
}

async function submitAction() {
  if (!selectedTransaction.value) return
  submitting.value = true
  error.value = ''
  const action = selectedAction.value
  const body = new URLSearchParams({
    id: selectedTransaction.value.id,
    action,
    remarks: remarks.value,
  })
  try {
    const response = await fetch(`${legacyBase}/WalletAdminServlet`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body,
    })
    const result = await response.json().catch(() => null)
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to update transaction.')
    notification.value = result.msg
    notificationType.value = 'success'
    actionModalOpen.value = false
    await loadApprovals()
  } catch (actionError) {
    notification.value = actionError.message || 'Unable to update transaction.'
    notificationType.value = 'error'
  } finally {
    submitting.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <div class="admin-wallet-approvals-page">
    <AdminBackground />

    <main class="glass-panel approval-panel">
      <div class="back-row">
        <a class="back-btn" href="/admin/dashboard" title="Back to Dashboard" aria-label="Back to Dashboard">
          <i class="ri-arrow-left-line"></i><span class="back-text">Dashboard</span>
        </a>
      </div>

      <div v-if="notification" :class="['notification', notificationType]" role="status">{{ notification }}</div>
      <div v-if="error" class="notification error" role="alert">{{ error }}</div>

      <header class="header-row">
        <div>
          <div class="balance-label">Audit Dashboard</div>
          <h1>{{ heading() }}</h1>
        </div>
        <span class="role-badge badge-admin">ADMIN</span>
      </header>

      <nav class="view-tabs" aria-label="Approval views">
        <a :class="['view-tab', { active: currentView !== 'history' }]" :href="approvalUrl('pending')">Pending</a>
        <a :class="['view-tab', { active: currentView === 'history' }]" :href="approvalUrl('history')">History</a>
      </nav>

      <div v-if="currentView !== 'history'" class="action-buttons" aria-label="Transaction type">
        <a :class="['btn-filter', 'btn-filter-topup', { active: currentType === 'TOPUP', inactive: currentType !== 'TOPUP' }]" :href="approvalUrl('pending', 'TOPUP')">Top Up Requests</a>
        <a :class="['btn-filter', 'btn-filter-withdraw', { active: currentType === 'WITHDRAW', inactive: currentType !== 'WITHDRAW' }]" :href="approvalUrl('pending', 'WITHDRAW')">Withdraw Requests</a>
      </div>

      <div v-if="loading" class="empty-state" aria-live="polite">Loading requests...</div>
      <div v-else-if="currentView === 'history'" class="txn-list">
        <div v-if="history.length === 0" class="txn-item empty-state">
          <i class="ri-history-line empty-icon"></i><p>No audit history found.</p>
        </div>
        <article v-for="log in history" :key="log.id" class="txn-item">
          <div class="txn-details">
            <div class="txn-left-main">
              {{ log.actionType }}
              <span class="txn-id">(Txn ID: {{ log.walletTransactionId }})</span>
            </div>
            <div class="txn-left-sub">
              By Admin: <strong>{{ log.adminName || '-' }}</strong> · {{ log.createdAt || '-' }}<br>
              Status: {{ log.previousStatus || '-' }} → <strong>{{ log.currentStatus || '-' }}</strong><br>
              <span class="history-detail">Amount: <strong>{{ log.amount === null ? 'N/A' : `RM ${log.amount}` }}</strong></span>
              <span class="history-detail">User ID: <strong>{{ log.userId ?? 'N/A' }}</strong></span>
              <span v-if="log.remarks" class="history-remarks"><br>Remarks: “{{ log.remarks }}”</span>
              <button v-if="log.receiptImage" class="receipt-link" type="button" @click="showImage(log.receiptImage, log.transactionType)">
                <i :class="log.transactionType === 'WITHDRAW' ? 'ri-qr-code-line' : 'ri-image-line'"></i>View Image
              </button>
            </div>
          </div>
          <div class="txn-right">
            <span :class="['status-pill', log.currentStatus === 'APPROVED' ? 'approved' : 'rejected']">{{ log.currentStatus }}</span>
          </div>
        </article>
      </div>
      <div v-else class="txn-list">
        <div v-if="transactions.length === 0" class="txn-item empty-state">
          <i class="ri-inbox-line empty-icon"></i><p>No pending requests found.</p>
        </div>
        <article v-for="transaction in transactions" :key="transaction.id" class="txn-item">
          <div class="txn-details">
            <div class="txn-left-main">
              {{ transaction.transactionType === 'TOPUP' ? 'Top Up Request' : 'Withdraw Request' }}
              <span class="txn-id">(ID: {{ transaction.id }})</span>
            </div>
            <div class="txn-left-sub">
              User ID: {{ transaction.userId }} · {{ transaction.createdAt || '-' }}
              <button v-if="transaction.receiptImage" class="receipt-link" type="button" @click="showImage(transaction.receiptImage, transaction.transactionType)">
                <i :class="transaction.transactionType === 'WITHDRAW' ? 'ri-qr-code-line' : 'ri-image-line'"></i>
                {{ transaction.transactionType === 'WITHDRAW' ? 'View QR Code' : 'View Receipt' }}
              </button>
            </div>
          </div>
          <div class="txn-right">
            <strong :class="['txn-amount', transaction.transactionType === 'TOPUP' ? 'positive' : 'negative']">
              {{ transaction.transactionType === 'TOPUP' ? '+' : '-' }} RM {{ transaction.amount }}
            </strong>
            <button class="admin-action-btn btn-reject" type="button" @click="openActionModal(transaction, 'reject')">Reject</button>
            <button class="admin-action-btn btn-approve" type="button" @click="openActionModal(transaction, 'approve')">Approve</button>
          </div>
        </article>
      </div>
    </main>

    <div v-if="imageModalOpen" class="modal-overlay" @click.self="imageModalOpen = false">
      <div class="image-modal" role="dialog" aria-modal="true" aria-label="Transaction image">
        <button class="modal-close" type="button" aria-label="Close image preview" @click="imageModalOpen = false">&times;</button>
        <img class="modal-img" :src="previewUrl" :alt="previewAlt">
      </div>
    </div>

    <div v-if="actionModalOpen" class="modal-overlay" @click.self="closeActionModal">
      <section class="modal-content" role="dialog" aria-modal="true" aria-labelledby="action-title">
        <button class="modal-close" type="button" aria-label="Close approval dialog" @click="closeActionModal">&times;</button>
        <h2 id="action-title" :class="selectedAction">{{ selectedAction === 'approve' ? 'Approve Request' : 'Reject Request' }}</h2>
        <div v-if="selectedTransaction" class="transaction-summary">
          <div><span>User ID:</span><strong>{{ selectedTransaction.userId }}</strong></div>
          <div><span>Amount:</span><strong>RM {{ selectedTransaction.amount }}</strong></div>
          <div><span>Time:</span><strong>{{ selectedTransaction.createdAt }}</strong></div>
        </div>
        <form @submit.prevent="submitAction">
          <label for="approval-remarks">Please provide remarks for this action. This will be recorded in the audit log.</label>
          <textarea id="approval-remarks" v-model="remarks" class="modal-textarea" :placeholder="selectedAction === 'approve' ? 'Optional remarks...' : 'Reason for rejection (Required)...'" :required="selectedAction === 'reject'" rows="4"></textarea>
          <div class="modal-actions">
            <button class="btn-filter cancel-action" type="button" @click="closeActionModal">Cancel</button>
            <button :class="['btn-filter', 'confirm-action', selectedAction]" type="submit" :disabled="submitting">
              {{ submitting ? 'Submitting...' : `Confirm ${selectedAction === 'approve' ? 'Approve' : 'Reject'}` }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.admin-wallet-approvals-page,.admin-wallet-approvals-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-wallet-approvals-page { position: relative; z-index: 1; min-height: 100vh; overflow-x: hidden; padding: 1px 20px 40px; color: #333; }
.approval-panel { position: relative; z-index: 1; width: 100%; max-width: 1000px; margin: 60px auto; padding: 50px; border: 1px solid rgba(255,255,255,.6); border-radius: 30px; background: rgba(255,255,255,.75); box-shadow: 0 20px 60px rgba(0,0,0,.08); backdrop-filter: blur(20px); }
.back-row { margin-bottom: 30px; }
.back-btn { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; border: 1px solid rgba(0,0,0,.05); border-radius: 50%; background: rgba(255,255,255,.5); color: #666; font-size: 1.1rem; text-decoration: none; transition: all .3s ease; }
.back-btn:hover { width: 130px; border-radius: 20px; background: rgba(0,0,0,.05); color: #2d3436; }
.back-text { max-width: 0; margin-left: 0; overflow: hidden; opacity: 0; font-size: .9rem; font-weight: 600; white-space: nowrap; transition: all .3s ease; }
.back-btn:hover .back-text { max-width: 100px; margin-left: 8px; opacity: 1; }
.header-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.balance-label { margin-bottom: 5px; color: #666; font-size: .9rem; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }
h1 { margin: 10px 0 0; color: #2d3436; font-size: 2rem; font-weight: 800; overflow-wrap: anywhere; }
.role-badge { padding: 8px 20px; border-radius: 999px; box-shadow: 0 8px 18px rgba(0,0,0,.18); color: white; font-size: .85rem; font-weight: 700; letter-spacing: .8px; text-transform: uppercase; }
.badge-admin { background: linear-gradient(135deg,#ff5e55,#d92e25); }
.view-tabs { display: flex; width: fit-content; gap: 4px; margin-bottom: 20px; padding: 5px; border-radius: 50px; background: rgba(255,255,255,.5); }
.view-tab { padding: 8px 20px; border-radius: 40px; color: #666; font-weight: 600; text-decoration: none; transition: all .3s; }
.view-tab:hover { background: rgba(255,255,255,.75); }
.view-tab.active { background: linear-gradient(135deg,#ff5e55,#d92e25); color: #fff; }
.action-buttons { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 30px; }
.btn-filter { display: inline-flex; align-items: center; justify-content: center; padding: 12px 24px; border: 2px solid transparent; border-radius: 50px; font-size: 1rem; font-weight: 600; text-decoration: none; transition: all .3s ease; }
.btn-filter-topup { border-color: #2ecc71; background: #2ecc71; box-shadow: 0 4px 15px rgba(46,204,113,.3); color: #fff; }
.btn-filter-topup:hover,.btn-filter-topup.active { transform: translateY(-2px); border-color: #27ae60; background: #27ae60; }
.btn-filter-withdraw { border-color: #e74c3c; background: transparent; color: #e74c3c; }
.btn-filter-withdraw:hover,.btn-filter-withdraw.active { transform: translateY(-2px); background: #e74c3c; box-shadow: 0 4px 15px rgba(231,76,60,.2); color: #fff; }
.btn-filter.inactive { box-shadow: none; opacity: .55; }
.txn-list { display: flex; flex-direction: column; gap: 18px; }
.txn-item { display: flex; align-items: center; justify-content: space-between; gap: 22px; padding: 22px 30px; border-radius: 24px; background: linear-gradient(90deg,#fff,#fefaf3); box-shadow: 0 10px 30px rgba(0,0,0,.03); }
.txn-details { min-width: 0; }
.txn-left-main { margin-bottom: 4px; color: #2d3436; font-size: 1rem; font-weight: 600; overflow-wrap: anywhere; }
.txn-id { color: #666; font-size: .9rem; font-weight: 400; }
.txn-left-sub { color: #95a5a6; font-size: .85rem; line-height: 1.8; overflow-wrap: anywhere; }
.txn-left-sub strong { color: #555; }
.history-detail { display: inline-block; margin-right: 10px; color: #555; }
.history-remarks { color: #666; font-style: italic; }
.receipt-link { display: inline-flex; align-items: center; gap: 5px; margin-top: 5px; padding: 0; border: 0; background: none; color: #3498db; cursor: pointer; font: inherit; text-decoration: none; }
.receipt-link:hover { text-decoration: underline; }
.txn-right { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; color: #2d3436; font-size: 1rem; font-weight: 700; }
.txn-amount { margin-right: 5px; font-size: 1.1rem; white-space: nowrap; }
.positive { color: #2ecc71; }
.negative { color: #e74c3c; }
.admin-action-btn { display: inline-block; padding: 8px 14px; border: 0; border-radius: 999px; color: #fff; cursor: pointer; font-size: .85rem; font-weight: 600; text-decoration: none; white-space: nowrap; }
.btn-approve { background: #2ecc71; }
.btn-reject { background: #e74c3c; }
.empty-state { justify-content: center; color: #888; text-align: center; }
.empty-state p { margin: 0; }
.empty-icon { margin-bottom: 8px; font-size: 3rem; opacity: .5; }
.notification { display: flex; align-items: center; gap: 10px; margin-bottom: 24px; padding: 15px 20px; border: 1px solid; border-radius: 15px; }
.notification.success { border-color: #c3e6cb; background: #d4edda; color: #155724; }
.notification.error { border-color: #f5c6cb; background: #f8d7da; color: #721c24; }
.modal-overlay { position: fixed; z-index: 20; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(0,0,0,.6); backdrop-filter: blur(5px); }
.modal-content,.image-modal { position: relative; width: min(500px,90vw); max-height: 90vh; overflow-y: auto; padding: 30px; border-radius: 20px; background: rgba(255,255,255,.9); box-shadow: 0 10px 30px rgba(0,0,0,.2); animation: slideUp .3s ease; }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.image-modal { display: flex; max-width: 90vw; align-items: center; justify-content: center; padding: 20px; background: transparent; box-shadow: none; }
.modal-close { position: absolute; z-index: 1; top: 12px; right: 16px; border: 0; background: transparent; color: #666; cursor: pointer; font-size: 1.5rem; }
.modal-img { display: block; max-width: min(100%,900px); max-height: 80vh; border-radius: 10px; object-fit: contain; }
.modal-content h2 { margin: 0 30px 15px 0; color: #2d3436; font-size: 1.4rem; }
.modal-content h2.approve { color: #2ecc71; }
.modal-content h2.reject { color: #e74c3c; }
.transaction-summary { margin-bottom: 20px; padding: 15px; border-radius: 10px; background: #f8f9fa; font-size: .9rem; }
.transaction-summary div { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 5px; }
.transaction-summary div:last-child { margin-bottom: 0; }
.transaction-summary span { color: #666; }
.transaction-summary strong { text-align: right; overflow-wrap: anywhere; }
.modal-content form label { display: block; margin-bottom: 10px; color: #666; font-size: .9rem; }
.modal-textarea { width: 100%; min-height: 100px; margin-bottom: 20px; padding: 12px; border: 1px solid #ddd; border-radius: 10px; font: inherit; resize: vertical; }
.modal-textarea:focus { border-color: #d63031; outline: 2px solid rgba(214,48,49,.15); }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
.cancel-action { border: 1px solid #ddd; background: transparent; color: #555; cursor: pointer; }
.confirm-action { border: 0; color: white; cursor: pointer; }
.confirm-action.approve { background: #2ecc71; }
.confirm-action.reject { background: #e74c3c; }
.confirm-action:disabled { cursor: wait; opacity: .7; }
@media(max-width:760px) {
  .approval-panel { margin: 24px auto; padding: 30px 22px; border-radius: 22px; }
  .txn-item { align-items: flex-start; flex-direction: column; padding: 20px; border-radius: 18px; }
  .txn-right { width: 100%; flex-wrap: wrap; }
}
@media(max-width:480px) {
  .admin-wallet-approvals-page { padding: 1px 10px 24px; }
  .approval-panel { padding: 24px 16px; }
  .header-row { align-items: flex-start; flex-direction: column; }
  h1 { font-size: 1.65rem; }
  .action-buttons { flex-direction: column; }
  .action-buttons .btn-filter { width: 100%; }
  .txn-right { align-items: flex-start; }
  .txn-amount { width: 100%; }
  .modal-content { padding: 26px 18px 20px; }
  .modal-actions { justify-content: stretch; }
  .modal-actions .btn-filter { flex: 1; padding: 11px 12px; }
}
</style>
