
(ns app.comp.workspace
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> action-> cursor-> list-> input button span div]]
            [app.config :as config]
            [respo-alerts.core :refer [comp-prompt comp-alert comp-confirm comp-modal]]
            [clojure.string :as string]
            [feather.core :refer [comp-i comp-icon]]
            ["copy-text-to-clipboard" :as copy!]
            [app.comp.creator :refer [comp-creator]]
            [app.comp.modifications :refer [comp-modifications]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def style-hint {:display :inline-block, :color (hsl 0 0 80), :margin "0 16px"})

(defcomp
 comp-locale
 (states k v)
 (let [state (or (:data states) {:copied? false})]
   (div
    {:class-name "locale-card",
     :style (merge
             ui/flex
             ui/column
             {:padding "8px 16px",
              :display :inline-flex,
              :background-color :white,
              :margin 8}
             (if (:copied? state)
               {:transform "scale(1.08)", :box-shadow (<< "0 0 3px ~(hsl 0 0 0 0.2)")}))}
    (div
     {:style (merge
              ui/row-parted
              {:min-width 240,
               :font-family ui/font-code,
               :overflow :auto,
               :color (hsl 0 0 70)})}
     (div
      {:style ui/row-middle}
      (cursor->
       :rename
       comp-prompt
       states
       {:trigger (<> k), :initial k}
       (fn [result d! m!]
         (when (not (string/blank? result)) (d! :locale/rename-one {:from k, :to result}))))
      (=< 8 nil)
      (comp-icon
       :copy
       {:font-size 14, :color (hsl 0 80 80), :cursor :pointer}
       (fn [e d! m!]
         (copy! (<< "~{k}"))
         (m! (assoc state :copied? true))
         (js/setTimeout (fn [] (m! (assoc state :copied? false))) 600))))
     (cursor->
      :remove
      comp-confirm
      states
      {:trigger (span {:class-name "minor"} (comp-i :x 14 (hsl 0 80 80))),
       :text "确认要删除这个字段?"}
      (fn [e d! m!] (d! :locale/rm-one k))))
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
     {:style ui/row-middle}
     (<> "enUS" style-hint)
     (cursor->
      "enUS"
      comp-prompt
      states
      {:trigger (<> (get v "enUS")), :initial (get v "enUS")}
      (fn [result d! m!]
        (when (not (string/blank? result))
          (d! :locale/edit-one {:lang "enUS", :key k, :text result}))))
     (=< 8 nil)
     (span
      {:on-click (fn [e d! m!] (d! :effect/translate [k (get v "zhCN")]))}
      (comp-icon :globe {:font-size 14, :color (hsl 0 0 80), :cursor :pointer} nil))))))

(defcomp
 comp-lang-table
 (states locales total query)
 (div
  {:style (merge ui/flex {:overflow :auto, :background-color (hsl 0 0 90), :padding 8})}
  (let [size (count locales)]
    (div
     {:style (merge ui/row-center {:padding 16, :font-size 16})}
     (if (not (string/blank? query)) (<> (<< "搜索 ~(pr-str query), ")))
     (if (= size total)
       (<> (<< "全部 ~{size} 条数据已显示"))
       (<> (<< "已显示 ~{size} 条数据, 总共 ~{total} 条")))
     (=< 8 nil)
     (if (not (string/blank? query))
       (button
        {:style ui/button,
         :inner-text "清除",
         :on-click (fn [e d! m!] (d! :session/query nil))}))))
  (list->
   {}
   (->> locales
        (sort-by (fn [[k v]] (count k)))
        (map (fn [[k v]] [k (cursor-> k comp-locale states k v)]))))))

(defcomp
 comp-search-box
 (states need-save? translation)
 (let [state (or (:data states) {:text "", :editing? false})]
   (div
    {:style (merge
             ui/row-parted
             {:padding 16, :border-bottom (<< "1px solid ~(hsl 0 0 90)")})}
    (div
     {:style ui/row-middle}
     (button
      {:style ui/button,
       :class-name "add-button",
       :inner-text "添加",
       :title "快捷键 Command i",
       :on-click (fn [e d! m!]
         (m! (assoc state :editing? true))
         (js/setTimeout
          (fn []
            (let [target (js/document.querySelector ".zh-input")]
              (if (some? target) (.focus target))))
          200))})
     (=< 16 nil)
     (input
      {:value (:text state),
       :style ui/input,
       :placeholder "回车键搜索",
       :on-input (fn [e d! m!] (m! (assoc state :text (:value e)))),
       :on-keydown (fn [e d! m!]
         (when (= "Enter" (.-key (:event e))) (d! :session/query (:text state))))}))
    (div
     {}
     (when need-save?
       (button
        {:style (merge ui/button),
         :inner-text "回滚",
         :on-click (fn [e d! m!] (d! :locale/rollback nil)),
         :title "回滚修改到已经保存的版本"}))
     (=< 16 nil)
     (button
      {:style (merge ui/button (when need-save? )),
       :inner-text "查看全部数据",
       :on-click (fn [e d! m!]
         (comment d! :effect/display nil)
         (d! :router/change {:name :text}))})
     (=< 16 nil)
     (button
      {:style (merge ui/button (when need-save? {:background-color :blue, :color :white})),
       :inner-text "生成文件",
       :on-click (fn [e d! m!] (d! :effect/codegen nil)),
       :title "快捷键 Command s"}))
    (let [on-close (fn [m!] (m! %cursor (assoc state :editing? false :text "")))]
      (comp-modal
       (:editing? state)
       {:style {:width 480}}
       on-close
       (fn []
         (cursor->
          :creator
          comp-creator
          states
          translation
          (:text state)
          (fn [e d! m!] (on-close m!))
          (fn [m!] (m! %cursor (assoc state :text ""))))))))))

(defcomp
 comp-workspace
 (states locales query total need-save? translation modifications)
 (div
  {:style (merge ui/flex ui/column {:overflow :auto})}
  (cursor-> :search comp-search-box states need-save? translation)
  (cursor-> :table comp-lang-table states locales total query)
  (comp-modifications modifications)))
