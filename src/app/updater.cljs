
(ns app.updater
  (:require [app.updater.session :as session]
            [app.updater.user :as user]
            [app.updater.router :as router]
            [app.updater.locale :as locale]
            [app.schema :as schema]
            [respo-message.updater :refer [update-messages]]))

(defn updater [db op op-data sid op-id op-time]
  (let [f (case op
            :session/connect session/connect
            :session/disconnect session/disconnect
            :session/remove-message session/remove-message
            :session/notify session/notify
            :session/query session/query
            :session/store-translation session/store-translation
            :user/log-in user/log-in
            :user/sign-up user/sign-up
            :user/log-out user/log-out
            :router/change router/change
            :locale/edit-one locale/edit-one
            :locale/rename-one locale/rename-one
            :locale/rm-one locale/rm-one
            :locale/mark-saved locale/mark-saved
            :locale/accept-translation locale/accept-translation
            :locale/checkout locale/checkout
            :locale/create-locale locale/create-locale
            :locale/rollback locale/rollback
            (do (println "Unknown op:" op) identity))]
    (f db op-data sid op-id op-time)))
