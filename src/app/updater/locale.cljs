
(ns app.updater.locale )

(defn accept-translation [db op-data sid op-id op-time]
  (-> db
      (assoc-in [:locales (:key op-data) "enUS"] (:text op-data))
      (assoc-in [:sessions sid :translation] nil)))

(defn checkout [db op-data sid op-id op-time]
  (-> db (assoc :locales op-data) (assoc :saved-locales op-data)))

(defn create-locale [db op-data sid op-id op-time]
  (let [k (:key op-data), zh (:zh op-data), en (:en op-data)]
    (update
     db
     :locales
     (fn [locales]
       (if (contains? locales k) locales (assoc locales k {"zhCN" zh, "enUS" en}))))))

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

(defn rollback [db op-data sid op-id op-time] (assoc db :locales (:saved-locales db)))
