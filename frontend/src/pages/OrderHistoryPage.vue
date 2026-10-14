<script setup>
import { onMounted, ref } from 'vue'
import { legacyBase, productImageUrl } from '../api'
import AccountSidebar from '../components/AccountSidebar.vue'

const statusOptions = [
  ['ALL', 'All'],
  ['PAID', 'To Ship'],
  ['SHIPPED', 'To Receive'],
  ['COMPLETED', 'Completed'],
  ['RETURNS', 'Returns'],
  ['CANCELLED', 'Cancelled'],
]
const status = new URLSearchParams(window.location.search).get('status') || 'ALL'
const orders = ref([])
const loading = ref(true)
const loadError = ref('')
const busy = ref(false)
const message = ref('')
const messageType = ref('success')
const refundOrder = ref(null)
const refundType = ref('MONEY_ONLY')
const refundReason = ref('')
const returnTrackingNumbers = ref({})
const confirmation = ref(null)
const confirmedAction = ref(null)

function setMessage(text, type = 'error') {
  message.value = text
  messageType.value = type
}

async function loadOrders() {
  loading.value = true
  try {
    const response = await fetch(`${legacyBase}/customer/orders?status=${encodeURIComponent(status)}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!result || !response.ok || result.code !== 200) throw new Error(result?.msg || 'Unable to load your orders.')
    loadError.value = ''
    updateOrders(result.data)
    document.title = 'My Orders - PrimeGo'
  } catch (error) {
    loadError.value = error.message || 'Unable to load your orders.'
  } finally {
    loading.value = false
  }
}

function updateOrders(data) {
  orders.value = data.orders || []
  if (data.message) setMessage(data.message, data.messageType || 'success')
}

async function sendAction(action, orderId, fields = {}) {
  busy.value = true
  const body = new URLSearchParams({ action, orderId: String(orderId), status, ...fields })
  try {
    const response = await fetch(`${legacyBase}/customer/orders`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body,
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!result || !response.ok || result.code !== 200) throw new Error(result?.msg || 'Unable to update this order.')
    updateOrders(result.data)
  } catch (error) {
    setMessage(error.message || 'Unable to update this order.')
  } finally {
    busy.value = false
  }
}

function askConfirmation(title, text, action) {
  confirmation.value = { title, text }
  confirmedAction.value = action
}

async function runConfirmedAction() {
  const action = confirmedAction.value
  confirmation.value = null
  confirmedAction.value = null
  await action?.()
}

function openRefundModal(order) {
  refundOrder.value = order
  refundType.value = 'MONEY_ONLY'
  refundReason.value = ''
}

async function submitRefund() {
  if (!refundOrder.value) return
  const orderId = refundOrder.value.id
  await sendAction('processRefundRequest', orderId, { refundType: refundType.value, reason: refundReason.value })
  refundOrder.value = null
}

function submitReturnTracking(order) {
  const trackingNumber = (returnTrackingNumbers.value[order.id] || '').trim()
  if (!trackingNumber) {
    setMessage('Please enter return tracking number.')
    return
  }
  askConfirmation('Confirm Shipping', 'Submit return tracking number and mark as shipped?', () =>
    sendAction('confirmReturnShipped', order.id, { returnTrackingNumber: trackingNumber }))
}

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

function statusClass(order) {
  if (['SHIPPED', 'COMPLETED'].includes(order.orderStatus) && order.rejectionCount > 0) return 'status-rejected'
  return 'status-' + (order.orderStatus || '').toLowerCase().replaceAll('_', '-')
}

function statusLabel(order) {
  if (['SHIPPED', 'COMPLETED'].includes(order.orderStatus) && order.rejectionCount > 0) return 'Rejected (Action Required)'
  return (order.orderStatus || '').replaceAll('_', ' ')
}

function returnSupportUrl(orderId) {
  return `https://wa.me/60123456789?text=${encodeURIComponent('Arranging return for Order ' + orderId)}`
}

onMounted(loadOrders)
</script>

<template>
  <div class="orders-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>

    <AccountSidebar active="orders" />

    <main class="main-content">
      <header class="header-section"><h1>Order History</h1></header>

      <div v-if="message" :class="['notice', 'notice-' + messageType]" role="status">{{ message }}</div>

      <nav class="order-tabs" aria-label="Order status">
        <a
          v-for="[value, label] in statusOptions"
          :key="value"
          :href="'/customer/orders?status=' + value"
          :class="['order-tab', { active: status === value }]"
        >{{ label }}</a>
      </nav>

      <div v-if="loading" class="empty-orders" aria-live="polite">Loading orders...</div>
      <div v-else-if="loadError" class="glass-panel empty-orders" role="alert">{{ loadError }}</div>
      <section v-else-if="!orders.length" class="glass-panel empty-orders">
        <i class="ri-shopping-cart-line empty-cart-icon"></i>
        <p>You haven't placed any orders yet.</p>
        <a href="/" class="btn-edit">Start Shopping</a>
      </section>

      <div v-else class="orders-list">
        <article v-for="order in orders" :key="order.id" class="glass-panel order-card">
          <header class="order-header">
            <div>
              <h2>Order #{{ order.id }}</h2>
              <span class="placed-date">Placed on: {{ order.createdAt || '' }}</span>
            </div>
            <div class="order-status-area">
              <span :class="['status-badge', statusClass(order)]">{{ statusLabel(order) }}</span>
              <div v-if="order.trackingNumber" class="tracking-number"><i class="ri-truck-line"></i> Tracking: <strong>{{ order.trackingNumber }}</strong></div>
            </div>
          </header>

          <div class="order-items">
            <a
              v-for="item in order.items"
              :key="item.productId"
              :href="'/customer/product/product_detail.jsp?id=' + item.productId"
              class="order-item"
            >
              <img :src="productImageUrl(item.productImageUrl)" :alt="item.productName" @error="imageFailed" />
              <div class="item-copy"><strong>{{ item.productName }}</strong><span>x{{ item.quantity }}</span></div>
              <strong class="item-subtotal">${{ item.subtotal }}</strong>
            </a>
          </div>

          <div class="order-footer">
            <div class="shipping-copy">
              <span>Shipping to:</span>
              <p>{{ order.address }}</p>
              <section v-if="order.refundStatus === 'WAITING_RETURN'" class="return-address-card">
                <span class="return-badge"><i class="ri-check-line"></i> Merchant Agreed</span>
                <div v-if="order.returnAddress" class="address-content">
                  <strong><i class="ri-map-pin-line"></i> Return Address</strong>
                  <p>{{ order.returnAddress }}</p>
                </div>
                <small v-if="order.returnAddress"><i class="ri-arrow-right-line"></i> Ship item here &amp; update tracking on the right.</small>
              </section>
            </div>

            <div class="order-total-actions">
              <span class="total-label">Total Amount</span>
              <strong class="order-total">${{ order.totalAmount }}</strong>
              <div class="order-actions">
                <button
                  v-if="order.orderStatus === 'PAID'"
                  class="action-button cancel-action"
                  type="button"
                  :disabled="busy"
                  @click="askConfirmation('Cancel Order', 'Are you sure you want to cancel this order?', () => sendAction('cancelOrder', order.id))"
                ><i class="ri-close-circle-line"></i> Cancel Order</button>

                <template v-if="order.refundStatus === 'WAITING_RETURN'">
                  <form class="return-shipment-form" @submit.prevent="submitReturnTracking(order)">
                    <div class="tracking-input-group">
                      <input v-model="returnTrackingNumbers[order.id]" class="tracking-input" type="text" placeholder="Return tracking no." required />
                      <button class="btn-ship-action" type="submit" :disabled="busy">I Have Shipped</button>
                    </div>
                  </form>
                  <a :href="returnSupportUrl(order.id)" target="_blank" rel="noopener noreferrer" class="btn-whatsapp-action">
                    <i class="ri-whatsapp-line"></i> Contact to Ship
                  </a>
                </template>
                <button v-else-if="order.refundStatus === 'RETURN_SHIPPED'" class="action-button disabled-action" type="button" disabled>
                  <i class="ri-truck-line"></i> Return Shipped - Wait Merchant
                </button>
                <button v-else-if="order.orderStatus === 'RETURN_REQUESTED'" class="action-button disabled-action" type="button" disabled>
                  <i class="ri-time-line"></i> Wait for Approval
                </button>
                <template v-else-if="order.rejectionCount >= 2">
                  <a
                    :href="'https://wa.me/6001114489511?text=' + encodeURIComponent('Order ID: ' + order.id + '. My return was rejected twice.')"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="btn-whatsapp-action"
                  ><i class="ri-whatsapp-line"></i> Contact Support</a>
                  <div class="reject-reason">Reason: {{ order.merchantRejectReason }}</div>
                </template>
                <template v-else-if="order.orderStatus === 'SHIPPED'">
                  <div v-if="order.canReturn" class="refund-action-wrap">
                    <button class="action-button refund-action" type="button" :disabled="busy" @click="openRefundModal(order)">
                      <i class="ri-refund-line"></i> {{ order.rejectionCount > 0 ? 'Apply Again' : 'Request Refund' }}
                    </button>
                    <div v-if="order.rejectionCount > 0" class="reject-reason">Rejected once: {{ order.merchantRejectReason }}</div>
                  </div>
                  <button
                    class="action-button receipt-action"
                    type="button"
                    :disabled="busy"
                    @click="askConfirmation('Confirm Receipt', 'Are you sure you want to confirm receipt?', () => sendAction('confirmReceipt', order.id))"
                  ><i class="ri-check-double-line"></i> Confirm Receipt</button>
                </template>
              </div>
            </div>
          </div>
        </article>
      </div>
    </main>

    <div v-if="refundOrder" class="modal-overlay" @click.self="refundOrder = null" @keydown.esc="refundOrder = null">
      <section class="modal-box" role="dialog" aria-modal="true" aria-labelledby="refund-title">
        <header class="modal-header">
          <h2 id="refund-title">Request Refund</h2>
          <button type="button" class="modal-close" aria-label="Close" @click="refundOrder = null">&times;</button>
        </header>
        <form @submit.prevent="submitRefund">
          <label for="refund-type">Refund Type:</label>
          <select id="refund-type" v-model="refundType" class="form-control">
            <option value="MONEY_ONLY">Refund only (no return)</option>
            <option value="RETURN_AND_REFUND">Return &amp; refund</option>
          </select>
          <label for="refund-reason">Return Reason (Optional):</label>
          <textarea id="refund-reason" v-model="refundReason" class="form-textarea" rows="4" placeholder="e.g. Item not as described..."></textarea>
          <p class="refund-note"><i class="ri-information-fill"></i> This return request is allowed within 7 days of shipping.</p>
          <button class="btn-submit-refund" type="submit" :disabled="busy">{{ busy ? 'Submitting...' : 'Submit Request' }}</button>
        </form>
      </section>
    </div>

    <div v-if="confirmation" class="modal-overlay" @click.self="confirmation = null" @keydown.esc="confirmation = null">
      <section class="confirm-box" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div class="confirm-icon"><i class="ri-question-line"></i></div>
        <h2 id="confirm-title">{{ confirmation.title }}</h2>
        <p>{{ confirmation.text }}</p>
        <div class="confirm-actions">
          <button type="button" class="confirm-cancel" :disabled="busy" @click="confirmation = null">Cancel</button>
          <button type="button" class="confirm-submit" :disabled="busy" @click="runConfirmedAction">{{ busy ? 'Processing...' : 'Yes, Proceed' }}</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.orders-page { display: flex; min-height: 100vh; position: relative; overflow-x: hidden; color: #333; }
.background-blob { position: fixed; z-index: -1; border-radius: 50%; pointer-events: none; }
.blob-red,.blob-yellow { background: white; }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.main-content { width: calc(100% - 250px); min-width: 0; margin-left: 250px; flex: 1; padding: 40px; }
.header-section { display: flex; align-items: center; justify-content: space-between; margin-bottom: 30px; }
.header-section h1 { display: inline-block; margin: 0; padding: 10px 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 50px; background: rgba(255,255,255,.6); color: #e68a00; font-size: 2.2rem; box-shadow: 0 4px 15px rgba(0,0,0,.05); backdrop-filter: blur(10px); }
.notice { margin-bottom: 20px; padding: 15px; border-radius: 10px; }
.notice-success { background: #d4edda; color: #155724; }
.notice-error { background: #f8d7da; color: #721c24; }
.order-tabs { display: flex; gap: 10px; margin-bottom: 20px; padding-bottom: 5px; overflow-x: auto; }
.order-tab { flex: 0 0 auto; padding: 8px 20px; border: 1px solid transparent; border-radius: 20px; background: rgba(255,255,255,.5); color: #555; font-size: .9rem; font-weight: 600; text-decoration: none; transition: .2s; }
.order-tab:hover { background: rgba(255,255,255,.8); }
.order-tab.active { background: #e68a00; color: white; box-shadow: 0 4px 10px rgba(230,138,0,.3); }
.orders-list { display: flex; flex-direction: column; gap: 20px; }
.glass-panel { margin: 0; padding: 25px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); backdrop-filter: blur(20px); box-shadow: 0 8px 32px rgba(0,0,0,.1); }
.order-card { border-left: 5px solid #e68a00; }
.order-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,.05); }
.order-header h2 { margin: 0 0 4px; color: #2d3436; font-size: 1.1rem; }
.placed-date { color: #888; font-size: .85rem; }
.order-status-area { display: flex; flex-direction: column; align-items: flex-end; text-align: right; }
.status-badge { padding: 5px 12px; border-radius: 15px; font-size: .8rem; font-weight: 600; }
.status-rejected { background: #ffeaa7; color: #d35400; }
.status-completed { background: #d4edda; color: #155724; }
.status-pending { background: #fff3cd; color: #856404; }
.status-shipped { background: #dbeafe; color: #1e40af; }
.status-return-requested { background: #ffeaa7; color: #d35400; }
.status-refunded { background: #fab1a0; color: #c0392b; }
.status-cancelled { background: #f8d7da; color: #721c24; }
.status-paid { background: #e2e3e5; color: #383d41; }
.tracking-number { margin-top: 8px; color: #555; font-size: .85rem; }
.order-items { margin-bottom: 15px; padding: 5px 10px; border-radius: 10px; background: rgba(255,255,255,.5); }
.order-item { display: flex; align-items: center; gap: 15px; padding: 10px; border-bottom: 1px solid rgba(0,0,0,.05); color: inherit; text-decoration: none; transition: .2s; }
.order-item:last-child { border-bottom: 0; }
.order-item:hover { background: rgba(255,255,255,.8); }
.order-item img { width: 60px; height: 60px; flex: 0 0 60px; border: 1px solid #eee; border-radius: 8px; object-fit: cover; }
.item-copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }
.item-copy strong { overflow-wrap: anywhere; color: #333; font-size: .95rem; }
.item-copy span { color: #888; font-size: .8rem; }
.item-subtotal { color: #e68a00; white-space: nowrap; }
.order-footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-top: 10px; }
.shipping-copy { max-width: 60%; min-width: 0; }
.shipping-copy > span,.total-label { color: #666; font-size: .9rem; }
.shipping-copy > p { margin: 5px 0 0; color: #333; font-size: .9rem; line-height: 1.4; overflow-wrap: anywhere; }
.return-address-card { position: relative; margin-top: 15px; padding: 15px; border: 1px solid #ffe0b2; border-radius: 12px; background: #fff8e1; box-shadow: 0 4px 12px rgba(230,126,34,.05); }
.return-badge { display: inline-flex; align-items: center; gap: 5px; margin-bottom: 10px; padding: 4px 10px; border-radius: 20px; background: #ff9800; color: white; font-size: .75rem; font-weight: 600; }
.address-content { padding: 10px; border: 1px dashed #ffcc80; border-radius: 8px; background: rgba(255,255,255,.6); color: #5d4037; font-size: .9rem; line-height: 1.5; overflow-wrap: anywhere; }
.address-content strong { display: block; margin-bottom: 5px; color: #e65100; }
.address-content p { margin: 0; }
.return-address-card small { display: flex; align-items: center; gap: 5px; margin-top: 8px; color: #8d6e63; font-size: .75rem; }
.order-total-actions { display: flex; min-width: 280px; max-width: 350px; flex-direction: column; align-items: flex-end; text-align: right; }
.order-total { margin: 2px 0 10px; color: #e68a00; font-size: 1.3rem; }
.order-actions { display: flex; width: 100%; flex-wrap: wrap; align-items: flex-start; justify-content: flex-end; gap: 10px; }
.action-button,.btn-ship-action { padding: 8px 15px; border-radius: 10px; cursor: pointer; font-size: .85rem; font-weight: 600; transition: .2s; }
.action-button:disabled,.btn-ship-action:disabled { opacity: .6; cursor: wait; }
.cancel-action { border: 1px solid #ff6b6b; background: transparent; color: #ff6b6b; }
.refund-action { display: inline-flex; align-items: center; gap: 5px; border: 1px solid #e74c3c; background: transparent; color: #e74c3c; }
.receipt-action,.confirm-submit { border: 0; background: #2d3436; color: white; }
.disabled-action { border: 0; background: #eee; color: #888; cursor: not-allowed; }
.status-return-shipped,.status-return-requested { background: #ffeaa7; color: #d35400; }
.return-shipment-form,.refund-action-wrap { width: 100%; }
.tracking-input-group { display: flex; width: 100%; gap: 8px; margin-bottom: 10px; padding: 5px; border: 1px solid #eee; border-radius: 12px; background: white; box-shadow: 0 2px 10px rgba(0,0,0,.03); }
.tracking-input { min-width: 0; flex: 1; padding: 10px 12px; border: 0; border-radius: 8px; background: transparent; outline: none; font: inherit; font-size: .9rem; }
.tracking-input:focus { background: #fafafa; }
.btn-ship-action { border: 0; background: #2d3436; color: white; white-space: nowrap; }
.btn-ship-action:hover { background: #000; }
.btn-whatsapp-action { display: flex; width: 100%; height: 42px; align-items: center; justify-content: center; gap: 8px; margin-top: 5px; padding: 0 15px; border: 1px solid #c8e6c9; border-radius: 10px; background: #e8f5e9; color: #2e7d32; font-size: .9rem; font-weight: 600; text-decoration: none; transition: .2s; }
.btn-whatsapp-action:hover { border-color: #25d366; background: #25d366; color: white; }
.reject-reason { width: 100%; margin-top: 5px; color: #c0392b; font-size: .8rem; text-align: right; overflow-wrap: anywhere; }
.refund-action-wrap .reject-reason { color: #e67e22; }
.empty-orders { color: #666; text-align: center; }
.empty-orders p { margin: 0 0 18px; font-size: 1.1rem; }
.empty-cart-icon { display: block; margin-bottom: 10px; color: #ddd; font-size: 3rem; }
.btn-edit { display: inline-block; padding: 10px 25px; border: 0; border-radius: 20px; background: #e68a00; color: white; font-weight: 600; text-decoration: none; transition: .2s; }
.btn-edit:hover { background: #d35400; }
.modal-overlay { position: fixed; z-index: 1000; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); }
.modal-box,.confirm-box { width: 450px; max-width: 100%; padding: 30px; border: 1px solid rgba(255,255,255,.8); border-radius: 20px; background: rgba(255,255,255,.95); box-shadow: 0 10px 40px rgba(0,0,0,.2); }
.modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.modal-header h2 { margin: 0; color: #2d3436; font-size: 1.2rem; }
.modal-close { border: 0; background: transparent; color: #666; cursor: pointer; font-size: 1.5rem; }
.modal-box label { display: block; margin: 8px 0; color: #666; font-size: .9rem; font-weight: 500; }
.form-control,.form-textarea { width: 100%; margin-bottom: 12px; padding: 12px; border: 1px solid #ddd; border-radius: 12px; background: rgba(255,255,255,.9); font: inherit; }
.form-textarea { resize: vertical; }
.refund-note { margin: 0 0 20px; padding: 10px; border-radius: 8px; background: #fff3cd; color: #856404; font-size: .8rem; }
.btn-submit-refund { width: 100%; padding: 12px 20px; border: 0; border-radius: 12px; background: #e74c3c; color: white; cursor: pointer; font-weight: 600; transition: .2s; }
.btn-submit-refund:hover:not(:disabled) { background: #c0392b; }
.btn-submit-refund:disabled { opacity: .6; cursor: wait; }
.confirm-box { width: 400px; text-align: center; }
.confirm-icon { display: grid; width: 60px; height: 60px; place-items: center; margin: 0 auto 15px; border-radius: 50%; background: #fff3e0; color: #ff9800; font-size: 30px; }
.confirm-box h2 { margin: 0 0 10px; color: #333; font-size: 1.2rem; }
.confirm-box p { margin: 0 0 25px; color: #666; line-height: 1.5; }
.confirm-actions { display: flex; justify-content: center; gap: 10px; }
.confirm-actions button { padding: 10px 20px; border: 0; border-radius: 50px; cursor: pointer; font-weight: 600; }
.confirm-cancel { background: #e0e0e0; color: #333; }
.confirm-submit:disabled,.confirm-cancel:disabled { opacity: .6; cursor: wait; }
@media (max-width: 900px) { .main-content { width: calc(100% - 210px); margin-left: 210px; padding: 28px; } .order-footer { align-items: stretch; flex-direction: column; gap: 18px; } .shipping-copy { max-width: none; } .order-total-actions { width: 100%; max-width: none; min-width: 0; } }
@media (max-width: 680px) { .orders-page { display: block; } .main-content { width: 100%; margin: 0; padding: 18px 14px 30px; } .header-section { margin-bottom: 18px; } .header-section h1 { padding: 8px 20px; font-size: 1.55rem; } .order-tab { padding: 8px 14px; font-size: .82rem; } .glass-panel { padding: 16px; } .order-header { flex-direction: column; gap: 10px; } .order-status-area { align-items: flex-start; text-align: left; } .order-item { gap: 10px; padding: 8px 4px; } .order-item img { width: 48px; height: 48px; flex-basis: 48px; } .item-copy strong { font-size: .88rem; } .item-subtotal { font-size: .85rem; } .tracking-input-group { flex-direction: column; } .btn-ship-action { min-height: 40px; } }
</style>
