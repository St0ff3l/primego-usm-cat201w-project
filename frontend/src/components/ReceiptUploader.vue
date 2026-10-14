<script setup>
import { onBeforeUnmount, ref } from 'vue'

defineProps({ modelValue: { type: Object, default: null }, name: { type: String, required: true } })
const emit = defineEmits(['update:modelValue'])
const input = ref(null)
const previewUrl = ref('')

function selectFile(file) {
  if (!file?.type || !file.type.startsWith('image/')) return
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(file)
  emit('update:modelValue', file)
}

function onChange(event) {
  selectFile(event.target.files?.[0])
}

function onDrop(event) {
  selectFile(event.dataTransfer.files?.[0])
}

function clearFile() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  if (input.value) input.value.value = ''
  emit('update:modelValue', null)
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div class="iu-upload-area" @dragover.prevent @drop.prevent="onDrop">
    <input :id="'receipt-' + name" ref="input" class="visually-hidden" type="file" :name="name" accept="image/*" @change="onChange" />
    <div v-if="modelValue" class="iu-preview">
      <div class="iu-preview-wrapper">
        <img :src="previewUrl" class="iu-preview-item" alt="Selected receipt preview" />
        <button class="iu-del-btn" type="button" aria-label="Remove selected image" title="Remove selected image" @click="clearFile">
          <i class="ri-close-line"></i>
        </button>
      </div>
      <p class="file-name">{{ modelValue.name }}</p>
    </div>
    <label v-else class="iu-placeholder" :for="'receipt-' + name">
      <span class="iu-icon"><i class="ri-camera-lens-line"></i></span>
      <span class="upload-main">Click or drag an image here</span>
      <span class="upload-sub">Upload a clear receipt image</span>
    </label>
    <label class="iu-add-btn" :for="'receipt-' + name" :aria-label="modelValue ? 'Choose another image' : 'Choose an image'" title="Choose an image">
      <i class="ri-add-line"></i>
    </label>
  </div>
</template>

<style scoped>
.iu-upload-area { position: relative; display: flex; min-height: 220px; flex-direction: column; align-items: center; justify-content: center; padding: 20px 20px 70px; border: 2px dashed #cbd5e1; border-radius: 20px; background: #f8fafc; transition: .3s; }
.iu-upload-area:hover { border-color: #4f46e5; background: rgba(255,255,255,.8); }
.visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
.iu-placeholder { display: flex; width: 100%; flex-direction: column; align-items: center; padding: 10px; cursor: pointer; text-align: center; }
.iu-icon { display: grid; width: 72px; height: 72px; place-items: center; border-radius: 50%; background: white; box-shadow: 0 10px 20px rgba(0,0,0,.05); color: #4f46e5; font-size: 2.3rem; }
.upload-main { margin-top: 14px; color: #6b7280; font-weight: 600; }
.upload-sub { margin-top: 5px; color: #9ca3af; font-size: .78rem; }
.iu-preview { display: flex; width: 100%; flex-wrap: wrap; justify-content: center; gap: 10px; }
.iu-preview-wrapper { position: relative; width: 108px; height: 108px; }
.iu-preview-item { width: 100%; height: 100%; border: 3px solid white; border-radius: 12px; object-fit: cover; box-shadow: 0 8px 15px rgba(0,0,0,.1); }
.iu-del-btn { position: absolute; top: -8px; right: -8px; display: grid; width: 26px; height: 26px; place-items: center; border: 2px solid white; border-radius: 50%; background: #ef4444; color: white; cursor: pointer; }
.file-name { max-width: 100%; margin: 0; color: #2ecc71; font-weight: 700; overflow-wrap: anywhere; text-align: center; }
.iu-add-btn { position: absolute; right: 15px; bottom: 15px; display: grid; width: 45px; height: 45px; place-items: center; border-radius: 50%; background: #4f46e5; box-shadow: 0 10px 20px rgba(0,0,0,.15); color: white; cursor: pointer; font-size: 1.5rem; }
.iu-add-btn:hover { filter: brightness(1.1); }
@media (max-width: 480px) { .iu-upload-area { min-height: 180px; padding: 14px 14px 60px; } }
</style>
