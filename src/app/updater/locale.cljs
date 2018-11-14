
(ns app.updater.locale )

(defn add-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (let [entry-creator (fn [info]
                           (println "about info:" (count info) op-data)
                           (if (contains? info op-data) info (assoc info op-data op-data)))]
       (-> locales (update "zhCN" entry-creator) (update "enUS" entry-creator))))))

(defn edit-one [db op-data sid op-id op-time]
  (assoc-in db [:locales (:lang op-data) (:key op-data)] (:text op-data)))

(defn rename-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (let [entry-updater (fn [info]
                           (let [x0 (:from op-data), x1 (:to op-data)]
                             (-> info (dissoc x0) (assoc x1 (get info x0)))))]
       (-> locales (update "zhCN" entry-updater) (update "enUS" entry-updater))))))

(defn rm-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (let [entry-removal (fn [info] (dissoc info op-data))]
       (-> locales (update "zhCN" entry-removal) (update "enUS" entry-removal))))))
