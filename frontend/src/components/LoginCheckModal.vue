<script setup>
defineProps({ open: Boolean })
defineEmits(['close'])
</script>

<template>
  <div v-if="open" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>Login Required</h3>
        <button type="button" class="close-modal" aria-label="Close" @click="$emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <p>You need to login to proceed with this action.</p>
        <div class="modal-actions">
          <a href="/public/login.jsp" class="btn-login-modal">Login Now</a>
          <button type="button" class="btn-cancel-modal" @click="$emit('close')">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.modal-overlay { position: fixed; inset: 0; z-index: 2000; display: flex; justify-content: center; align-items: center; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); padding: 20px; }
.modal-content { width: 90%; max-width: 400px; padding: 25px; border-radius: 15px; background: white; box-shadow: 0 10px 30px rgba(0,0,0,.2); animation: modalFadeIn .3s ease; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.modal-header h3 { margin: 0; color: #2d3436; }
.close-modal { border: 0; background: transparent; font-size: 1.5rem; cursor: pointer; color: #aaa; }
.close-modal:hover { color: #333; }
.modal-body p { color: #666; margin: 0 0 20px; }
.modal-actions { display: flex; gap: 10px; }
.btn-login-modal, .btn-cancel-modal { flex: 1; padding: 10px; border: 0; border-radius: 8px; text-align: center; text-decoration: none; font-weight: 600; cursor: pointer; }
.btn-login-modal { background: #ff9500; color: white; }
.btn-login-modal:hover { background: #e68600; }
.btn-cancel-modal { background: #f1f2f6; color: #333; }
.btn-cancel-modal:hover { background: #dfe4ea; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
</style>
