<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, productImageUrl } from '../api'
import GlobalModal from '../components/GlobalModal.vue'
import MerchantBackground from '../components/MerchantBackground.vue'
import MerchantSidebar from '../components/MerchantSidebar.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const currentUser = ref(null)
const orders = ref([])
const loading = ref(true)
const error = ref('')
const activeFilter = ref(new URLSearchParams(window.location.search).get('filter')?.trim().toLowerCase() || 'all')
const shipOrderId = ref(null)
const trackingNumber = ref('')
const refundOrder = ref(null)
const merchantReason = ref('')
const modal = ref(null)

const counts = computed(() => {
  const result = { all: 0, to_ship: 0, shipped: 0, completed: 0, return: 0, cancelled: 0 }
  for (const order of orders.value) {
    const status = order.orderStatus || ''
    const normalized = status.toUpperCase()
    if (status && normalized !== 'PENDING') result.all++
    if (normalized === 'PAID') result.to_ship++
    else if (normalized === 'SHIPPED' && order.rejectionCount > 0) result.return++
    else if (normalized === 'SHIPPED') result.shipped++
    else if (normalized === 'COMPLETED') result.completed++
    else if (normalized === 'CANCELLED') result.cancelled++
    else if (normalized === 'RETURN_REQUESTED' || normalized === 'REFUNDED') result.return++
  }
  return result
})

function matchesFilter(order) {
  const status = (order.orderStatus || '').toUpperCase()
  const refundStatus = (order.refundStatus || '').toUpperCase()
  const rejectionCount = Number.parseInt(order.rejectionCount || '0', 10) || 0
  if (status === 'PENDING') return false
  if (activeFilter.value === 'all') return true
  if (activeFilter.value === 'to_ship') return status === 'PAID'
  if (activeFilter.value === 'shipped') return status === 'SHIPPED' && rejectionCount === 0
  if (activeFilter.value === 'completed') return status === 'COMPLETED'
  if (activeFilter.value === 'cancelled') return status === 'CANCELLED'
  if (activeFilter.value === 'return') {
    return status === 'RETURN_REQUESTED' || status === 'REFUNDED' || (status === 'SHIPPED' && rejectionCount > 0)
      || refundStatus === 'WAITING_RETURN' || refundStatus === 'RETURN_SHIPPED'
  }
  return true
}

function statusPresentation(order) {
  const status = order.orderStatus || ''
  const refundStatus = order.refundStatus || ''
  if (order.rejectionCount > 0 && refundStatus === 'REJECTED') return { label: 'Refund Rejected', className: 'status-rejected' }
  if (refundStatus === 'WAITING_RETURN') return { label: 'Waiting Customer Return', className: 'status-return' }
  if (refundStatus === 'RETURN_SHIPPED') return { label: 'Return Shipped', className: 'status-return' }
  if (refundStatus === 'APPROVED') return { label: 'Refund Approved', className: 'status-refunded' }
  if (status === 'SHIPPED' && order.rejectionCount > 0) return { label: 'Rejected (Action Required)', className: 'status-rejected' }
  const classes = {
    PAID: 'status-paid', SHIPPED: 'status-shipped', COMPLETED: 'status-completed',
    RETURN_REQUESTED: 'status-return', REFUNDED: 'status-refunded', CANCELLED: 'status-cancelled',
  }
  return { label: status.replace('_', ' '), className: classes[status] || 'status-pending' }
}

function openShipModal(orderId) {
  shipOrderId.value = orderId
  trackingNumber.value = ''
}

function openRefundModal(order) {
  const refundType = order.refundType || (['WAITING_RETURN', 'RETURN_SHIPPED'].includes(order.refundStatus) ? 'RETURN_AND_REFUND' : 'MONEY_ONLY')
  refundOrder.value = {
    ...order,
    displayReason: order.refundReason || 'No reason provided.',
    refundType,
    refundTypeLabel: refundType === 'RETURN_AND_REFUND' ? 'Return & Refund' : 'Refund only (no return)',
    approveLabel: refundType === 'MONEY_ONLY'
      ? 'Approve Refund'
      : order.refundStatus === 'RETURN_SHIPPED' ? 'Confirm Return Receipt (Refund)' : 'Agree to Return',
    approveDecision: refundType === 'MONEY_ONLY' || order.refundStatus === 'RETURN_SHIPPED'
      ? 'confirm_return_receipt' : 'agree_return',
  }
  merchantReason.value = ''
}

function changeFilter(filter) {
  activeFilter.value = filter
  const url = new URL(window.location)
  url.searchParams.set('filter', filter)
  window.history.pushState({}, '', url)
}

async function postOrderAction(fields) {
  const response = await fetch(`${legacyBase}/merchant/order/order_management`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    credentials: 'include',
    body: new URLSearchParams(fields),
  })
  const result = await response.json().catch(() => null)
  if (response.status === 401) {
    window.location.replace('/public/login.jsp')
    return
  }
  if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to update this order.')
  if (result.data.redirect) window.location.assign(result.data.redirect)
  else modal.value?.show('Action Required', result.data.message || 'Unable to update this order.', 'error')
}

function submitShip(event) {
  if (!event.currentTarget.reportValidity()) return
  postOrderAction({ action: 'shipOrder', orderId: shipOrderId.value, trackingNumber: trackingNumber.value })
    .catch((submitError) => modal.value?.show('Action Required', submitError.message, 'error'))
}

function submitRefund(decision) {
  if (decision === 'reject' && !merchantReason.value.trim()) {
    modal.value?.show('Action Required', 'Please provide a reason for rejection.', 'error')
    return
  }
  modal.value?.confirm('Confirm Action', `Proceed with ${decision}?`, () => {
    postOrderAction({
      action: 'processRefund',
      orderId: refundOrder.value.id,
      decision,
      refundType: refundOrder.value.refundType,
      merchantReason: merchantReason.value,
    }).catch((submitError) => modal.value?.show('Action Required', submitError.message, 'error'))
  })
}

async function loadOrders() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || user.role !== 'MERCHANT') {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const response = await fetch(`${legacyBase}/merchant/order/order_management`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load your orders.')
    orders.value = result.data.orders
    document.title = 'Order Management - PrimeGo Seller'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your orders.'
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="merchant-orders-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />

    <div class="layout-container">
      <MerchantSidebar active="order" />
      <main class="main-content">
        <div class="glass-panel">
          <div class="panel-heading">
            <h2>Order Management</h2>
          </div>

          <div class="metrics-bar" id="orderMetrics">
            <div v-for="filter in ['all', 'to_ship', 'shipped', 'completed', 'return', 'cancelled']" :key="filter"
              :class="['metric-card', { active: activeFilter === filter }]" role="button" tabindex="0"
              @click="changeFilter(filter)" @keydown.enter.prevent="changeFilter(filter)">
              <div :class="['metric-val', { 'metric-return': filter === 'return' && counts.return > 0 }]">{{ counts[filter] }}</div>
              <div class="metric-label">{{ ({ all: 'All Orders', to_ship: 'To Ship', shipped: 'Shipped', completed: 'Completed', return: 'Returns', cancelled: 'Cancelled' })[filter] }}</div>
            </div>
          </div>

          <div v-if="loading" class="empty-state" aria-live="polite">Loading orders...</div>
          <div v-else-if="error" class="empty-state" role="alert">{{ error }}</div>
          <div v-else-if="orders.length === 0" class="empty-state">
            <i class="ri-inbox-line empty-icon"></i>
            No orders found.
          </div>

          <div v-for="order in orders" v-show="!loading && !error && matchesFilter(order)" :key="order.id"
            class="order-card" :data-status="order.orderStatus" :data-rejection-count="order.rejectionCount"
            :data-refund-status="order.refundStatus">
            <div class="order-header">
              <div>
                <div class="order-id">Order #{{ order.id }}</div>
                <div class="order-date"><i class="ri-time-line"></i> {{ order.createdAt }}</div>
              </div>
              <span :class="['status-badge', statusPresentation(order).className]">{{ statusPresentation(order).label }}</span>
            </div>

            <div v-if="order.refundType === 'RETURN_AND_REFUND' && ['WAITING_RETURN', 'RETURN_SHIPPED'].includes(order.refundStatus)"
              class="return-progress">
              <div class="return-progress-title"><i class="ri-loop-left-line"></i> Return & Refund Progress</div>
              <div v-if="order.returnAddress" class="return-address-info">
                <strong>Return Address:</strong>
                <div class="preserve-lines">{{ order.returnAddress }}</div>
              </div>
              <div v-if="order.returnTrackingNumber" class="return-tracking-info">
                <strong>Customer Return Tracking No:</strong>
                <span>{{ order.returnTrackingNumber }}</span>
              </div>
            </div>

            <div class="order-items">
              <div v-for="(item, index) in order.orderItems" :key="`${order.id}-${index}`" class="item-row">
                <div class="item-info">
                  <div class="item-thumbnail">
                    <img :src="productImageUrl(item.productImageUrl)" alt="" @error="$event.currentTarget.src = '/assets/images/product-placeholder.svg'" />
                  </div>
                  <span>{{ item.productName }} <span class="item-quantity">x{{ item.quantity }}</span></span>
                </div>
                <span class="item-subtotal">${{ item.subtotal }}</span>
              </div>
            </div>

            <div class="address-box">
              <i class="ri-map-pin-user-fill address-icon"></i>
              <div>
                <div class="address-title">Shipping Address</div>
                <div class="address-text">{{ order.address }}</div>
              </div>
            </div>

            <div v-if="order.refundReason" class="reason-box"><strong>Customer's Reason:</strong> {{ order.refundReason }}</div>
            <div v-if="order.rejectionCount > 0" class="reason-box">
              <strong>Rejection Reason:</strong> {{ order.merchantRejectReason || '(No reason provided)' }}
            </div>

            <div class="order-footer">
              <div>
                <span class="total-label">Total:</span>
                <span class="total-value">${{ order.totalAmount }}</span>
              </div>
              <button v-if="order.orderStatus === 'PAID'" class="btn-ship" type="button" @click="openShipModal(order.id)">
                <i class="ri-truck-line"></i> Ship Order
              </button>
              <button v-if="order.orderStatus === 'RETURN_REQUESTED' || ['WAITING_RETURN', 'RETURN_SHIPPED'].includes(order.refundStatus)"
                class="btn-handle-refund" type="button" @click="openRefundModal(order)">
                <i class="ri-customer-service-2-line"></i> Handle Refund
              </button>
              <div v-if="order.orderStatus === 'SHIPPED'" class="shipped-info">
                <span><i class="ri-check-line"></i> Shipped</span>
                <div>Trk: {{ order.trackingNumber }}</div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <div v-if="shipOrderId !== null" class="modal-overlay" @click.self="shipOrderId = null">
      <div class="modal-content">
        <h3 class="modal-heading">Ship Order</h3>
        <p class="modal-description">Enter tracking number to confirm shipment.</p>
        <form @submit.prevent="submitShip">
          <div class="form-group">
            <label for="trackingNumber">Tracking Number</label>
            <input id="trackingNumber" v-model="trackingNumber" type="text" class="form-control" placeholder="e.g. JNT-12345678" required />
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" type="button" @click="shipOrderId = null">Cancel</button>
            <button class="btn-confirm" type="submit">Confirm Ship</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="refundOrder" class="modal-overlay" @click.self="refundOrder = null">
      <div class="modal-content refund-modal-content">
        <h3 class="refund-modal-heading">Handle Refund Request</h3>
        <div class="form-group">
          <label class="muted-label">Customer's Reason:</label>
          <div class="modal-info italic">{{ refundOrder.displayReason }}</div>
        </div>
        <div class="form-group">
          <label class="muted-label">Refund Type:</label>
          <div class="modal-info">{{ refundOrder.refundTypeLabel }}</div>
        </div>
        <div class="form-group">
          <label for="merchantReason">Your Response / Rejection Reason</label>
          <textarea id="merchantReason" v-model="merchantReason" class="form-textarea" placeholder="If rejecting, please explain why (e.g. item damaged by user)..."></textarea>
        </div>
        <div v-if="refundOrder.returnTrackingNumber" class="form-group">
          <label class="muted-label">Customer Return Tracking No:</label>
          <div class="modal-info">{{ refundOrder.returnTrackingNumber }}</div>
        </div>
        <div class="modal-actions modal-actions-split">
          <button class="btn-cancel" type="button" @click="refundOrder = null">Cancel</button>
          <div class="refund-actions">
            <button class="btn-reject" type="button" @click="submitRefund('reject')">Reject</button>
            <button class="btn-approve" :style="{ background: refundOrder.approveDecision === 'agree_return' ? '#3498db' : '#2ecc71' }" type="button" @click="submitRefund(refundOrder.approveDecision)">{{ refundOrder.approveLabel }}</button>
          </div>
        </div>
      </div>
    </div>
    <GlobalModal ref="modal" />
  </div>
</template>

<style scoped>
.merchant-orders-page { position: relative; min-height: 100vh; padding-top: 90px; background-color: #F3F6F9; color: #2d3436; --bg-color: #F3F6F9; --primary: #FF9500; --text-dark: #2d3436; --text-gray: #636e72; --card-radius: 16px; --danger: #e74c3c; --success: #2ecc71; --warning: #f1c40f; }
.merchant-orders-page,.merchant-orders-page * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.merchant-orders-page i[class^='ri-'],.merchant-orders-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.layout-container { position: relative; z-index: 1; display: grid; max-width: 1400px; grid-template-columns: 240px minmax(0,1fr); align-items: start; gap: 30px; margin: 30px auto; padding: 0 20px; }
.main-content { display: flex; min-width: 0; flex-direction: column; gap: 25px; }
.glass-panel { padding: 25px; border: 1px solid rgba(255,255,255,.9); border-radius: var(--card-radius); background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1); backdrop-filter: blur(25px); -webkit-backdrop-filter: blur(25px); }
.panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.panel-heading h2 { color: var(--text-dark); font-size: 1.5rem; }
.metrics-bar { display: grid; grid-template-columns: repeat(auto-fit,minmax(140px,1fr)); gap: 14px; margin: 0 0 20px; }
.metric-card { padding: 14px 16px; border: 1px solid #f1f2f6; border-radius: 14px; background: white; box-shadow: 0 4px 10px rgba(0,0,0,.03); cursor: pointer; transition: all .2s ease; user-select: none; }
.metric-card:hover { transform: translateY(-2px); border-color: rgba(255,149,0,.15); }
.metric-card.active { border-color: rgba(255,149,0,.8); box-shadow: 0 10px 24px rgba(255,149,0,.18); }
.metric-val { color: var(--text-dark); font-size: 1.6rem; font-weight: 800; line-height: 1.1; }
.metric-val.metric-return { color: #e74c3c; }
.metric-label { margin-top: 4px; color: var(--text-gray); font-size: .9rem; }
.order-card { margin-bottom: 20px; padding: 20px; border-left: 5px solid var(--primary); border-radius: 12px; background: white; box-shadow: 0 4px 10px rgba(0,0,0,.03); transition: transform .2s; }
.order-card[data-status='RETURN_REQUESTED'] { border-left-color: var(--danger); }
.order-card[data-status='REFUNDED'] { border-left-color: var(--text-gray); }
.order-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,.08); }
.order-header { display: flex; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee; }
.order-id { color: var(--text-dark); font-size: 1.1rem; font-weight: 700; }
.order-date { color: var(--text-gray); font-size: .85rem; }
.status-badge { padding: 5px 12px; border-radius: 20px; font-size: .8rem; font-weight: 600; }
.status-paid { background: #fff3cd; color: #856404; }
.status-shipped { background: #d1e7dd; color: #0f5132; }
.status-completed { background: #d4edda; color: #155724; }
.status-pending { background: #f8d7da; color: #721c24; }
.status-cancelled { background: #e2e3e5; color: #41464b; }
.status-return { background: #ffeaa7; color: #d35400; }
.status-refunded { background: #fab1a0; color: #c0392b; }
.status-rejected { border: 1px solid #d35400; background: #ffeaa7; color: #d35400; }
.return-progress { margin: 0 0 15px; padding: 12px; border-radius: 10px; background: #f8f9fa; }
.return-progress-title { margin-bottom: 6px; color: #2d3436; font-weight: 700; }
.return-address-info,.return-tracking-info { color: #444; font-size: .9rem; }
.return-address-info { margin-bottom: 8px; }
.preserve-lines { margin-top: 4px; white-space: pre-wrap; }
.return-tracking-info span { font-weight: 700; }
.order-items { margin-bottom: 15px; }
.item-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #eee; font-size: .95rem; }
.item-row:last-child { border-bottom: none; }
.item-info { display: flex; align-items: center; gap: 10px; }
.item-thumbnail { width: 30px; height: 30px; overflow: hidden; border-radius: 5px; background: #eee; }
.item-thumbnail img { width: 100%; height: 100%; object-fit: cover; }
.item-quantity { color: #999; }
.item-subtotal { font-weight: 600; }
.address-box { display: flex; align-items: flex-start; gap: 10px; margin: 15px 0; padding: 15px; border-radius: 8px; background: #f8f9fa; color: #444; font-size: .9rem; }
.address-icon { margin-top: 2px; color: var(--primary); font-size: 1.2rem; }
.address-title { margin-bottom: 3px; font-weight: 600; }
.address-text { line-height: 1.4; }
.reason-box { margin-bottom: 15px; padding: 10px; border-radius: 8px; background: #fff3cd; color: #856404; font-size: .9rem; }
.order-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 15px; padding-top: 15px; border-top: 1px solid #f0f0f0; }
.total-label { color: #666; font-size: .9rem; }
.total-value { margin-left: 4px; color: var(--primary); font-size: 1.2rem; font-weight: 700; }
.btn-ship,.btn-handle-refund { display: flex; align-items: center; gap: 5px; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: .3s; }
.btn-ship { border: 0; background: var(--text-dark); color: white; }
.btn-ship:hover { transform: translateY(-2px); background: var(--primary); }
.btn-handle-refund { border: 1px solid var(--danger); background: white; color: var(--danger); }
.btn-handle-refund:hover { background: var(--danger); color: white; }
.shipped-info { text-align: right; }
.shipped-info span { color: #0f5132; font-size: .9rem; font-weight: 500; }
.shipped-info div { color: #666; font-size: .8rem; }
.empty-state { padding: 50px; color: #999; text-align: center; }
.empty-icon { display: block; margin-bottom: 10px; font-size: 3rem; }
.modal-overlay { position: fixed; z-index: 1000; display: flex; inset: 0; align-items: center; justify-content: center; background: rgba(0,0,0,.5); }
.modal-content { width: 450px; padding: 30px; border-radius: 16px; background: white; box-shadow: 0 10px 30px rgba(0,0,0,.2); animation: popIn .3s ease; }
.refund-modal-content { width: 500px; }
@keyframes popIn { from { transform: scale(.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-heading,.refund-modal-heading { margin-bottom: 15px; }
.refund-modal-heading { margin-bottom: 10px; color: #d35400; }
.modal-description { margin-bottom: 20px; color: #666; }
.form-group { margin-bottom: 15px; }
.form-group label { display: block; margin-bottom: 5px; color: #333; font-weight: 600; }
.form-group .muted-label { color: #666; }
.form-control,.form-textarea { width: 100%; padding: 10px; border: 2px solid #eee; border-radius: 8px; font-size: 1rem; }
.form-textarea { min-height: 80px; resize: vertical; font-size: .95rem; }
.modal-info { padding: 10px; border-radius: 8px; background: #f8f9fa; color: #555; }
.modal-info.italic { font-style: italic; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.modal-actions-split { justify-content: space-between; }
.btn-cancel,.btn-confirm,.btn-approve,.btn-reject { padding: 10px 20px; border: 0; border-radius: 8px; cursor: pointer; }
.btn-cancel { background: #eee; }
.btn-confirm { background: var(--primary); color: white; font-weight: 600; }
.btn-approve { background: var(--success); color: white; font-weight: 600; }
.btn-reject { background: var(--danger); color: white; font-weight: 600; }
.refund-actions { display: flex; gap: 10px; }
@media(max-width:1024px) { .layout-container { grid-template-columns: 80px minmax(0,1fr); } }
</style>
