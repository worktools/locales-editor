
(ns app.twig.container
  (:require [recollect.twig :refer [deftwig]]
            [app.twig.user :refer [twig-user]]
            ["randomcolor" :as color]
            [clojure.string :as string]
            [fuzzy-filter.core :refer [parse-by-word]]
            [app.locales :refer [get-modifications]]))

(defn find-chunk [xs x]
  (comment string/includes? (string/lower-case xs) (string/lower-case x))
  (:matches? (parse-by-word (string/lower-case xs) (string/lower-case x))))

(deftwig
 twig-members
 (sessions users)
 (->> sessions
      (map (fn [[k session]] [k (get-in users [(:user-id session) :name])]))
      (into {})))

(deftwig
 twig-container
 (db session records)
 (let [logged-in? (or true (some? (:user-id session)))
       router (:router session)
       matched-locale-pairs (->> (:locales db)
                                 (filter
                                  (fn [[k info]]
                                    (if (not (string/blank? (:query session)))
                                      (or (find-chunk k (:query session))
                                          (find-chunk (get info "zhCN") (:query session))
                                          (find-chunk (get info "enUS") (:query session)))
                                      true))))]
   {:logged-in? true,
    :session session,
    :reel-length (count records),
    :color (color/randomColor),
    :count (count (:sessions db)),
    :router (assoc
             router
             :data
             (case (:name router)
               :home (:pages db)
               :profile (twig-members (:sessions db) (:users db))
               {})),
    :locales (->> matched-locale-pairs
                  (sort-by (fn [[k info]] (count k)))
                  (take 40)
                  (into {})),
    :matched-count (count matched-locale-pairs),
    :need-save? (not= (:locales db) (:saved-locales db)),
    :modifications (get-modifications (:locales db) (:saved-locales db))}))
