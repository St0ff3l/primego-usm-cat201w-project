<script setup>
import { ref } from 'vue'

const open = ref(false)
const title = ref('Title')
const message = ref('Message goes here.')
const type = ref('error')
const confirmMode = ref(false)
let onConfirm = null
let onCancel = null

function show(nextTitle, nextMessage, nextType = 'error', callback = null) {
  title.value = nextTitle
  message.value = nextMessage
  type.value = nextType
  confirmMode.value = false
  onConfirm = callback
  onCancel = null
  open.value = true
}

function confirm(nextTitle, nextMessage, callback, cancelCallback = null) {
  title.value = nextTitle
  message.value = nextMessage
  type.value = 'warning'
  confirmMode.value = true
  onConfirm = callback
  onCancel = cancelCallback
  open.value = true
}

function close(primary = false) {
  open.value = false
  const callback = confirmMode.value ? (primary ? onConfirm : onCancel) : onConfirm
  onConfirm = null
  onCancel = null
  callback?.()
}

defineExpose({ show, confirm })
</script>

<template>
  <div :class="['custom-modal-overlay', { active: open }]" @click.self="close(false)">
    <div class="custom-modal-box">
      <div :class="['modal-icon-wrap', `icon-${type}`]">
        <i :class="type === 'warning' ? 'ri-question-line' : type === 'success' ? 'ri-checkbox-circle-line' : 'ri-error-warning-line'"></i>
      </div>
      <h3 class="modal-title">{{ title }}</h3>
      <p class="modal-message">{{ message }}</p>
      <button v-if="confirmMode" class="modal-btn modal-btn-cancel" type="button" @click="close(false)">Cancel</button>
      <button class="modal-btn" type="button" @click="close(true)">{{ confirmMode ? 'Yes, Proceed' : 'OK, Got it' }}</button>
    </div>
  </div>
</template>

<style scoped>
.custom-modal-overlay { position: fixed; z-index: 9999; display: none; inset: 0; align-items: center; justify-content: center; background: rgba(0,0,0,.4); backdrop-filter: blur(5px); opacity: 0; transition: opacity .3s ease; }
.custom-modal-overlay.active { display: flex; opacity: 1; }
.custom-modal-box { width: 400px; max-width: 90%; padding: 30px; transform: scale(.8); border-radius: 20px; background: #fff; box-shadow: 0 20px 60px rgba(0,0,0,.2); text-align: center; transition: transform .3s cubic-bezier(.175,.885,.32,1.275); }
.custom-modal-overlay.active .custom-modal-box { transform: scale(1); }
.modal-icon-wrap { display: flex; width: 60px; height: 60px; align-items: center; justify-content: center; margin: 0 auto 15px; border-radius: 50%; font-size: 30px; }
.icon-error { background: #ffebee; color: #ef5350; }
.icon-success { background: #e8f5e9; color: #66bb6a; }
.icon-warning { background: #fff3e0; color: #ff9800; }
.modal-title { margin-bottom: 10px; color: #333; font-size: 1.2rem; font-weight: 700; }
.modal-message { margin-bottom: 25px; color: #666; font-size: .95rem; line-height: 1.5; }
.modal-btn { padding: 10px 30px; border: 0; border-radius: 50px; outline: none; background: #2d3436; color: #fff; cursor: pointer; font-weight: 600; transition: .2s; }
.modal-btn:hover { background: #000; box-shadow: 0 4px 12px rgba(0,0,0,.15); }
.modal-btn-cancel { margin-right: 10px; background: #e0e0e0; color: #333; }
</style>
