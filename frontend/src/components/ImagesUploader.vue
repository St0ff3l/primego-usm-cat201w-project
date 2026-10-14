<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import './ImagesUploader.css'

const props = defineProps({
  inputName: { type: String, default: 'newImages' },
  deleteInputName: { type: String, default: 'deleteImageIds' },
  sortInputName: { type: String, default: 'imageSortOrder' },
  initialImages: { type: Array, default: () => [] },
  placeholderImg: { type: String, default: '' },
  maxImages: { type: Number, default: 9 },
})

const input = ref(null)
const images = ref([])
const deletedIds = ref(new Set())
const draggingFiles = ref(false)
const maxCount = Math.max(1, Math.floor(props.maxImages) || 9)
const sortOrder = computed(() => images.value.filter((image) => image.id != null).map((image) => image.id).join(','))
const deleteOrder = computed(() => [...deletedIds.value].join(','))

let nextLocalId = 0
let draggedKey = null

watch(() => props.initialImages, (initialImages) => {
  images.value = initialImages
    .filter((image) => image && image.id != null && String(image.url || '').trim())
    .slice(0, maxCount)
    .map((image) => ({ key: `server_${image.id}`, id: String(image.id), url: String(image.url) }))
}, { immediate: true })

function openPicker() {
  input.value?.click()
}

function syncFiles() {
  const files = new DataTransfer()
  images.value.forEach((image) => {
    if (image.file) files.items.add(image.file)
  })
  input.value.files = files.files
}

function addFiles(fileList) {
  const remaining = Math.max(0, maxCount - images.value.length)
  const picked = Array.from(fileList || []).filter((file) => file.type.startsWith('image/')).slice(0, remaining)
  picked.forEach((file) => {
    images.value.push({ key: `local_${nextLocalId++}`, file, url: URL.createObjectURL(file) })
  })
  syncFiles()
}

function onFileChange(event) {
  const files = Array.from(event.target.files)
  event.target.value = ''
  addFiles(files)
}

function onDrop(event) {
  event.preventDefault()
  draggingFiles.value = false
  if (event.dataTransfer.files.length) addFiles(event.dataTransfer.files)
}

function onItemDragStart(event, key) {
  draggedKey = key
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', key)
}

function onItemDragOver(event, targetKey) {
  event.preventDefault()
  if (!draggedKey || draggedKey === targetKey) return

  const from = images.value.findIndex((image) => image.key === draggedKey)
  const target = images.value.findIndex((image) => image.key === targetKey)
  const bounds = event.currentTarget.getBoundingClientRect()
  let to = target + Number(event.clientX > bounds.left + bounds.width / 2)
  if (from < to) to -= 1
  const [moved] = images.value.splice(from, 1)
  images.value.splice(Math.max(0, Math.min(to, images.value.length)), 0, moved)
  syncFiles()
}

function onItemDrop(event) {
  if (event.dataTransfer.files.length) onDrop(event)
  draggedKey = null
}

function onItemDragEnd() {
  draggedKey = null
  syncFiles()
}

function removeImage(image) {
  if (image.id != null) deletedIds.value.add(image.id)
  if (image.file) URL.revokeObjectURL(image.url)
  images.value = images.value.filter((item) => item.key !== image.key)
  syncFiles()
}

function imageFailed(event) {
  if (!props.placeholderImg) return
  event.currentTarget.onerror = null
  event.currentTarget.src = props.placeholderImg
}

onBeforeUnmount(() => {
  images.value.forEach((image) => {
    if (image.file) URL.revokeObjectURL(image.url)
  })
})
</script>

<template>
  <div
    class="iu-upload-area"
    :class="{ 'drag-over': draggingFiles }"
    @dragenter.prevent="draggingFiles = true"
    @dragover.prevent="draggingFiles = true"
    @dragleave.prevent="draggingFiles = false"
    @drop="onDrop"
  >
    <button v-if="images.length === 0" type="button" class="iu-placeholder" @click="openPicker">
      <span class="iu-icon"><i class="ri-camera-lens-line"></i></span>
      <span class="iu-prompt">Click or Drag images here</span>
      <span class="iu-hint">First image will be the Primary Image</span>
    </button>

    <div v-if="images.length" class="iu-preview" role="list">
      <div
        v-for="(image, index) in images"
        :key="image.key"
        class="iu-preview-wrapper"
        role="listitem"
        draggable="true"
        @dragstart="onItemDragStart($event, image.key)"
        @dragover="onItemDragOver($event, image.key)"
        @drop.stop="onItemDrop"
        @dragend="onItemDragEnd"
      >
        <img :src="image.url" class="iu-preview-item" alt="Product image preview" @error="imageFailed" />
        <button type="button" class="iu-del-btn" :aria-label="`Remove image ${index + 1}`" @click="removeImage(image)">
          <i class="ri-close-line"></i>
        </button>
      </div>
    </div>

    <button v-if="images.length" type="button" class="iu-add-btn" aria-label="Add product images" title="Add images" :disabled="images.length >= maxCount" @click="openPicker">
      <i class="ri-add-line"></i>
    </button>

    <input ref="input" type="file" :name="inputName" multiple accept="image/*" hidden @change="onFileChange" />
    <input type="hidden" :name="deleteInputName" :value="deleteOrder" />
    <input type="hidden" :name="sortInputName" :value="sortOrder" />
  </div>
</template>

<style>
.iu-placeholder {
  appearance: none;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
}
.iu-prompt,
.iu-hint { display: block; }
.iu-prompt { margin-top: 15px; color: #6b7280; }
.iu-hint { margin-top: 4px; color: #9ca3af; font-size: .75rem; }
.iu-del-btn,
.iu-add-btn { padding: 0; }
.iu-add-btn:disabled { opacity: .5; cursor: not-allowed; pointer-events: none; }
.iu-placeholder:focus-visible,
.iu-del-btn:focus-visible,
.iu-add-btn:focus-visible { outline: 3px solid var(--iu-primary); outline-offset: 3px; }
.iu-upload-area .iu-preview { display: flex; }
.iu-upload-area .iu-add-btn { display: flex; }
</style>
