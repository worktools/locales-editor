
(ns app.comp.workspace
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.comp.space :refer [=<]]
            [respo.core :refer [defcomp <> >> list-> input button span div]]
            [app.config :as config]
            [respo-alerts.core :refer [comp-prompt comp-alert comp-confirm use-modal]]
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
 (let [cursor (:cursor states), state (or (:data states) {:copied? false})]
   (div
    {:class-name "locale-card",
     :style (merge
             ui/flex
             ui/column
             {:padding "4px 8px",
              :display :inline-flex,
              :background-color :white,
              :margin 8}
             (if (:copied? state)
               {:transform "scale(1.08)", :box-shadow (<< "0 0 3px ~(hsl 0 0 0 0.2)")}))}
    (div
     {:style (merge
              ui/row-parted
              {:min-width 200,
               :font-family ui/font-code,
               :overflow :auto,
               :color (hsl 0 0 70)})}
     (div
      {:style ui/row-middle}
      (comp-prompt
       (>> states :rename)
       {:trigger (<> k), :initial k}
       (fn [result d!]
         (when (not (string/blank? result)) (d! :locale/rename-one {:from k, :to result}))))
      (=< 8 nil)
      (comp-icon
       :copy
       {:font-size 14, :color (hsl 0 80 80), :cursor :pointer}
       (fn [e d!]
         (copy! (<< "~{k}"))
         (d! cursor (assoc state :copied? true))
         (js/setTimeout (fn [] (d! cursor (assoc state :copied? false))) 600))))
     (comp-confirm
      (>> states :remove)
      {:trigger (span {:class-name "minor"} (comp-i :x 14 (hsl 0 80 80))),
       :text "确认要删除这个字段?"}
      (fn [e d!] (d! :locale/rm-one k))))
    (div
     {}
     (<> "zhCN" style-hint)
     (comp-prompt
      (>> states "zhCN")
      {:trigger (<> (get v "zhCN")), :initial (get v "zhCN")}
      (fn [result d!]
        (when (not (string/blank? result))
          (d! :locale/edit-one {:lang "zhCN", :key k, :text result})))))
    (div
     {:style ui/row-middle}
     (<> "enUS" style-hint)
     (comp-prompt
      (>> states "enUS")
      {:trigger (<> (get v "enUS")), :initial (get v "enUS")}
      (fn [result d!]
        (when (not (string/blank? result))
          (d! :locale/edit-one {:lang "enUS", :key k, :text result}))))
     (=< 8 nil)
     (span
      {:on-click (fn [e d!] (d! :effect/translate [k (get v "zhCN")]))}
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
        (map (fn [[k v]] [k (comp-locale (>> states k) k v)]))))))

(defcomp
 comp-search-box
 (states need-save? translation)
 (let [cursor (:cursor states)
       state (or (:data states) {:text ""})
       edit-modal (use-modal
                   (>> states :edit)
                   {:render (fn [on-close]
                      (comp-creator
                       (>> states :creator)
                       translation
                       (:text state)
                       (fn [e d!] (on-close d!))
                       (fn [d!] (d! cursor (assoc state :text "")))))})]
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
       :on-click (fn [e d!]
         ((:show edit-modal) d!)
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
       :on-input (fn [e d!] (d! cursor (assoc state :text (:value e)))),
       :on-keydown (fn [e d!]
         (when (= "Enter" (.-key (:event e))) (d! :session/query (:text state))))}))
    (div
     {}
     (when need-save?
       (button
        {:style (merge ui/button),
         :inner-text "回滚",
         :on-click (fn [e d!] (d! :locale/rollback nil)),
         :title "回滚修改到已经保存的版本"}))
     (=< 16 nil)
     (button
      {:style (merge ui/button (when need-save? )),
       :inner-text "查看全部数据",
       :on-click (fn [e d!]
         (comment d! :effect/display nil)
         (d! :router/change {:name :text}))})
     (=< 16 nil)
     (button
      {:style (merge ui/button (when need-save? {:background-color :blue, :color :white})),
       :inner-text "生成文件",
       :on-click (fn [e d!] (d! :effect/codegen nil)),
       :title "快捷键 Command s"}))
    (:ui edit-modal))))

(defcomp
 comp-workspace
 (states locales query total need-save? translation modifications)
 (div
  {:style (merge ui/flex ui/column {:overflow :auto})}
  (comp-search-box (>> states :search) need-save? translation)
  (comp-lang-table (>> states :table) locales total query)
  (comp-modifications modifications)))
