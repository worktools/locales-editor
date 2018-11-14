
(ns app.node-config (:require ["path" :as path] [app.config :as config]))

(def env {:storage-path (path/join js/process.env.PWD "locales.edn")})
