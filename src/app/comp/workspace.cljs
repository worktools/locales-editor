
(ns app.comp.workspace
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> list-> input span div]]
            [app.config :as config]))

(def style-hint {:display :inline-block, :color (hsl 0 0 80), :margin-right 16})

(defcomp
 comp-workspace
 (locales)
 (div
  {}
  (list->
   {}
   (->> locales
        (map
         (fn [[k v]]
           [k
            (div
             {:style (merge ui/row {:padding "8px 16px"})}
             (div {:style {:width 320}} (<> k))
             (div
              {:style (merge ui/flex ui/column)}
              (div {} (<> "zhCN" style-hint) (<> (get v "zhCN")))
              (div {} (<> "enUS" style-hint) (<> (get v "enUS")))))]))))))
