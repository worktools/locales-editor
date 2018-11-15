
(ns app.updater.locale )

(defn add-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (if (contains? locales op-data)
       locales
       (assoc locales op-data {"zhCN" op-data, "enUS" op-data})))))

(defn edit-one [db op-data sid op-id op-time]
  (assoc-in db [:locales (:key op-data) (:lang op-data)] (:text op-data)))

(defn rename-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (let [x0 (:from op-data), x1 (:to op-data)]
       (-> locales (dissoc x0) (assoc x1 (get locales x0)))))))

(defn rm-one [db op-data sid op-id op-time]
  (update db :locales (fn [locales] (dissoc locales op-data))))
