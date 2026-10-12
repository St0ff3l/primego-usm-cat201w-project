<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase } from '../api'
import GlobalModal from '../components/GlobalModal.vue'
import MerchantBackground from '../components/MerchantBackground.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const currentUser = ref(null)
const order = ref(null)
const returnAddress = ref('')
const loading = ref(true)
const error = ref('')
const modal = ref(null)
const orderId = new URLSearchParams(window.location.search).get('orderId') || ''

function cancel() {
  window.location.href = '/merchant/order/order_management?filter=return'
}

async function loadOrder() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || user.role !== 'MERCHANT') {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const query = new URLSearchParams({ action: 'toReturnAddress', orderId })
    const response = await fetch(`${legacyBase}/merchant/order/order_management?${query}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Order not found or not accessible.')
    order.value = result.data.order
    returnAddress.value = order.value.returnAddress || ''
    document.title = 'Return Address - PrimeGo Seller'
  } catch (loadError) {
    error.value = loadError.message || 'Order not found or not accessible.'
  } finally {
    loading.value = false
  }
}

function confirmSave() {
  if (!returnAddress.value.trim()) {
    modal.value?.show('Validation Error', 'Return address is required.', 'error')
    return
  }
  modal.value?.confirm('Confirm Save', 'Are you sure you want to save this return address?', saveAddress)
}

async function saveAddress() {
  try {
    const response = await fetch(`${legacyBase}/merchant/order/order_management`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body: new URLSearchParams({ action: 'saveReturnAddress', orderId, returnAddress: returnAddress.value.trim() }),
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to save the return address.')
    if (result.data.redirect) window.location.assign(result.data.redirect)
    else modal.value?.show('Save Failed', result.data.message || 'Unable to save the return address.', 'error')
  } catch (saveError) {
    modal.value?.show('Save Failed', saveError.message || 'Unable to save the return address.', 'error')
  }
}

onMounted(loadOrder)
</script>

<template>
  <div class="merchant-return-address-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />
    <div class="container">
      <div class="panel">
        <div class="title">Provide Return Address</div>
        <div class="sub">Order #{{ order?.id }} — This address will be shown to the customer for returning the item.</div>

        <div v-if="loading" class="page-state" aria-live="polite">Loading order...</div>
        <div v-else-if="error" class="error-state" role="alert">{{ error }}</div>
        <form v-else-if="order" @submit.prevent="confirmSave">
          <label for="returnAddress">Return Address</label>
          <textarea id="returnAddress" v-model="returnAddress" placeholder="Receiver name, phone, full address" required></textarea>
          <div class="hint">Tip: include name + phone + full address to avoid delivery issues.</div>

          <div class="row">
            <button class="btn btn-cancel" type="button" @click="cancel">Cancel</button>
            <button class="btn btn-save" type="submit">Save & Continue</button>
          </div>
        </form>
      </div>
    </div>
    <GlobalModal ref="modal" />
  </div>
</template>

<style scoped>
.merchant-return-address-page { position: relative; min-height: 100vh; padding-top: 90px; background: #F3F6F9; color: #2d3436; }
.merchant-return-address-page,.merchant-return-address-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.container { position: relative; z-index: 1; max-width: 900px; margin: 30px auto; padding: 0 20px; }
.panel { padding: 24px; border-radius: 16px; background: white; box-shadow: 0 10px 30px rgba(0,0,0,.08); }
.title { margin-bottom: 6px; color: #2d3436; font-size: 1.4rem; font-weight: 700; }
.sub { margin-bottom: 18px; color: #636e72; }
label { display: block; margin: 14px 0 6px; font-weight: 600; }
textarea { width: 100%; min-height: 120px; padding: 12px; resize: vertical; border: 2px solid #eee; border-radius: 12px; }
.row { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.btn { padding: 10px 16px; border: 0; border-radius: 10px; cursor: pointer; font-weight: 600; }
.btn-cancel { background: #eee; color: #2d3436; }
.btn-save { background: #3498db; color: white; }
.hint { margin-top: 6px; color: #666; font-size: .85rem; }
.page-state { color: #636e72; }
.error-state { color: #c0392b; }
</style>
