
(ns app.config (:require [app.util :refer [get-env!]]))

(def bundle-builds #{"release" "local-bundle"})

(def dev?
  (if (exists? js/window)
    (do ^boolean js/goog.DEBUG)
    (not (contains? bundle-builds (get-env! "mode")))))

(def site
  {:storage-key "locales-editor",
   :port 8008,
   :title "Locales",
   :icon "http://cdn.tiye.me/logo/jimeng.png",
   :dev-ui "http://localhost:8100/main.css",
   :release-ui "http://cdn.tiye.me/favored-fonts/main.css",
   :cdn-url "http://cdn.tiye.me/locales-editor/",
   :cdn-folder "tiye.me:cdn/locales-editor",
   :upload-folder "tiye.me:repo/chenyong/locales-editor/",
   :server-folder "tiye.me:servers/locales-editor",
   :theme "#eeeeff"})
