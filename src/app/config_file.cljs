
(ns app.config-file (:require [cljs.spec.alpha :as s] [expound.alpha :refer [expound]]))

(s/def ::app-id string?)

(s/def ::app-secret string?)

(s/def
 ::locale-detail
 (s/and
  (s/map-of (s/or :zh #(= % "zhCN") :en #(= % "enUS")) string?)
  (fn [x] (and (contains? x "zhCN") (contains? x "enUS")))))

(s/def ::locales (s/map-of string? ::locale-detail))

(s/def ::version string?)

(s/def ::schema (s/keys :req-un [::version]))

(s/def ::settings (s/keys :req-un [::app-id ::app-secret]))

(s/def ::config (s/keys :req-un [::locales ::settings ::schema]))

(defn validate! [data]
  (if (s/valid? ::config data)
    (println "Validation success.")
    (do (println (expound ::config data)) (js/process.exit 1))))
