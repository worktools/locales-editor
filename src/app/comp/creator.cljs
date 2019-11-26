
(ns app.comp.creator
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core :refer [defcomp <> div span action-> cursor-> button input]]
            [respo.comp.inspect :refer [comp-inspect]]
            [respo.comp.space :refer [=<]]
            [app.comp.profile :refer [comp-profile]]
            [app.config :refer [dev?]]
            [app.schema :as schema]
            [app.config :as config]
            [clojure.string :refer [blank? replace trim upper-case lower-case]]
            [feather.core :refer [comp-i]]
            ["copy-text-to-clipboard" :as copy!]))

(defn generate-key [text]
  (let [target (-> text (replace (re-pattern "\\s+[a-zA-Z]") (fn [x] (upper-case (trim x)))))]
    (str (lower-case (subs target 0 1)) (subs target 1))))

(defn render-field [label content]
  (div
   {:style (merge ui/row {:padding 8, :width 480})}
   (div {:style {:width 80}} (<> label))
   content))

(defcomp
 comp-creator
 (states translation init-zh on-close! clear-zh!)
 (let [state (or (:data states) {:zh-text (or init-zh ""), :en-text "", :key-text ""})
       zh (:zh-text state)
       en (:en-text state)
       key-text (:key-text state)]
   (div
    {:style {:padding "16px"}}
    (div {} (<> "添加多语言"))
    (render-field
     "zhCN"
     (div
      {:style ui/expand}
      (input
       {:style (merge ui/input {:width 320}),
        :class-name "zh-input",
        :value zh,
        :on-input (fn [e d! m!] (m! (assoc state :zh-text (:value e))))})
      (div
       {:style (merge ui/row-middle {:min-height 32, :margin-bottom -8})}
       (span
        {:style {:cursor :pointer},
         :on-click (fn [e d! m!] (d! :effect/translate [key-text zh]))}
        (comp-i :globe 14 (hsl 200 80 60)))
       (=< 8 nil)
       (if (some? translation)
         (div
          {:style (merge ui/flex ui/row-middle)}
          (<>
           (:text translation)
           (merge
            ui/flex
            {:width :fit-content, :max-width :max-content, :line-height "20px"}))
          (=< 8 nil)
          (span
           {:style {:cursor :pointer},
            :on-click (fn [e d! m!]
              (m!
               (assoc
                state
                :en-text
                (:text translation)
                :key-text
                (generate-key (:text translation))))
              (d! :session/store-translation nil))}
           (comp-i :check 14 (hsl 200 80 60)))
          (=< 8 nil)
          (span
           {:style {:cursor :pointer},
            :on-click (fn [e d! m!] (d! :session/store-translation nil))}
           (comp-i :x 14 (hsl 20 80 60))))
         (<> "翻译" {:color (hsl 0 0 80)})))))
    (render-field
     "enUS"
     (div
      {:style ui/row-middle}
      (input
       {:style (merge ui/input {:width 320}),
        :value en,
        :on-input (fn [e d! m!] (m! (assoc state :en-text (:value e))))})
      (=< 8 nil)
      (span
       {:style {:cursor :pointer},
        :on-click (fn [e d! m!]
          (m! (assoc state :key-text (generate-key en)))
          (d! :session/store-translation nil))}
       (comp-i :corner-down-left 14 (hsl 200 80 70)))))
    (render-field
     "key"
     (div
      {:style ui/row}
      (input
       {:style (merge ui/input {:width 320}),
        :value key-text,
        :on-input (fn [e d! m!] (m! (assoc state :key-text (:value e))))})))
    (div
     {:style (merge ui/row-parted {:margin-top 16})}
     (span {})
     (div
      {:style ui/row-middle}
      (button
       {:style ui/button,
        :inner-text "连续提交",
        :on-click (fn [e d! m!]
          (if (not (or (blank? zh) (blank? en) (blank? key-text)))
            (do
             (d! :locale/create-locale {:key key-text, :zh zh, :en en})
             (d! :session/query key-text)
             (m! nil)
             (comment on-close! e d! m!)
             (clear-zh! m!)
             (copy! key-text))
            (js/console.warn "not allowed to be empty!")))})
      (=< 8 nil)
      (button
       {:style ui/button,
        :inner-text "复制并提交",
        :on-click (fn [e d! m!]
          (if (not (or (blank? zh) (blank? en) (blank? key-text)))
            (do
             (d! :locale/create-locale {:key key-text, :zh zh, :en en})
             (d! :session/query key-text)
             (m! nil)
             (on-close! e d! m!)
             (copy! key-text))
            (js/console.warn "not allowed to be empty!")))}))))))
