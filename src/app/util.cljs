
(ns app.util
  (:require [favored-edn.core :refer [write-edn]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(defn display-snapshot! [data]
  (let [code (write-edn data), child (.open js/window)]
    (-> child .-document (.write (<< "<!DOCTYPE html><body><pre>~{code}</pre></body>\n")))))

(defn find-first [f xs] (reduce (fn [_ x] (when (f x) (reduced x))) nil xs))
