
(ns app.comp.workspace
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> cursor-> list-> input button span div]]
            [app.config :as config]
            [respo-alerts.comp.alerts :refer [comp-prompt]]
            [clojure.string :as string]))

(def style-hint {:display :inline-block, :color (hsl 0 0 80), :margin-right 16})

(defcomp
 comp-lang-table
 (states locales)
 (div
  {:style (merge ui/flex {:overflow :auto})}
  (list->
   {}
   (->> locales
        (take 40)
        (map
         (fn [[k v]]
           [k
            (div
             {:style (merge ui/row {:padding "8px 16px"})}
             (div
              {:style {:width 320, :font-family ui/font-code}}
              (cursor->
               :rename
               comp-prompt
               states
               {:trigger (<> k), :initial k}
               (fn [result d! m!]
                 (when (not (string/blank? result))
                   (d! :locale/rename-one {:from k, :to result})))))
             (div
              {:style (merge ui/flex ui/column)}
              (div
               {}
               (<> "zhCN" style-hint)
               (cursor->
                "zhCN"
                comp-prompt
                states
                {:trigger (<> (get v "zhCN")), :initial (get v "zhCN")}
                (fn [result d! m!]
                  (when (not (string/blank? result))
                    (d! :locale/edit-one {:lang "zhCN", :key k, :text result})))))
              (div
               {}
               (<> "enUS" style-hint)
               (cursor->
                "enUS"
                comp-prompt
                states
                {:trigger (<> (get v "enUS")), :initial (get v "enUS")}
                (fn [result d! m!]
                  (when (not (string/blank? result))
                    (d! :locale/edit-one {:lang "enUS", :key k, :text result})))))))]))))
  (let [size (count locales)]
    (div
     {:style (merge ui/center {:padding 16})}
     (if (> size 40)
       (<> (str "40 locales displayed, " (- size 40) " not shown"))
       (<> (str "all " size " locales are displayed")))))))

(defcomp
 comp-search-box
 (states)
 (let [state (or (:data states) {:text ""})]
   (div
    {:style {:padding 16}}
    (input
     {:value (:text state),
      :style ui/input,
      :on-input (fn [e d! m!] (m! (assoc state :text (:value e)))),
      :on-keydown (fn [e d! m!]
        (when (= "Enter" (.-key (:event e))) (d! :session/query (:text state))))})
    (=< 16 nil)
    (button
     {:style ui/button,
      :inner-text "Search",
      :on-click (fn [e d! m!] (d! :session/query (:text state)))})
    (=< 16 nil)
    (cursor->
     :add
     comp-prompt
     states
     {:trigger (button {:style ui/button, :inner-text "Create"})}
     (fn [result d! m!]
       (when (not (string/blank? result))
         (d! :locale/add-one result)
         (d! :session/query result))))
    (=< 16 nil)
    (button
     {:style ui/button,
      :inner-text "Codegen",
      :on-click (fn [e d! m!] (d! :effect/codegen nil))}))))

(defcomp
 comp-workspace
 (states locales)
 (div
  {:style (merge ui/flex ui/column {:overflow :auto})}
  (cursor-> :search comp-search-box states)
  (cursor-> :table comp-lang-table states locales)))
