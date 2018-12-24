
(ns app.comp.modifications
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> list-> span div]]
            [app.config :as config]))

(defcomp
 comp-key
 (k)
 (span
  {:style {:display :inline-block,
           :padding "0 8px",
           :line-height "20px",
           :color :black,
           :cursor :pointer,
           :font-family ui/font-code},
   :inner-text k,
   :on-click (fn [e d! m!] (d! :session/query k))}))

(def style-group
  {:display :inline-block, :color (hsl 0 0 80), :margin-right 40, :font-family ui/font-fancy})

(def style-list {:display :inline-block})

(defcomp
 comp-modifications
 (data)
 (let [added (:added data), changed (:changed data), removed (:removed data)]
   (div
    {:style {:padding "8px 16px"}}
    (if (not (empty? added))
      (div
       {:style style-group}
       (<> "Added")
       (=< 8 nil)
       (list-> {:style style-list} (->> added (map (fn [k] [k (comp-key k)]))))))
    (if (not (empty? changed))
      (div
       {:style style-group}
       (<> "Changed")
       (=< 8 nil)
       (list-> {:style style-list} (->> changed (map (fn [k] [k (comp-key k)]))))))
    (if (not (empty? added))
      (div
       {:style style-group}
       (<> "Removed")
       (=< 8 nil)
       (list-> {:style style-list} (->> removed (map (fn [k] [k (comp-key k)])))))))))
