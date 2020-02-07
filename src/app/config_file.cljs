
(ns app.config-file
  (:require [lilac.core
             :refer
             [validate-lilac
              number+
              string+
              record+
              map+
              vector+
              boolean+
              or+
              and+
              is+
              optional+
              deflilac]]
            ["chalk" :as chalk]))

(deflilac
 lilac-config+
 ()
 (record+
  {:sessions (optional+ (record+ {})),
   :users (optional+ (record+ {})),
   :locales (map+
             (string+)
             (record+ {"zhCN" (string+), "enUS" (string+)} {:exact-keys? true})),
   :schema (record+ {:version (is+ "0.2.x")}),
   :settings (record+ {:app-id (string+), :app-secret (string+)})}
  {:check-keys? true}))

(defn validate! [data]
  (let [result (validate-lilac data (lilac-config+))]
    (if (:ok? result)
      (println (chalk/gray "Validation success."))
      (do (println (chalk/red (:formatted-message result))) (js/process.exit 1)))))
