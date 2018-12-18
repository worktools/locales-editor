
(ns app.config )

(def bundle-builds #{"release" "local-bundle"})

(def cdn?
  (cond
    (exists? js/window) false
    (exists? js/process) (= "true" js/process.env.cdn)
    :else false))

(def dev?
  (let [debug? (do ^boolean js/goog.DEBUG)]
    (if debug?
      (cond
        (exists? js/window) true
        (exists? js/process) (not= "true" js/process.env.release)
        :else true)
      false)))

(def site
  {:port 8008,
   :title "多语言编辑",
   :icon "http://cdn.tiye.me/logo/jimeng-360x360.png",
   :dev-ui "http://localhost:8100/main.css",
   :release-ui "http://cdn.tiye.me/favored-fonts/main.css",
   :cdn-url "http://cdn.tiye.me/locales-editor/",
   :cdn-folder "tiye.me:cdn/locales-editor",
   :upload-folder "tiye.me:repo/chenyong/locales-editor/",
   :server-folder "tiye.me:servers/locales-editor",
   :theme "#eeeeff",
   :storage-key "locales-editor",
   :storage-path "locales.edn"})
