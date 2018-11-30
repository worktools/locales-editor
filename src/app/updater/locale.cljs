
(ns app.updater.locale )

(defn accept-translation [db op-data sid op-id op-time]
  (-> db
      (assoc-in [:locales (:key op-data) "enUS"] (:text op-data))
      (assoc-in [:sessions sid :translation] nil)))

(defn add-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (if (contains? locales op-data)
       locales
       (assoc locales op-data {"zhCN" op-data, "enUS" op-data})))))

(defn checkout [db op-data sid op-id op-time]
  (-> db (assoc :locales op-data) (assoc :saved-locales op-data)))

(defn edit-one [db op-data sid op-id op-time]
  (assoc-in db [:locales (:key op-data) (:lang op-data)] (:text op-data)))

(defn mark-saved [db op-data sid op-id op-time] (assoc db :saved-locales (:locales db)))

(defn rename-one [db op-data sid op-id op-time]
  (update
   db
   :locales
   (fn [locales]
     (let [x0 (:from op-data), x1 (:to op-data)]
       (-> locales (dissoc x0) (assoc x1 (get locales x0)))))))

(defn rm-one [db op-data sid op-id op-time]
  (update db :locales (fn [locales] (dissoc locales op-data))))
