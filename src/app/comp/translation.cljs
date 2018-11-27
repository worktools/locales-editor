
(ns app.comp.translation
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> span div button]]
            [app.config :as config])
  (:require-macros [clojure.core.strint :refer [<<]]))

(defcomp
 comp-translation
 (info)
 (div
  {:style {:position :fixed,
           :top 140,
           :right 16,
           :background-color :white,
           :padding 16,
           :border (<< "1px solid ~(hsl 0 0 80)"),
           :min-width 320}}
  (div
   {:style {:font-family ui/font-code, :color (hsl 0 0 70), :font-size 14}}
   (<> (:key info)))
  (div {} (<> (:text info)))
  (=< nil 16)
  (div
   {:style ui/row-parted}
   (span {})
   (div
    {}
    (button
     {:style ui/button,
      :inner-text "丢弃",
      :on-click (fn [e d! m!] (d! :session/store-translation nil))})
    (=< 8 nil)
    (button
     {:style ui/button,
      :inner-text "使用",
      :on-click (fn [e d! m!] (d! :locale/accept-translation info))})))))
