
(ns app.comp.text-mode
  (:require [hsl.core :refer [hsl]]
            [app.schema :as schema]
            [respo-ui.core :as ui]
            [respo.core :refer [defcomp list-> <> span div button textarea a]]
            [respo.comp.space :refer [=<]]
            [app.config :as config]
            [favored-edn.core :refer [write-edn]]
            ["copy-text-to-clipboard" :as copy!]))

(defcomp
 comp-text-mode
 (store)
 (div
  {:style (merge ui/fullscreen ui/column {:padding 8})}
  (div
   {:style ui/row-middle}
   (<> "Text mode")
   (=< 8 nil)
   (button
    {:inner-text "复制",
     :style ui/button,
     :on-click (fn [e d! m!] (copy! (write-edn (:locales store))))})
   (=< 40 nil)
   (a
    {:style ui/link,
     :inner-text "返回",
     :on-click (fn [e d! m!] (d! :router/change {:name :home}))}))
  (=< nil 8)
  (textarea
   {:style (merge
            ui/textarea
            ui/flex
            {:font-family ui/font-code, :width "100%", :height "80%", :font-size 12}),
    :value (write-edn (:locales store))})))
