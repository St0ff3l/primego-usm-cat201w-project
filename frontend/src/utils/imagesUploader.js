import { legacyBase } from '../api'

let assetsPromise

export function createImagesUploader(selector, config) {
  if (!assetsPromise) {
    const stylesheet = document.createElement('link')
    stylesheet.rel = 'stylesheet'
    stylesheet.href = `${legacyBase}/assets/css/images_uploader.css`
    const stylesLoaded = new Promise((resolve, reject) => {
      stylesheet.onload = resolve
      stylesheet.onerror = () => reject(new Error('Unable to load the product image uploader.'))
    })
    document.head.append(stylesheet)

    const script = document.createElement('script')
    script.src = `${legacyBase}/assets/js/images_uploader.js?v=${Date.now()}`
    const scriptLoaded = new Promise((resolve, reject) => {
      script.onload = resolve
      script.onerror = () => reject(new Error('Unable to load the product image uploader.'))
    })
    document.head.append(script)

    assetsPromise = Promise.all([stylesLoaded, scriptLoaded])
  }

  return assetsPromise.then(() => new window.ImagesUploader(selector, config))
}
