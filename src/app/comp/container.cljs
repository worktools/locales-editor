
(ns app.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core :refer [defcomp <> div span action-> cursor-> button]]
            [respo.comp.inspect :refer [comp-inspect]]
            [respo.comp.space :refer [=<]]
            [respo-message.comp.messages :refer [comp-messages]]
            [cumulo-reel.comp.reel :refer [comp-reel]]
            [app.config :refer [dev?]]
            [app.schema :as schema]
            [app.config :as config]
            [app.comp.workspace :refer [comp-workspace]]
            [app.comp.translation :refer [comp-translation]]
            [respo-md.comp.md :refer [comp-md]]
            [app.comp.text-mode :refer [comp-text-mode]]))

(defcomp
 comp-offline
 ()
 (div
  {:style (merge
           ui/global
           ui/fullscreen
           ui/column-dispersive
           {:background-color (:theme config/site)})}
  (div {:style {:height 0}})
  (div
   {:style {:background-image (str "url(" (:icon config/site) ")"),
            :width 128,
            :height 128,
            :background-size :contain}})
  (div
   {:style (merge ui/center {:cursor :pointer, :line-height "32px"}),
    :on-click (action-> :effect/connect nil)}
   (<> "No connection..." {:font-family ui/font-fancy, :font-size 24})
   (=< nil 16)
   (comp-md
    "This is a locales editor. Its backend is probably not started. Find out more [on GitHub](https://github.com/jimengio/locales-editor)."))))

(defcomp
 comp-status-color
 (color)
 (div
  {:style (let [size 24]
     {:width size,
      :height size,
      :position :absolute,
      :bottom 60,
      :left 8,
      :background-color color,
      :border-radius "50%",
      :opacity 0.6,
      :pointer-events :none})}))

(defcomp
 comp-container
 (states store)
 (let [state (:data states)
       session (:session store)
       router (:router store)
       router-data (:data router)]
   (if (nil? store)
     (comp-offline)
     (div
      {:style (merge ui/global ui/fullscreen ui/column)}
      (if (:logged-in? store)
        (case (:name router)
          :home
            (cursor->
             :workspace
             comp-workspace
             states
             (:locales store)
             (:query session)
             (:matched-count store)
             (:need-save? store)
             (:translation session)
             (:modifications store))
          :text (comp-text-mode store)
          (<> router)))
      (let [translation (:translation session)]
        (when (and (some? translation) (not (empty? (:key translation))))
          (comp-translation (:translation session))))
      (comment comp-status-color (:color store))
      (when dev? (comp-inspect "Store" store {:bottom 40, :right 0, :max-width "100%"}))
      (comp-messages
       (get-in store [:session :messages])
       {}
       (fn [info d! m!] (d! :session/remove-message info)))
      (when dev? (comp-reel (:reel-length store) {}))))))

(def style-body {:padding "8px 16px"})
