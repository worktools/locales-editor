
(ns app.twig.container
  (:require [recollect.macros :refer [deftwig]]
            [app.twig.user :refer [twig-user]]
            ["randomcolor" :as color]
            [clojure.string :as string]))

(deftwig
 twig-members
 (sessions users)
 (->> sessions
      (map (fn [[k session]] [k (get-in users [(:user-id session) :name])]))
      (into {})))

(deftwig
 twig-container
 (db session records)
 (let [logged-in? (some? (:user-id session))
       router (:router session)
       base-data {:logged-in? logged-in?,
                  :session session,
                  :reel-length (count records),
                  :locales (let [locales (:locales db)
                                 locale-keys (set
                                              (concat
                                               (keys (get locales "zhCn"))
                                               (keys (get locales "enUS"))))]
                    (->> locale-keys
                         (map
                          (fn [locale-key]
                            [locale-key
                             {"zhCN" (get-in locales ["zhCN" locale-key]),
                              "enUS" (get-in locales ["enUS" locale-key])}]))
                         (filter
                          (fn [[k info]]
                            (if (some? (:query session))
                              (string/includes? k (:query session))
                              true)))
                         (into {})))}]
   (merge
    base-data
    (if logged-in?
      {:user (twig-user (get-in db [:users (:user-id session)])),
       :router (assoc
                router
                :data
                (case (:name router)
                  :home (:pages db)
                  :profile (twig-members (:sessions db) (:users db))
                  {})),
       :count (count (:sessions db)),
       :color (color/randomColor)}
      nil))))
