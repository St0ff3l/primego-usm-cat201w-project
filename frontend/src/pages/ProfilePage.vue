<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import AccountSidebar from '../components/AccountSidebar.vue'

const activeTab = ref(new URLSearchParams(window.location.search).get('tab') || 'profile')
const username = ref('')
const profile = ref(null)
const addresses = ref([])
const loading = ref(true)
const busy = ref(false)
const message = ref('')
const messageType = ref('success')
const settingsMessage = ref('')
const settingsMessageType = ref('success')
const settingsMessageTarget = ref('')
const editProfileOpen = ref(false)
const addressModalOpen = ref(false)
const addressMode = ref('add')
const addressForm = ref(null)
const pinRefs = ref({ old: [], new: [], confirm: [] })
const pinDigits = reactive({ old: ['', '', '', '', '', ''], new: ['', '', '', '', '', ''], confirm: ['', '', '', '', '', ''] })
const confirmation = ref(null)
const confirmedAction = ref(null)
const addressFormData = reactive({
  id: '', recipientName: '', phone: '', province: '', city: '', district: '', detail: '', defaultAddress: false,
})
const hasPin = computed(() => Boolean(profile.value?.hasPaymentPin))

function setMessage(text, type = 'error') {
  message.value = text
  messageType.value = type
}

function setSettingsMessage(text, type = 'error') {
  settingsMessage.value = text
  settingsMessageType.value = type
}

function updateProfileData(data) {
  profile.value = data.profile
  addresses.value = data.addresses || []
  username.value = data.username || username.value
  if (data.message) setMessage(data.message, data.messageType || 'success')
  if (data.settingsMessage) setSettingsMessage(data.settingsMessage, data.settingsMessageType || 'success')
}

async function loadProfile() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (user.role !== 'CUSTOMER') {
      window.location.replace(user.role === 'MERCHANT' ? '/merchant/user/merchant_profile.jsp' : '/admin/user/admin_profile.jsp')
      return
    }
    username.value = user.username || ''

    const response = await fetch(legacyBase + '/profile', {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!result || !response.ok || result.code !== 200) throw new Error(result?.msg || 'Unable to load your profile.')
    updateProfileData(result.data)
    document.title = 'Customer Profile - PrimeGo'
    if (new URLSearchParams(window.location.search).get('pinAction') === 'true') {
      await nextTick()
      document.getElementById('pin-settings')?.scrollIntoView({ block: 'center', behavior: 'smooth' })
    }
  } catch (error) {
    setMessage(error.message || 'Unable to load your profile.')
  } finally {
    loading.value = false
  }
}

async function postForm(path, form) {
  const response = await fetch(legacyBase + path, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    credentials: 'include',
    body: new URLSearchParams(new FormData(form)),
  })
  const result = await response.json().catch(() => null)
  if (response.status === 401) {
    window.location.replace('/public/login.jsp')
    throw new Error('Please log in again.')
  }
  if (!result || !response.ok || result.code !== 200) throw new Error(result?.msg || 'Unable to save your changes.')
  return result.data
}

async function saveProfile(event) {
  busy.value = true
  try {
    updateProfileData(await postForm('/profile', event.currentTarget))
    editProfileOpen.value = false
  } catch (error) {
    setMessage(error.message || 'Unable to update your profile.')
  } finally {
    busy.value = false
  }
}

async function changePassword(event) {
  busy.value = true
  try {
    const data = await postForm('/profile', event.currentTarget)
    updateProfileData(data)
    settingsMessageTarget.value = 'password'
    setSettingsMessage(data.settingsMessage || 'Password updated.', data.settingsMessageType || 'success')
    if (data.settingsMessageType === 'success') event.currentTarget.reset()
  } catch (error) {
    settingsMessageTarget.value = 'password'
    setSettingsMessage(error.message || 'Unable to update your password.')
  } finally {
    busy.value = false
  }
}

function clearPinFields() {
  for (const group of Object.keys(pinDigits)) pinDigits[group] = ['', '', '', '', '', '']
}

async function updatePin(event) {
  busy.value = true
  try {
    const data = await postForm('/profile', event.currentTarget)
    updateProfileData(data)
    settingsMessageTarget.value = 'pin'
    setSettingsMessage(data.settingsMessage || 'Payment PIN updated.', data.settingsMessageType || 'success')
    if (data.settingsMessageType === 'success') clearPinFields()
  } catch (error) {
    settingsMessageTarget.value = 'pin'
    setSettingsMessage(error.message || 'Unable to update your payment PIN.')
  } finally {
    busy.value = false
  }
}

function openAddressModal(mode, address = null) {
  addressMode.value = mode
  Object.assign(addressFormData, address ? {
    id: address.id,
    recipientName: address.recipientName || '',
    phone: address.phone || '',
    province: address.province || '',
    city: address.city || '',
    district: address.district || '',
    detail: address.detail || '',
    defaultAddress: address.defaultAddress,
  } : {
    id: '', recipientName: '', phone: '', province: '', city: '', district: '', detail: '', defaultAddress: false,
  })
  addressModalOpen.value = true
}

async function saveAddress(event) {
  busy.value = true
  try {
    const data = await postForm('/user/address', event.currentTarget)
    addresses.value = data.addresses || []
    setMessage(data.message, data.messageType || 'success')
    addressModalOpen.value = false
  } catch (error) {
    setMessage(error.message || 'Unable to save this address.')
  } finally {
    busy.value = false
  }
}

async function sendAddressAction(action, addressId) {
  busy.value = true
  const body = new URLSearchParams({ action, addressId: String(addressId) })
  try {
    const response = await fetch(legacyBase + '/user/address', {
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
    if (!result || !response.ok || result.code !== 200) throw new Error(result?.msg || 'Unable to update this address.')
    addresses.value = result.data.addresses || []
    setMessage(result.data.message, result.data.messageType || 'success')
  } catch (error) {
    setMessage(error.message || 'Unable to update this address.')
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

function setPinRef(group, index, element) {
  pinRefs.value[group][index] = element
}

function updatePinDigit(group, index, event) {
  const digit = event.target.value.replace(/\D/g, '').slice(-1)
  pinDigits[group][index] = digit
  event.target.value = digit
  if (digit && index < 5) pinRefs.value[group][index + 1]?.focus()
}

function handlePinKeydown(group, index, event) {
  if (event.key === 'Backspace' && !pinDigits[group][index] && index > 0) pinRefs.value[group][index - 1]?.focus()
}

function addressLines(address) {
  return [address.detail, [address.district, address.city].filter(Boolean).join(', '), address.province].filter(Boolean)
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <AccountSidebar :active="activeTab === 'profile' ? 'profile' : activeTab" />

    <main class="profile-main">
      <div v-if="loading" class="profile-state" aria-live="polite">Loading your profile...</div>
      <div v-else-if="message && !profile" class="profile-state error-state" role="alert">{{ message }}</div>

      <template v-else-if="profile">
        <div v-if="message" :class="['profile-notice', 'notice-' + messageType]" role="status">{{ message }}</div>

        <template v-if="activeTab === 'addresses'">
          <header class="header-section">
            <h1>My Addresses</h1>
            <button class="btn-edit add-address-button" type="button" @click="openAddressModal('add')">
              <i class="ri-add-line"></i> Add New Address
            </button>
          </header>
          <div v-if="!addresses.length" class="glass-panel no-addresses">No addresses saved yet.</div>
          <div v-else class="address-grid">
            <article v-for="address in addresses" :key="address.id" :class="['glass-panel', 'saved-address', { 'default-address': address.defaultAddress }]">
              <span v-if="address.defaultAddress" class="default-badge">Default</span>
              <h2>{{ address.recipientName }}</h2>
              <p class="address-phone">{{ address.phone }}</p>
              <p class="saved-address-lines">{{ addressLines(address).join('\n') }}</p>
              <div class="address-actions">
                <button class="btn-outline" type="button" :disabled="busy" @click="openAddressModal('update', address)">Edit</button>
                <button class="btn-outline delete-button" type="button" :disabled="busy" @click="askConfirmation('Delete Address', 'Are you sure you want to delete this address?', () => sendAddressAction('delete', address.id))">Delete</button>
                <button v-if="!address.defaultAddress" class="btn-outline default-button" type="button" :disabled="busy" @click="sendAddressAction('setDefault', address.id)">Set Default</button>
              </div>
            </article>
          </div>
        </template>

        <template v-else-if="activeTab === 'settings'">
          <header class="header-section"><h1>Account Settings</h1></header>
          <div class="settings-grid">
            <section class="glass-panel settings-card">
              <h2>Change Password</h2>
              <div v-if="settingsMessage && settingsMessageTarget === 'password'" :class="['settings-notice', 'notice-' + settingsMessageType]" role="status">{{ settingsMessage }}</div>
              <form @submit.prevent="changePassword">
                <input type="hidden" name="action" value="changePassword" />
                <label>Current Password<input class="form-input" type="password" name="oldPassword" autocomplete="current-password" required /></label>
                <label>New Password<input class="form-input" type="password" name="newPassword" autocomplete="new-password" required /></label>
                <label>Confirm New Password<input class="form-input" type="password" name="confirmPassword" autocomplete="new-password" required /></label>
                <button class="btn-edit full-button" type="submit" :disabled="busy">{{ busy ? 'Updating...' : 'Update Password' }}</button>
              </form>
            </section>

            <section id="pin-settings" class="glass-panel settings-card">
              <h2>{{ hasPin ? 'Change Payment PIN' : 'Set Payment PIN' }}</h2>
              <div v-if="settingsMessage && settingsMessageTarget === 'pin'" :class="['settings-notice', 'notice-' + settingsMessageType]" role="status">{{ settingsMessage }}</div>
              <form @submit.prevent="updatePin">
                <input type="hidden" name="action" value="updatePin" />
                <div v-if="hasPin" class="pin-group">
                  <label>Current PIN</label>
                  <div class="pin-container">
                    <input
                      v-for="(_, index) in pinDigits.old"
                      :key="'old-' + index"
                      :ref="(element) => setPinRef('old', index, element)"
                      :value="pinDigits.old[index]"
                      class="pin-digit"
                      type="password"
                      inputmode="numeric"
                      maxlength="1"
                      :name="'oldPin' + (index + 1)"
                      :aria-label="'Current PIN digit ' + (index + 1)"
                      required
                      @input="updatePinDigit('old', index, $event)"
                      @keydown="handlePinKeydown('old', index, $event)"
                    />
                  </div>
                </div>
                <div class="pin-group">
                  <label>New PIN (6 digits)</label>
                  <div class="pin-container">
                    <input
                      v-for="(_, index) in pinDigits.new"
                      :key="'new-' + index"
                      :ref="(element) => setPinRef('new', index, element)"
                      :value="pinDigits.new[index]"
                      class="pin-digit"
                      type="password"
                      inputmode="numeric"
                      maxlength="1"
                      :name="'pin' + (index + 1)"
                      :aria-label="'New PIN digit ' + (index + 1)"
                      required
                      @input="updatePinDigit('new', index, $event)"
                      @keydown="handlePinKeydown('new', index, $event)"
                    />
                  </div>
                </div>
                <div class="pin-group">
                  <label>Confirm New PIN</label>
                  <div class="pin-container">
                    <input
                      v-for="(_, index) in pinDigits.confirm"
                      :key="'confirm-' + index"
                      :ref="(element) => setPinRef('confirm', index, element)"
                      :value="pinDigits.confirm[index]"
                      class="pin-digit"
                      type="password"
                      inputmode="numeric"
                      maxlength="1"
                      :name="'confirmPin' + (index + 1)"
                      :aria-label="'Confirm PIN digit ' + (index + 1)"
                      required
                      @input="updatePinDigit('confirm', index, $event)"
                      @keydown="handlePinKeydown('confirm', index, $event)"
                    />
                  </div>
                </div>
                <button class="btn-edit full-button" type="submit" :disabled="busy">{{ busy ? 'Saving...' : (hasPin ? 'Update PIN' : 'Set PIN') }}</button>
              </form>
            </section>
          </div>
        </template>

        <template v-else-if="activeTab === 'orders'">
          <header class="header-section"><h1>Order History</h1></header>
          <nav class="order-tabs" aria-label="Order status">
            <a v-for="[value, label] in [['ALL','All'],['PAID','To Ship'],['SHIPPED','To Receive'],['COMPLETED','Completed'],['CANCELLED','Cancelled']]" :key="value" :href="'/customer/orders?status=' + value" class="order-tab">{{ label }}</a>
          </nav>
          <section class="glass-panel orders-link-panel">
            <p>For the full order list, open the standalone My Orders page.</p>
            <a class="btn-edit" href="/customer/orders?status=ALL">Go to My Orders</a>
          </section>
        </template>

        <template v-else>
          <header class="header-section"><h1>Profile Information</h1></header>
          <section class="glass-panel avatar-card">
            <div class="avatar-circle">{{ username.charAt(0).toUpperCase() || '?' }}</div>
            <div class="user-details">
              <h2>{{ username }}</h2>
              <p>{{ profile.email }}</p>
              <span class="role-badge">Customer Account</span>
              <button class="btn-edit" type="button" @click="editProfileOpen = true">Edit Profile</button>
            </div>
          </section>

          <section class="glass-panel">
            <h2 class="panel-title">My Orders</h2>
            <div class="order-status-grid">
              <a class="status-item" href="/customer/orders?status=PAID"><i class="ri-box-3-line status-icon"></i><span class="status-label">To Ship</span></a>
              <a class="status-item" href="/customer/orders?status=SHIPPED"><i class="ri-truck-line status-icon"></i><span class="status-label">To Receive</span></a>
            </div>
          </section>

          <section class="glass-panel contact-panel">
            <h2 class="panel-title">Contact Details</h2>
            <template v-if="profile">
              <p><strong>Full Name:</strong> {{ profile.fullName }}</p>
              <p><strong>Phone:</strong> {{ profile.phone }}</p>
              <p><strong>Address:</strong> {{ profile.formattedAddress }}</p>
              <p>Manage your addresses in the <a href="/profile?tab=addresses">Addresses</a> tab.</p>
            </template>
            <p v-else>No additional contact details found.</p>
          </section>
        </template>
      </template>
    </main>

    <div v-if="editProfileOpen" class="modal-overlay" @click.self="editProfileOpen = false" @keydown.esc="editProfileOpen = false">
      <section class="modal-dialog" role="dialog" aria-modal="true" aria-labelledby="edit-profile-title">
        <header class="modal-header"><h2 id="edit-profile-title">Edit Profile</h2><button class="btn-close" type="button" aria-label="Close" @click="editProfileOpen = false">&times;</button></header>
        <form @submit.prevent="saveProfile">
          <input type="hidden" name="action" value="updateCustomerProfile" />
          <div class="form-stack">
            <div class="form-grid">
              <label>First Name *<input class="form-input" type="text" name="firstName" :value="profile.firstName" required /></label>
              <label>Last Name *<input class="form-input" type="text" name="lastName" :value="profile.lastName" required /></label>
            </div>
            <div class="form-grid phone-grid">
              <label>Area Code
                <select class="form-input" name="phoneAreaCode" :value="profile.phoneAreaCode">
                  <option value="+1">+1 (US)</option><option value="+60">+60 (MY)</option><option value="+86">+86 (CN)</option><option value="+44">+44 (UK)</option>
                </select>
              </label>
              <label>Phone Number *<input class="form-input" type="text" name="phoneNumber" :value="profile.phoneNumberOnly" required /></label>
            </div>
          </div>
          <div class="modal-actions"><button class="btn-edit" type="submit" :disabled="busy">{{ busy ? 'Saving...' : 'Save information' }}</button><button class="btn-edit cancel-button" type="button" @click="editProfileOpen = false">Cancel</button></div>
        </form>
      </section>
    </div>

    <div v-if="addressModalOpen" class="modal-overlay" @click.self="addressModalOpen = false" @keydown.esc="addressModalOpen = false">
      <section class="modal-dialog address-dialog" role="dialog" aria-modal="true" aria-labelledby="address-modal-title">
        <header class="modal-header"><h2 id="address-modal-title">{{ addressMode === 'update' ? 'Update Address' : 'Add Address' }}</h2><button class="btn-close" type="button" aria-label="Close" @click="addressModalOpen = false">&times;</button></header>
        <form ref="addressForm" @submit.prevent="saveAddress">
          <input type="hidden" name="action" :value="addressMode" />
          <input type="hidden" name="addressId" :value="addressFormData.id" />
          <div class="form-stack">
            <div class="form-grid">
              <label>Receiver Name<input v-model="addressFormData.recipientName" class="form-input" name="recipientName" type="text" required /></label>
              <label>Phone<input v-model="addressFormData.phone" class="form-input" name="phone" type="text" required /></label>
            </div>
            <label>Province/State<input v-model="addressFormData.province" class="form-input" name="province" type="text" /></label>
            <div class="form-grid">
              <label>City<input v-model="addressFormData.city" class="form-input" name="city" type="text" /></label>
              <label>District (Optional)<input v-model="addressFormData.district" class="form-input" name="district" type="text" /></label>
            </div>
            <label>Detailed Address<textarea v-model="addressFormData.detail" class="form-input detail-input" name="detail" rows="3" required></textarea></label>
            <label class="checkbox-label"><input v-model="addressFormData.defaultAddress" type="checkbox" name="isDefault" value="true" /> Set as Default Address</label>
          </div>
          <div class="modal-actions"><button class="btn-edit" type="submit" :disabled="busy">{{ busy ? 'Saving...' : 'Save Address' }}</button><button class="btn-edit cancel-button" type="button" @click="addressModalOpen = false">Cancel</button></div>
        </form>
      </section>
    </div>

    <div v-if="confirmation" class="modal-overlay" @click.self="confirmation = null" @keydown.esc="confirmation = null">
      <section class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div class="confirm-icon"><i class="ri-question-line"></i></div>
        <h2 id="confirm-title">{{ confirmation.title }}</h2>
        <p>{{ confirmation.text }}</p>
        <div class="modal-actions"><button class="btn-edit cancel-button" type="button" @click="confirmation = null">Cancel</button><button class="btn-edit" type="button" :disabled="busy" @click="runConfirmedAction">{{ busy ? 'Processing...' : 'Yes, Proceed' }}</button></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page { display: flex; min-height: 100vh; position: relative; overflow-x: hidden; color: #333; }
.background-blob { position: fixed; z-index: -1; border-radius: 50%; pointer-events: none; }
.blob-red,.blob-yellow { background: white; }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.profile-main { width: calc(100% - 250px); min-width: 0; margin-left: 250px; flex: 1; padding: 40px; }
.header-section { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-bottom: 30px; }
.header-section h1 { display: inline-block; margin: 0; padding: 10px 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 50px; background: rgba(255,255,255,.6); color: #e68a00; font-size: 2.2rem; box-shadow: 0 4px 15px rgba(0,0,0,.05); backdrop-filter: blur(10px); }
.glass-panel { margin: 0 0 30px; padding: 30px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); backdrop-filter: blur(20px); box-shadow: 0 8px 32px rgba(0,0,0,.1); }
.profile-notice,.settings-notice { margin-bottom: 20px; padding: 15px; border-radius: 10px; }
.notice-success { background: #d4edda; color: #155724; }
.notice-error { background: #f8d7da; color: #721c24; }
.avatar-card { display: flex; align-items: center; gap: 30px; }
.avatar-circle { display: flex; width: 100px; height: 100px; flex: 0 0 100px; align-items: center; justify-content: center; border-radius: 50%; background: linear-gradient(135deg,#ff9500,#ffcc00); color: white; font-size: 3rem; font-weight: 700; box-shadow: 0 10px 20px rgba(255,149,0,.3); }
.user-details h2 { margin: 0 0 5px; color: #2d3436; }
.user-details p { margin: 0 0 5px; color: #636e72; }
.role-badge { display: inline-block; margin-top: 5px; padding: 4px 12px; border-radius: 20px; background: #fff3e0; color: #e68a00; font-size: .85rem; font-weight: 600; }
.btn-edit { margin-top: 15px; padding: 10px 25px; border: 0; border-radius: 20px; background: #e68a00; color: white; cursor: pointer; font-weight: 600; text-decoration: none; transition: .2s; }
.btn-edit:hover:not(:disabled) { background: #d35400; }
.btn-edit:disabled { opacity: .6; cursor: wait; }
.panel-title { margin: 0 0 20px; color: #e68a00; font-size: 1.2rem; }
.order-status-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 20px; text-align: center; }
.status-item { display: block; padding: 20px; border-radius: 15px; color: inherit; text-decoration: none; transition: .2s; }
.status-item:hover { background: rgba(255,255,255,.5); transform: translateY(-4px); }
.status-icon { display: block; margin-bottom: 10px; color: #e68a00; font-size: 2rem; }
.status-label { color: #555; font-weight: 600; }
.contact-panel p { margin: 0 0 10px; overflow-wrap: anywhere; }
.contact-panel a { color: #d35400; font-weight: 600; }
.profile-state { padding: 50px; color: #666; text-align: center; }
.error-state { color: #721c24; }
.address-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(300px,1fr)); gap: 20px; }
.saved-address { position: relative; margin: 0; }
.default-address { border: 2px solid #e68a00; }
.saved-address h2 { margin: 0 0 5px; padding-right: 75px; color: #2d3436; font-size: 1.15rem; overflow-wrap: anywhere; }
.default-badge { position: absolute; top: 15px; right: 15px; padding: 4px 10px; border-radius: 15px; background: #e68a00; color: white; font-size: .8rem; font-weight: 700; }
.address-phone { margin: 0 0 10px; color: #666; font-size: .9rem; }
.saved-address-lines { min-height: 60px; margin: 0 0 15px; white-space: pre-line; overflow-wrap: anywhere; }
.address-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.btn-outline { padding: 5px 15px; border: 1px solid #ccc; border-radius: 15px; background: transparent; color: #444; cursor: pointer; font: inherit; font-size: .9rem; }
.btn-outline:hover:not(:disabled) { background: #f5f5f5; }
.btn-outline:disabled { opacity: .6; cursor: wait; }
.delete-button { border-color: #ff6b6b; color: #ff6b6b; }
.default-button { border-color: #e68a00; color: #e68a00; }
.no-addresses,.orders-link-panel { color: #666; text-align: center; }
.orders-link-panel p { margin: 0 0 15px; }
.order-tabs { display: flex; gap: 10px; margin-bottom: 20px; padding-bottom: 5px; overflow-x: auto; }
.order-tab { flex: 0 0 auto; padding: 8px 20px; border-radius: 20px; background: rgba(255,255,255,.5); color: #555; font-size: .9rem; font-weight: 600; text-decoration: none; }
.order-tab:hover { background: #e68a00; color: white; }
.settings-grid { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 20px; }
.settings-card { width: min(100%,500px); flex: 1 1 300px; margin: 0; }
.settings-card h2 { margin: 0 0 25px; color: #e68a00; font-size: 1.2rem; }
.settings-card label,.modal-dialog label { display: block; margin-bottom: 20px; color: #555; font-size: .85rem; font-weight: 500; }
.form-input { display: block; width: 100%; margin-top: 8px; padding: 12px 20px; border: 1px solid rgba(255,255,255,.5); border-radius: 25px; background: rgba(255,255,255,.75); color: #333; font: inherit; font-size: .9rem; outline: none; box-shadow: inset 2px 2px 5px rgba(0,0,0,.05); }
.form-input:focus { border-color: #e68a00; background: rgba(255,255,255,.95); box-shadow: 0 0 0 3px rgba(230,138,0,.2); }
select.form-input { appearance: none; background-image: url("data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%23e68a00%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E"); background-repeat: no-repeat; background-position: right 15px center; background-size: 12px auto; padding-right: 40px; }
.full-button { width: 100%; margin-top: 5px; padding: 12px; }
.pin-group { margin-bottom: 20px; }
.pin-group > label { margin: 0 0 8px; }
.pin-container { display: flex; justify-content: center; gap: 10px; margin: 10px 0; }
.pin-digit { width: 45px; height: 50px; border: 1px solid rgba(255,255,255,.5); border-radius: 12px; background: rgba(255,255,255,.8); color: #333; font-size: 1.2rem; font-weight: 700; text-align: center; box-shadow: 0 4px 10px rgba(0,0,0,.1); }
.pin-digit:focus { border-color: #e68a00; outline: none; box-shadow: 0 0 0 3px rgba(230,138,0,.2),0 8px 20px rgba(230,138,0,.15); }
.add-address-button { margin: 0; padding: 10px 20px; white-space: nowrap; }
.modal-overlay { position: fixed; z-index: 1000; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); }
.modal-dialog,.confirm-dialog { width: 500px; max-width: 100%; max-height: calc(100vh - 40px); overflow-y: auto; padding: 30px; border: 1px solid rgba(255,255,255,.8); border-radius: 20px; background: rgba(255,255,255,.93); box-shadow: 0 10px 40px rgba(0,0,0,.2); }
.address-dialog { width: 600px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 25px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,.05); }
.modal-header h2 { margin: 0; color: #2d3436; font-size: 1.2rem; }
.btn-close { border: 0; background: transparent; color: #666; cursor: pointer; font-size: 1.5rem; }
.form-stack { display: flex; flex-direction: column; gap: 4px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.phone-grid { grid-template-columns: .5fr 1fr; }
.detail-input { border-radius: 15px; resize: vertical; }
.checkbox-label { display: flex !important; align-items: center; gap: 10px; margin-bottom: 0 !important; font-size: .9rem !important; }
.checkbox-label input { accent-color: #e68a00; }
.modal-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; margin-top: 25px; }
.modal-actions .btn-edit { margin-top: 0; padding: 10px 25px; }
.cancel-button { background: #999; }
.confirm-dialog { width: 400px; text-align: center; }
.confirm-icon { display: grid; width: 60px; height: 60px; place-items: center; margin: 0 auto 15px; border-radius: 50%; background: #fff3e0; color: #ff9800; font-size: 30px; }
.confirm-dialog h2 { margin: 0 0 10px; color: #333; font-size: 1.2rem; }
.confirm-dialog p { margin: 0; color: #666; line-height: 1.5; }
.profile-notice:empty { display: none; }
@media (max-width: 900px) { .profile-main { width: calc(100% - 210px); margin-left: 210px; padding: 28px; } }
@media (max-width: 680px) { .profile-page { display: block; } .profile-main { width: 100%; margin: 0; padding: 18px 14px 30px; } .header-section { align-items: flex-start; flex-direction: column; margin-bottom: 20px; } .header-section h1 { padding: 8px 20px; font-size: 1.55rem; } .glass-panel { padding: 20px; } .avatar-card { align-items: flex-start; gap: 18px; } .avatar-circle { width: 72px; height: 72px; flex-basis: 72px; font-size: 2rem; } .user-details h2 { overflow-wrap: anywhere; } .address-grid { grid-template-columns: minmax(0,1fr); } .form-grid { grid-template-columns: minmax(0,1fr); gap: 0; } .phone-grid { grid-template-columns: minmax(0,1fr); } .pin-container { gap: 6px; } .pin-digit { width: min(45px,calc((100vw - 100px) / 6)); height: 48px; } .modal-dialog,.confirm-dialog { padding: 22px; } .add-address-button { align-self: flex-start; } }
</style>
